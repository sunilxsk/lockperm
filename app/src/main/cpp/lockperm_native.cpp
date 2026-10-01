#include <jni.h>
#include <dirent.h>
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <ifaddrs.h>
#include <net/if.h>
#include <pthread.h>
#include <signal.h>
#include <spawn.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/sysinfo.h>
#include <sys/system_properties.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/utsname.h>
#include <time.h>
#include <unistd.h>

#include <map>
#include <string>
#include <vector>

#include <android/log.h>
#include "dobby.h"

#define TAG "LockPermNat"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#ifndef PROP_VALUE_MAX
#define PROP_VALUE_MAX 92
#endif
// 😢😢😢
namespace {

volatile bool g_enabled = false;
volatile bool g_installed = false;

pthread_rwlock_t g_lock = PTHREAD_RWLOCK_INITIALIZER;

std::map<std::string, std::string> g_props;
std::vector<std::string> g_hide;
std::vector<std::string> g_exist;
std::string g_kernel;
std::string g_arch;
std::string g_gpu;
std::string g_cache;

std::vector<std::string> g_vpn;
volatile bool g_vpn_enable = false;

volatile bool g_anti_detect = false;

struct PatchRec {
  uintptr_t addr = 0;
  std::vector<uint8_t> bytes;
};
std::vector<PatchRec> g_patches;

std::vector<uintptr_t> g_hook_addrs;

struct MapSeg {
  uintptr_t vs = 0;
  uintptr_t ve = 0;
  uint64_t foff = 0;
  std::string path;
};
std::vector<MapSeg> g_maps;
volatile bool g_maps_ready = false;

std::map<int, std::string> g_fdpath;

volatile int64_t g_time_offset_ms = 0;
volatile bool g_time_enable = false;
volatile int64_t g_uptime_base_ms = 0;
volatile int64_t g_uptime_target_ms = 0;
volatile bool g_uptime_enable = false;

volatile bool g_block_exec = false;
volatile bool g_block_exit = false;
volatile bool g_crash_catch = false;

std::map<DIR *, std::string> g_dirs;

struct SpoofFile {
  std::string path;
  std::string content;
  std::string real;
  bool ready = false;
};
std::vector<SpoofFile *> g_files;

__thread bool t_bypass = false;

bool hookGpu();

struct ScopedBypass {
  bool prev;
  ScopedBypass() : prev(t_bypass) { t_bypass = true; }
  ~ScopedBypass() { t_bypass = prev; }
};

std::vector<std::string> split(const std::string &s, char sep) {
  std::vector<std::string> out;
  size_t start = 0;
  while (true) {
    size_t p = s.find(sep, start);
    if (p == std::string::npos) {
      out.push_back(s.substr(start));
      break;
    }
    out.push_back(s.substr(start, p - start));
    start = p + 1;
  }
  return out;
}

std::string unescape(const std::string &s) {
  std::string o;
  o.reserve(s.size());
  for (size_t i = 0; i < s.size(); ++i) {
    if (s[i] == '\\' && i + 1 < s.size()) {
      char c = s[i + 1];
      if (c == 'n') { o += '\n'; ++i; continue; }
      if (c == 't') { o += '\t'; ++i; continue; }
      if (c == '\\') { o += '\\'; ++i; continue; }
    }
    o += s[i];
  }
  return o;
}

uint32_t fnv(const char *s) {
  uint32_t h = 2166136261u;
  for (; *s; ++s) {
    h ^= (unsigned char)*s;
    h *= 16777619u;
  }
  return h;
}

bool pathEqualsOrUnder(const std::string &s, const std::string &h) {
  if (h.empty()) return false;
  if (s == h) return true;
  return s.size() > h.size() && s.compare(0, h.size(), h) == 0 && s[h.size()] == '/';
}

bool isHidden(const char *p) {
  if (!g_enabled || p == nullptr || *p == '\0') return false;
  std::string s(p);
  pthread_rwlock_rdlock(&g_lock);
  bool hit = false;
  for (size_t i = 0; i < g_hide.size() && !hit; ++i) {
    hit = pathEqualsOrUnder(s, g_hide[i]);
  }
  pthread_rwlock_unlock(&g_lock);
  return hit;
}

bool isForceExist(const char *p) {
  if (!g_enabled || p == nullptr || *p == '\0') return false;
  std::string s(p);
  pthread_rwlock_rdlock(&g_lock);
  bool hit = false;
  for (size_t i = 0; i < g_exist.size() && !hit; ++i) {
    hit = (s == g_exist[i]);
  }
  pthread_rwlock_unlock(&g_lock);
  return hit;
}

SpoofFile *findSpoof(const char *p) {
  if (!g_enabled || p == nullptr || *p == '\0') return nullptr;
  std::string s(p);
  pthread_rwlock_rdlock(&g_lock);
  SpoofFile *hit = nullptr;
  for (size_t i = 0; i < g_files.size() && hit == nullptr; ++i) {
    if (g_files[i]->path == s) hit = g_files[i];
  }
  pthread_rwlock_unlock(&g_lock);
  return hit;
}

int rawWriteFile(const char *path, const std::string &data) {
  ScopedBypass bp;
#if defined(__NR_openat)
  int fd = (int)syscall(__NR_openat, AT_FDCWD, path, O_WRONLY | O_CREAT | O_TRUNC, 0600);
#else
  int fd = (int)syscall(__NR_open, path, O_WRONLY | O_CREAT | O_TRUNC, 0600);
#endif
  if (fd < 0) return -1;
  size_t off = 0;
  while (off < data.size()) {
    ssize_t n = write(fd, data.data() + off, data.size() - off);
    if (n <= 0) break;
    off += (size_t)n;
  }
  close(fd);
  return 0;
}

const char *materialize(SpoofFile *f) {
  if (f == nullptr) return nullptr;
  if (f->ready) return f->real.c_str();
  std::string cache;
  pthread_rwlock_rdlock(&g_lock);
  cache = g_cache;
  pthread_rwlock_unlock(&g_lock);
  if (cache.empty()) return nullptr;
  char name[64];
  snprintf(name, sizeof(name), ".lk_%08x", fnv(f->path.c_str()));
  f->real = cache + "/" + name;
  if (rawWriteFile(f->real.c_str(), f->content) != 0) {
    f->real.clear();
    return nullptr;
  }
  f->ready = true;
  return f->real.c_str();
}

bool readWholeFile(const char *path, std::string &out) {
  ScopedBypass bp;
#if defined(__NR_openat)
  int fd = (int)syscall(__NR_openat, AT_FDCWD, path, O_RDONLY);
#else
  int fd = (int)syscall(__NR_open, path, O_RDONLY);
#endif
  if (fd < 0) return false;
  char buf[8192];
  out.clear();
  while (true) {
    ssize_t n = read(fd, buf, sizeof(buf));
    if (n > 0) {
      out.append(buf, (size_t)n);
      continue;
    }
    break;
  }
  close(fd);
  return !out.empty();
}

void parseMaps(const std::string &s, std::vector<MapSeg> &out) {
  out.clear();
  size_t start = 0;
  while (start <= s.size()) {
    size_t end = s.find('\n', start);
    std::string line = (end == std::string::npos) ? s.substr(start) : s.substr(start, end - start);
    if (!line.empty()) {
      unsigned long long a = 0, b = 0, fo = 0;
      char perm[16] = {0};
      int consumed = 0;
      int got = sscanf(line.c_str(), "%llx-%llx %15s %llx %*x:%*x %*u %n",
                       &a, &b, perm, &fo, &consumed);
      if (got >= 4 && consumed > 0 && (int)line.size() > consumed) {
        const char *p = line.c_str() + consumed;
        while (*p == ' ' || *p == '\t') ++p;
        if (*p != '\0') {
          MapSeg seg;
          seg.vs = (uintptr_t)a;
          seg.ve = (uintptr_t)b;
          seg.foff = fo;
          seg.path = p;
          if (!seg.path.empty() && seg.path[0] == '/' && seg.ve > seg.vs) {
            out.push_back(seg);
          }
        }
      }
    }
    if (end == std::string::npos) break;
    start = end + 1;
  }
}

void ensureMaps() {
  if (g_maps_ready) return;
  pthread_rwlock_wrlock(&g_lock);
  if (!g_maps_ready) {
    std::string content;
    if (readWholeFile("/proc/self/maps", content)) {
      parseMaps(content, g_maps);
      g_maps_ready = true;
    }
  }
  pthread_rwlock_unlock(&g_lock);
}

void collectPatches() {
  pthread_rwlock_wrlock(&g_lock);
  g_patches.clear();
  const size_t kSample = 32;
  for (size_t i = 0; i < g_hook_addrs.size(); ++i) {
    uintptr_t addr = g_hook_addrs[i];
    if (addr == 0) continue;
    if (addr < 4096) continue;
    PatchRec rec;
    rec.addr = addr;
    rec.bytes.resize(kSample);
    memcpy(rec.bytes.data(), (const void *)addr, kSample);
    g_patches.push_back(rec);
  }
  LOGI("anti-detect: collected %zu patched entries", g_patches.size());
  pthread_rwlock_unlock(&g_lock);
}

void syncBuffer(int fd, void *buf, size_t count, int64_t foff) {
  if (!g_enabled || !g_anti_detect) return;
  if (buf == nullptr || count == 0 || foff < 0) return;
  if (g_patches.empty()) return;

  std::string path;
  pthread_rwlock_rdlock(&g_lock);
  std::map<int, std::string>::const_iterator fdIt = g_fdpath.find(fd);
  if (fdIt == g_fdpath.end()) {
    pthread_rwlock_unlock(&g_lock);
    return;
  }
  path = fdIt->second;
  pthread_rwlock_unlock(&g_lock);

  if (path.empty() || path[0] != '/') return;
  if (path.find(".so") == std::string::npos) return;

  ensureMaps();

  pthread_rwlock_rdlock(&g_lock);
  for (size_t pi = 0; pi < g_patches.size(); ++pi) {
    const PatchRec &rec = g_patches[pi];
    for (size_t si = 0; si < g_maps.size(); ++si) {
      const MapSeg &seg = g_maps[si];
      if (seg.path != path) continue;
      if (rec.addr < seg.vs || rec.addr >= seg.ve) continue;
      int64_t poff = (int64_t)seg.foff + (int64_t)(rec.addr - seg.vs);
      if (poff + (int64_t)rec.bytes.size() <= foff) continue;
      if (poff >= foff + (int64_t)count) continue;
      for (size_t i = 0; i < rec.bytes.size(); ++i) {
        int64_t f = poff + (int64_t)i;
        if (f < foff || f >= foff + (int64_t)count) continue;
        ((uint8_t *)buf)[f - foff] = rec.bytes[i];
      }
      break;
    }
  }
  pthread_rwlock_unlock(&g_lock);
}

void syncMapped(void *addr, size_t len, int fd, int64_t foff) {
  if (!g_enabled || !g_anti_detect) return;
  if (addr == nullptr || addr == MAP_FAILED || len == 0 || fd < 0 || foff < 0) return;
  if (g_patches.empty()) return;

  std::string path;
  pthread_rwlock_rdlock(&g_lock);
  std::map<int, std::string>::const_iterator fdIt = g_fdpath.find(fd);
  if (fdIt == g_fdpath.end()) {
    pthread_rwlock_unlock(&g_lock);
    return;
  }
  path = fdIt->second;
  pthread_rwlock_unlock(&g_lock);

  if (path.empty() || path[0] != '/') return;
  if (path.find(".so") == std::string::npos) return;

  ensureMaps();

  pthread_rwlock_rdlock(&g_lock);
  bool touched = false;
  for (size_t pi = 0; pi < g_patches.size(); ++pi) {
    const PatchRec &rec = g_patches[pi];
    for (size_t si = 0; si < g_maps.size(); ++si) {
      const MapSeg &seg = g_maps[si];
      if (seg.path != path) continue;
      if (rec.addr < seg.vs || rec.addr >= seg.ve) continue;
      int64_t poff = (int64_t)seg.foff + (int64_t)(rec.addr - seg.vs);
      if (poff < foff || poff >= foff + (int64_t)len) continue;
      size_t offInMap = (size_t)(poff - foff);
      if (offInMap + rec.bytes.size() > len) continue;
      if (!touched) {
        syscall(__NR_mprotect, addr, len, PROT_READ | PROT_WRITE);
        touched = true;
      }
      memcpy((uint8_t *)addr + offInMap, rec.bytes.data(), rec.bytes.size());
      break;
    }
  }
  pthread_rwlock_unlock(&g_lock);

  if (touched) {
    syscall(__NR_mprotect, addr, len, PROT_READ);
  }
}

bool isVpnIface(const char *name) {
  if (!g_enabled || !g_vpn_enable || name == nullptr || *name == '\0') return false;
  std::string s(name);
  for (char &c : s) c = (char)tolower((unsigned char)c);
  pthread_rwlock_rdlock(&g_lock);
  bool hit = false;
  for (size_t i = 0; i < g_vpn.size() && !hit; ++i) {
    const std::string &p = g_vpn[i];
    if (p.empty()) continue;
    hit = (s == p) || (s.size() > p.size() && s.compare(0, p.size(), p) == 0);
  }
  pthread_rwlock_unlock(&g_lock);
  return hit;
}

int64_t realUptimeMs() {
  struct timespec ts;
  if (clock_gettime(CLOCK_MONOTONIC, &ts) != 0) return 0;
  return (int64_t)ts.tv_sec * 1000LL + ts.tv_nsec / 1000000LL;
}

typedef int (*prop_get_t)(const char *, char *);
prop_get_t orig_prop_get = nullptr;

int fake_prop_get(const char *name, char *value) {
  bool has = false;
  std::string v;
  if (g_enabled && name != nullptr) {
    pthread_rwlock_rdlock(&g_lock);
    std::map<std::string, std::string>::const_iterator it = g_props.find(name);
    if (it != g_props.end()) { has = true; v = it->second; }
    pthread_rwlock_unlock(&g_lock);
  }
  if (has) {
    if (value != nullptr) {
      size_t n = v.size();
      if (n >= PROP_VALUE_MAX) n = PROP_VALUE_MAX - 1;
      memcpy(value, v.data(), n);
      value[n] = '\0';
    }
    return (int)v.size();
  }
  if (orig_prop_get != nullptr) return orig_prop_get(name, value);
  return -1;
}

typedef int (*uname_t)(struct utsname *);
uname_t orig_uname = nullptr;

int fake_uname(struct utsname *u) {
  int r = orig_uname != nullptr ? orig_uname(u) : -1;
  if (!g_enabled || r != 0 || u == nullptr) return r;
  std::string kernel, arch;
  pthread_rwlock_rdlock(&g_lock);
  kernel = g_kernel;
  arch = g_arch;
  pthread_rwlock_unlock(&g_lock);
  if (!kernel.empty()) {
    strncpy(u->release, kernel.c_str(), sizeof(u->release) - 1);
    u->release[sizeof(u->release) - 1] = '\0';
    strncpy(u->version, "#1 SMP PREEMPT Mon Jan 1 00:00:00 UTC 2024",
            sizeof(u->version) - 1);
    u->version[sizeof(u->version) - 1] = '\0';
  }
  if (!arch.empty()) {
    strncpy(u->machine, arch.c_str(), sizeof(u->machine) - 1);
    u->machine[sizeof(u->machine) - 1] = '\0';
  }
  return r;
}

typedef int (*gettimeofday_t)(struct timeval *, struct timezone *);
typedef int (*clock_gettime_t)(clockid_t, struct timespec *);
typedef int (*sysinfo_t)(struct sysinfo *);

gettimeofday_t orig_gettimeofday = nullptr;
clock_gettime_t orig_clock_gettime = nullptr;
sysinfo_t orig_sysinfo = nullptr;

int fake_gettimeofday(struct timeval *tv, struct timezone *tz) {
  int r = orig_gettimeofday != nullptr ? orig_gettimeofday(tv, tz) : -1;
  if (r != 0 || tv == nullptr || !g_enabled || !g_time_enable) return r;
  int64_t off = g_time_offset_ms;
  if (off == 0) return r;
  int64_t us = (int64_t)tv->tv_sec * 1000000LL + tv->tv_usec + off * 1000LL;
  tv->tv_sec = (time_t)(us / 1000000LL);
  tv->tv_usec = (suseconds_t)(us % 1000000LL);
  return r;
}

int fake_clock_gettime(clockid_t clk, struct timespec *ts) {
  int r = orig_clock_gettime != nullptr ? orig_clock_gettime(clk, ts) : -1;
  if (r != 0 || ts == nullptr || !g_enabled) return r;

  if (g_time_enable && (clk == CLOCK_REALTIME || clk == CLOCK_REALTIME_ALARM)) {
    int64_t off = g_time_offset_ms;
    if (off != 0) {
      int64_t ns = (int64_t)ts->tv_sec * 1000000000LL + ts->tv_nsec + off * 1000000LL;
      ts->tv_sec = (time_t)(ns / 1000000000LL);
      ts->tv_nsec = (long)(ns % 1000000000LL);
    }
    return r;
  }

  if (g_uptime_enable &&
      (clk == CLOCK_MONOTONIC || clk == CLOCK_MONOTONIC_RAW ||
       clk == CLOCK_BOOTTIME || clk == CLOCK_MONOTONIC_COARSE)) {
    int64_t delta = realUptimeMs() - g_uptime_base_ms;
    if (delta < 0) delta = 0;
    int64_t ms = g_uptime_target_ms + delta;
    ts->tv_sec = (time_t)(ms / 1000LL);
    ts->tv_nsec = (long)((ms % 1000LL) * 1000000LL);
  }
  return r;
}

int fake_sysinfo(struct sysinfo *info) {
  int r = orig_sysinfo != nullptr ? orig_sysinfo(info) : -1;
  if (r != 0 || info == nullptr || !g_enabled || !g_uptime_enable) return r;
  int64_t delta = realUptimeMs() - g_uptime_base_ms;
  if (delta < 0) delta = 0;
  info->uptime = (long)((g_uptime_target_ms + delta) / 1000LL);
  return r;
}

typedef int (*open_t)(const char *, int, ...);
typedef int (*openat_t)(int, const char *, int, ...);
typedef FILE *(*fopen_t)(const char *, const char *);

open_t orig_open = nullptr;
open_t orig_open64 = nullptr;
openat_t orig_openat = nullptr;
openat_t orig_openat64 = nullptr;
fopen_t orig_fopen = nullptr;

int fake_open(const char *path, int flags, ...) {
  mode_t mode = 0;
  if (flags & O_CREAT) {
    va_list ap;
    va_start(ap, flags);
    mode = (mode_t)va_arg(ap, int);
    va_end(ap);
  }
  if (isHidden(path)) {
    errno = ENOENT;
    return -1;
  }
  SpoofFile *f = findSpoof(path);
  if (f != nullptr) {
    const char *rp = materialize(f);
    if (rp != nullptr) {
      if (orig_open != nullptr) return orig_open(rp, flags, mode);
      return (int)syscall(__NR_openat, AT_FDCWD, rp, flags, mode);
    }
  }
  if (orig_open != nullptr) {
    int fd = orig_open(path, flags, mode);
    if (fd >= 0 && g_enabled && g_anti_detect && path != nullptr) {
      pthread_rwlock_wrlock(&g_lock);
      g_fdpath[fd] = std::string(path);
      pthread_rwlock_unlock(&g_lock);
    }
    return fd;
  }
  return (int)syscall(__NR_openat, AT_FDCWD, path, flags, mode);
}

int fake_openat(int dirfd, const char *path, int flags, ...) {
  mode_t mode = 0;
  if (flags & O_CREAT) {
    va_list ap;
    va_start(ap, flags);
    mode = (mode_t)va_arg(ap, int);
    va_end(ap);
  }
  if (isHidden(path)) {
    errno = ENOENT;
    return -1;
  }
  SpoofFile *f = findSpoof(path);
  if (f != nullptr) {
    const char *rp = materialize(f);
    if (rp != nullptr && orig_openat != nullptr) return orig_openat(dirfd, rp, flags, mode);
  }
  if (orig_openat != nullptr) {
    int fd = orig_openat(dirfd, path, flags, mode);
    if (fd >= 0 && g_enabled && g_anti_detect && path != nullptr) {
      pthread_rwlock_wrlock(&g_lock);
      g_fdpath[fd] = std::string(path);
      pthread_rwlock_unlock(&g_lock);
    }
    return fd;
  }
  return (int)syscall(__NR_openat, dirfd, path, flags, mode);
}

FILE *fake_fopen(const char *path, const char *mode) {
  if (isHidden(path)) {
    errno = ENOENT;
    return nullptr;
  }
  SpoofFile *f = findSpoof(path);
  if (f != nullptr) {
    const char *rp = materialize(f);
    if (rp != nullptr && orig_fopen != nullptr) return orig_fopen(rp, mode);
  }
  return orig_fopen != nullptr ? orig_fopen(path, mode) : nullptr;
}

typedef int (*access_t)(const char *, int);
typedef int (*faccessat_t)(int, const char *, int, int);
typedef int (*stat_t)(const char *, struct stat *);
typedef int (*fstatat_t)(int, const char *, struct stat *, int);
typedef ssize_t (*readlink_t)(const char *, char *, size_t);
typedef char *(*realpath_t)(const char *, char *);

access_t orig_access = nullptr;
faccessat_t orig_faccessat = nullptr;
stat_t orig_stat = nullptr;
stat_t orig_lstat = nullptr;
fstatat_t orig_fstatat = nullptr;
readlink_t orig_readlink = nullptr;
realpath_t orig_realpath = nullptr;

int fake_access(const char *path, int mode) {
  if (isHidden(path)) {
    errno = ENOENT;
    return -1;
  }
  if (isForceExist(path)) return 0;
  return orig_access != nullptr ? orig_access(path, mode) : -1;
}

int fake_faccessat(int dirfd, const char *path, int mode, int flags) {
  if (isHidden(path)) {
    errno = ENOENT;
    return -1;
  }
  if (isForceExist(path)) return 0;
  return orig_faccessat != nullptr ? orig_faccessat(dirfd, path, mode, flags) : -1;
}

int fake_stat(const char *path, struct stat *buf) {
  if (isHidden(path)) {
    errno = ENOENT;
    return -1;
  }
  return orig_stat != nullptr ? orig_stat(path, buf) : -1;
}

int fake_lstat(const char *path, struct stat *buf) {
  if (isHidden(path)) {
    errno = ENOENT;
    return -1;
  }
  return orig_lstat != nullptr ? orig_lstat(path, buf) : -1;
}

int fake_fstatat(int dirfd, const char *path, struct stat *buf, int flags) {
  if (isHidden(path)) {
    errno = ENOENT;
    return -1;
  }
  return orig_fstatat != nullptr ? orig_fstatat(dirfd, path, buf, flags) : -1;
}

ssize_t fake_readlink(const char *path, char *buf, size_t size) {
  if (isHidden(path)) {
    errno = ENOENT;
    return -1;
  }
  return orig_readlink != nullptr ? orig_readlink(path, buf, size) : -1;
}

char *fake_realpath(const char *path, char *resolved) {
  if (isHidden(path)) {
    errno = ENOENT;
    return nullptr;
  }
  return orig_realpath != nullptr ? orig_realpath(path, resolved) : nullptr;
}

typedef DIR *(*opendir_t)(const char *);
typedef struct dirent *(*readdir_t)(DIR *);

opendir_t orig_opendir = nullptr;
readdir_t orig_readdir = nullptr;
readdir_t orig_readdir64 = nullptr;

DIR *fake_opendir(const char *path) {
  if (isHidden(path)) {
    errno = ENOENT;
    return nullptr;
  }
  DIR *d = orig_opendir != nullptr ? orig_opendir(path) : nullptr;
  if (d != nullptr && g_enabled && path != nullptr) {
    pthread_rwlock_wrlock(&g_lock);
    g_dirs[d] = std::string(path);
    pthread_rwlock_unlock(&g_lock);
  }
  return d;
}

struct dirent *fake_readdir(DIR *d) {
  if (orig_readdir == nullptr || d == nullptr) return nullptr;
  if (!g_enabled) return orig_readdir(d);

  std::string base;
  bool known = false;
  pthread_rwlock_rdlock(&g_lock);
  std::map<DIR *, std::string>::const_iterator it = g_dirs.find(d);
  if (it != g_dirs.end()) { base = it->second; known = true; }
  pthread_rwlock_unlock(&g_lock);
  if (!known || base.empty()) return orig_readdir(d);

  for (int i = 0; i < 64; ++i) {
    struct dirent *e = orig_readdir(d);
    if (e == nullptr) return nullptr;
    if (e->d_name[0] == '\0') return e;
    std::string full = base;
    if (full[full.size() - 1] != '/') full += '/';
    full += e->d_name;
    if (isHidden(full.c_str())) continue;
    return e;
  }
  return nullptr;
}

typedef int (*execve_t)(const char *, char *const[], char *const[]);
typedef int (*execv_t)(const char *, char *const[]);
typedef int (*execvp_t)(const char *, char *const[]);
typedef int (*system_t)(const char *);
typedef FILE *(*popen_t)(const char *, const char *);
typedef int (*posix_spawn_t)(pid_t *, const char *, const posix_spawn_file_actions_t *,
                             const posix_spawnattr_t *, char *const[], char *const[]);

execve_t orig_execve = nullptr;
execv_t orig_execv = nullptr;
execvp_t orig_execvp = nullptr;
system_t orig_system = nullptr;
popen_t orig_popen = nullptr;
posix_spawn_t orig_posix_spawn = nullptr;

bool cmdHitsHidden(const char *cmd) {
  if (!g_enabled || cmd == nullptr || *cmd == '\0') return false;
  std::string s(cmd);
  pthread_rwlock_rdlock(&g_lock);
  bool hit = false;
  for (size_t i = 0; i < g_hide.size() && !hit; ++i) {
    if (g_hide[i].empty()) continue;
    hit = s.find(g_hide[i]) != std::string::npos;
  }
  pthread_rwlock_unlock(&g_lock);
  return hit;
}

bool cmdHitsHiddenArgv(char *const argv[]) {
  if (argv == nullptr) return false;
  for (int i = 0; argv[i] != nullptr && i < 64; ++i) {
    if (cmdHitsHidden(argv[i])) return true;
  }
  return false;
}

bool isKillCmd(const char *file, char *const argv[]) {
  if (!g_enabled || !g_block_exit) return false;

  auto killish = [](const char *s) -> bool {
    if (s == nullptr) return false;
    const char *b = strrchr(s, '/');
    b = (b != nullptr) ? b + 1 : s;
    return strcmp(b, "kill") == 0 || strcmp(b, "killall") == 0 ||
           strcmp(b, "pkill") == 0 || strcmp(b, "killall5") == 0;
  };

  if (killish(file)) return true;

  if (argv != nullptr) {
    for (int i = 0; argv[i] != nullptr && i < 16; ++i) {
      const char *a = argv[i];
      if (a == nullptr) continue;
      if (killish(a)) return true;
      if (strstr(a, "force-stop") != nullptr) return true;
      if (strstr(a, "am kill") != nullptr) return true;
      const char *p = a;
      while (*p == ' ' || *p == '\t') ++p;
      if (strncmp(p, "kill ", 5) == 0 || strncmp(p, "pkill ", 6) == 0 ||
          strncmp(p, "killall ", 8) == 0) {
        return true;
      }
    }
  }
  return false;
}

int fake_execve(const char *path, char *const argv[], char *const envp[]) {
  if (isHidden(path) || cmdHitsHiddenArgv(argv)) {
    errno = ENOENT;
    return -1;
  }
  if (g_enabled && g_block_exec) {
    errno = EACCES;
    return -1;
  }
  if (isKillCmd(path, argv)) {
    LOGW("blocked kill command: %s", path != nullptr ? path : "(null)");
    errno = EACCES;
    return -1;
  }
  return orig_execve != nullptr ? orig_execve(path, argv, envp) : -1;
}

int fake_execv(const char *path, char *const argv[]) {
  if (isHidden(path) || cmdHitsHiddenArgv(argv)) {
    errno = ENOENT;
    return -1;
  }
  if (g_enabled && g_block_exec) {
    errno = EACCES;
    return -1;
  }
  if (isKillCmd(path, argv)) {
    LOGW("blocked kill command: %s", path != nullptr ? path : "(null)");
    errno = EACCES;
    return -1;
  }
  return orig_execv != nullptr ? orig_execv(path, argv) : -1;
}

int fake_execvp(const char *file, char *const argv[]) {
  if (cmdHitsHiddenArgv(argv)) {
    errno = ENOENT;
    return -1;
  }
  if (g_enabled && g_block_exec) {
    errno = EACCES;
    return -1;
  }
  if (isKillCmd(file, argv)) {
    LOGW("blocked kill command: %s", file != nullptr ? file : "(null)");
    errno = EACCES;
    return -1;
  }
  return orig_execvp != nullptr ? orig_execvp(file, argv) : -1;
}

bool cmdIsKill(const char *cmd) {
  if (cmd == nullptr || *cmd == '\0') return false;
  const char *p = cmd;
  while (*p == ' ' || *p == '\t') ++p;
  if (strncmp(p, "kill ", 5) == 0 || strncmp(p, "pkill ", 6) == 0 ||
      strncmp(p, "killall ", 8) == 0 || strcmp(p, "kill") == 0) {
    return true;
  }
  return strstr(p, "force-stop") != nullptr || strstr(p, "am kill") != nullptr;
}

int fake_system(const char *cmd) {
  if (cmdHitsHidden(cmd)) return 1;
  if (g_enabled && g_block_exec) return 1;
  if (cmdIsKill(cmd)) {
    LOGW("blocked kill command: %s", cmd);
    return 1;
  }
  return orig_system != nullptr ? orig_system(cmd) : -1;
}

FILE *fake_popen(const char *cmd, const char *type) {
  if (cmdHitsHidden(cmd) || (g_enabled && g_block_exec) || cmdIsKill(cmd)) {
    if (cmdIsKill(cmd)) LOGW("blocked kill command: %s", cmd != nullptr ? cmd : "");
    ScopedBypass bp;
    return orig_popen != nullptr ? orig_popen("true", type) : nullptr;
  }
  return orig_popen != nullptr ? orig_popen(cmd, type) : nullptr;
}

int fake_posix_spawn(pid_t *pid, const char *path,
                     const posix_spawn_file_actions_t *fa,
                     const posix_spawnattr_t *attr, char *const argv[],
                     char *const envp[]) {
  if (isHidden(path) || cmdHitsHiddenArgv(argv)) {
    errno = ENOENT;
    return -1;
  }
  if (g_enabled && g_block_exec) {
    errno = EACCES;
    return -1;
  }
  if (isKillCmd(path, argv)) {
    LOGW("blocked kill command: %s", path != nullptr ? path : "(null)");
    errno = EACCES;
    return -1;
  }
  return orig_posix_spawn != nullptr
             ? orig_posix_spawn(pid, path, fa, attr, argv, envp)
             : -1;
}

typedef ssize_t (*read_t)(int, void *, size_t);
typedef ssize_t (*pread_t)(int, void *, size_t, off_t);
typedef void *(*mmap_t)(void *, size_t, int, int, int, off_t);
typedef int (*close_t)(int);

read_t orig_read = nullptr;
pread_t orig_pread = nullptr;
pread_t orig_pread64 = nullptr;
mmap_t orig_mmap = nullptr;
close_t orig_close = nullptr;

ssize_t fake_read(int fd, void *buf, size_t count) {
  ssize_t n = orig_read != nullptr ? orig_read(fd, buf, count) : -1;
  if (n > 0) {
    off_t pos = lseek(fd, 0, SEEK_CUR);
    if (pos >= 0) syncBuffer(fd, buf, (size_t)n, (int64_t)(pos - n));
  }
  return n;
}

ssize_t fake_pread(int fd, void *buf, size_t count, off_t offset) {
  ssize_t n = orig_pread != nullptr ? orig_pread(fd, buf, count, offset) : -1;
  if (n > 0) syncBuffer(fd, buf, (size_t)n, (int64_t)offset);
  return n;
}

ssize_t fake_pread64(int fd, void *buf, size_t count, off_t offset) {
  ssize_t n = orig_pread64 != nullptr ? orig_pread64(fd, buf, count, offset) : -1;
  if (n > 0) syncBuffer(fd, buf, (size_t)n, (int64_t)offset);
  return n;
}

void *fake_mmap(void *addr, size_t len, int prot, int flags, int fd, off_t off) {
  void *r = orig_mmap != nullptr ? orig_mmap(addr, len, prot, flags, fd, off) : MAP_FAILED;
  if (r != MAP_FAILED && fd >= 0 && (prot & PROT_READ) != 0) {
    syncMapped(r, len, fd, (int64_t)off);
  }
  return r;
}

int fake_close(int fd) {
  if (g_enabled && g_anti_detect) {
    pthread_rwlock_wrlock(&g_lock);
    g_fdpath.erase(fd);
    pthread_rwlock_unlock(&g_lock);
  }
  return orig_close != nullptr ? orig_close(fd) : -1;
}

typedef int (*getifaddrs_t)(struct ifaddrs **);
typedef void (*freeifaddrs_t)(struct ifaddrs *);
typedef unsigned (*if_nametoindex_t)(const char *);

getifaddrs_t orig_getifaddrs = nullptr;
if_nametoindex_t orig_if_nametoindex = nullptr;

int fake_getifaddrs(struct ifaddrs **out) {
  int r = orig_getifaddrs != nullptr ? orig_getifaddrs(out) : -1;
  if (r != 0 || out == nullptr || *out == nullptr) return r;
  if (!g_enabled || !g_vpn_enable) return r;

  struct ifaddrs dummy;
  dummy.ifa_next = *out;
  struct ifaddrs *prev = &dummy;
  struct ifaddrs *cur = *out;
  while (cur != nullptr) {
    if (isVpnIface(cur->ifa_name)) {
      prev->ifa_next = cur->ifa_next;
      cur = prev->ifa_next;
    } else {
      prev = cur;
      cur = cur->ifa_next;
    }
  }
  *out = dummy.ifa_next;
  return r;
}

unsigned fake_if_nametoindex(const char *name) {
  if (isVpnIface(name)) return 0;
  return orig_if_nametoindex != nullptr ? orig_if_nametoindex(name) : 0;
}

typedef void (*exit_t)(int);
typedef void (*abort_t)(void);
typedef int (*kill_t)(pid_t, int);

exit_t orig_exit = nullptr;
exit_t orig__exit = nullptr;
abort_t orig_abort = nullptr;
kill_t orig_kill = nullptr;

void fake_exit(int code) {
  if (g_enabled && g_block_exit) {
    LOGW("blocked exit(%d)", code);
    return;
  }
  if (g_enabled) LOGD("exit(%d)", code);
  if (orig_exit != nullptr) orig_exit(code);
  _exit(code);
}

void fake__exit(int code) {
  if (g_enabled && g_block_exit) {
    LOGW("blocked _exit(%d)", code);
    return;
  }
  if (orig__exit != nullptr) orig__exit(code);
  _exit(code);
}

void fake_abort() {
  if (g_enabled && g_block_exit) {
    LOGW("blocked abort()");
    return;
  }
  if (orig_abort != nullptr) orig_abort();
  abort();
}

int fake_kill(pid_t pid, int sig) {
  if (g_enabled && g_block_exit && (pid == getpid() || pid == 0)) {
    LOGW("blocked kill(self, %d)", sig);
    return 0;
  }
  return orig_kill != nullptr ? orig_kill(pid, sig) : -1;
}

typedef const unsigned char *(*gl_get_string_t)(unsigned int);
typedef const char *(*egl_query_string_t)(void *, int);

gl_get_string_t orig_gl_get_string = nullptr;
egl_query_string_t orig_egl_query_string = nullptr;

static char g_gpu_buf[256] = {0};
static char g_gpu_vendor_buf[64] = {0};
static bool g_gpu_buf_ready = false;

void refreshGpuBuf() {
  pthread_rwlock_rdlock(&g_lock);
  std::string gpu = g_gpu;
  pthread_rwlock_unlock(&g_lock);
  if (gpu.empty()) {
    g_gpu_buf_ready = false;
    g_gpu_buf[0] = '\0';
    return;
  }
  size_t n = gpu.size();
  if (n >= sizeof(g_gpu_buf)) n = sizeof(g_gpu_buf) - 1;
  memcpy(g_gpu_buf, gpu.data(), n);
  g_gpu_buf[n] = '\0';

  const char *v = strstr(g_gpu_buf, "Adreno") != nullptr ? "Qualcomm"
                  : strstr(g_gpu_buf, "Mali") != nullptr ? "ARM"
                  : strstr(g_gpu_buf, "Immortalis") != nullptr ? "ARM"
                  : strstr(g_gpu_buf, "Maleoon") != nullptr ? "HiSilicon"
                  : strstr(g_gpu_buf, "PowerVR") != nullptr
                      ? "Imagination Technologies"
                      : "ARM";
  strncpy(g_gpu_vendor_buf, v, sizeof(g_gpu_vendor_buf) - 1);
  g_gpu_vendor_buf[sizeof(g_gpu_vendor_buf) - 1] = '\0';
  g_gpu_buf_ready = true;
}

const unsigned char *fake_gl_get_string(unsigned int name) {
  if (g_enabled && g_gpu_buf_ready) {
    if (name == 0x1F01) return (const unsigned char *)g_gpu_buf;
    if (name == 0x1F00) return (const unsigned char *)g_gpu_vendor_buf;
  }
  return orig_gl_get_string != nullptr ? orig_gl_get_string(name) : nullptr;
}

const char *fake_egl_query_string(void *dpy, int name) {
  if (g_enabled && g_gpu_buf_ready && name == 0x305D) {
    return g_gpu_buf;
  }
  return orig_egl_query_string != nullptr ? orig_egl_query_string(dpy, name) : nullptr;
}

void *resolveGfx(const char *lib, const char *sym, bool tryLoad) {
  void *p = DobbySymbolResolver(lib, sym);
  if (p == nullptr && tryLoad) {
    ScopedBypass bp;
    void *h = dlopen(lib, RTLD_LAZY);
    if (h != nullptr) {
      p = dlsym(h, sym);
    }
  }
  return p;
}

bool hookGpu() {
  if (!g_enabled) return false;

  if (orig_gl_get_string == nullptr) {
    void *p = resolveGfx("libGLESv2.so", "glGetString", true);
    if (p == nullptr) p = resolveGfx("libGLESv1_CM.so", "glGetString", true);
    if (p != nullptr) {
      DobbyHook(p, (dobby_dummy_func_t)fake_gl_get_string,
                (dobby_dummy_func_t *)&orig_gl_get_string);
    }
  }
  if (orig_egl_query_string == nullptr) {
    void *q = resolveGfx("libEGL.so", "eglQueryString", true);
    if (q != nullptr) {
      DobbyHook(q, (dobby_dummy_func_t)fake_egl_query_string,
                (dobby_dummy_func_t *)&orig_egl_query_string);
    }
  }
  return orig_gl_get_string != nullptr || orig_egl_query_string != nullptr;
}

struct sigaction g_old[NSIG];
volatile bool g_signal_installed = false;

void crashHandler(int sig, siginfo_t *info, void *ctx) {
  LOGE("native crash: signal=%d code=%d addr=%p", sig,
       info != nullptr ? info->si_code : -1, info != nullptr ? info->si_addr : nullptr);
  struct sigaction *old = &g_old[sig];
  if (old->sa_handler != nullptr && old->sa_handler != SIG_DFL &&
      old->sa_handler != SIG_IGN) {
    if (old->sa_flags & SA_SIGINFO) {
      if (old->sa_sigaction != nullptr) old->sa_sigaction(sig, info, ctx);
    } else {
      old->sa_handler(sig);
    }
    return;
  }
  signal(sig, SIG_DFL);
  raise(sig);
}

void installCrashHandlers() {
  if (g_signal_installed) return;
  const int sigs[] = {SIGSEGV, SIGABRT, SIGBUS, SIGFPE, SIGILL, SIGTRAP};
  for (size_t i = 0; i < sizeof(sigs) / sizeof(sigs[0]); ++i) {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = crashHandler;
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK;
    sigemptyset(&sa.sa_mask);
    if (sigaction(sigs[i], &sa, &g_old[sigs[i]]) == 0) {
      LOGD("crash handler installed for signal %d", sigs[i]);
    }
  }
  g_signal_installed = true;
}

bool hookSym(const char *name, void *fake, void **out) {
  if (name == nullptr || fake == nullptr || out == nullptr) return false;
  void *p = dlsym(RTLD_DEFAULT, name);
  if (p == nullptr) {
    LOGW("symbol not found: %s", name);
    return false;
  }
  if (DobbyHook(p, (dobby_dummy_func_t)fake, (dobby_dummy_func_t *)out) != RT_SUCCESS) {
    LOGW("hook failed: %s", name);
    return false;
  }
  g_hook_addrs.push_back((uintptr_t)p);
  return true;
}

int installHooks() {
  if (g_installed) return 0;
  g_installed = true;

  int n = 0, total = 0;
#define TRY(sym, fake, orig)                                                   \
  do {                                                                         \
    ++total;                                                                   \
    if (hookSym(sym, (void *)(fake), (void **)&(orig))) ++n;                    \
  } while (0)

  TRY("__system_property_get", fake_prop_get, orig_prop_get);
  TRY("uname", fake_uname, orig_uname);
  TRY("gettimeofday", fake_gettimeofday, orig_gettimeofday);
  TRY("clock_gettime", fake_clock_gettime, orig_clock_gettime);
  TRY("sysinfo", fake_sysinfo, orig_sysinfo);
  TRY("open", fake_open, orig_open);
  TRY("open64", fake_open, orig_open64);
  TRY("openat", fake_openat, orig_openat);
  TRY("openat64", fake_openat, orig_openat64);
  TRY("fopen", fake_fopen, orig_fopen);
  TRY("read", fake_read, orig_read);
  TRY("pread", fake_pread, orig_pread);
  TRY("pread64", fake_pread64, orig_pread64);
  TRY("mmap", fake_mmap, orig_mmap);
  TRY("close", fake_close, orig_close);
  TRY("access", fake_access, orig_access);
  TRY("faccessat", fake_faccessat, orig_faccessat);
  TRY("stat", fake_stat, orig_stat);
  TRY("lstat", fake_lstat, orig_lstat);
  TRY("fstatat", fake_fstatat, orig_fstatat);
  TRY("readlink", fake_readlink, orig_readlink);
  TRY("realpath", fake_realpath, orig_realpath);
  TRY("opendir", fake_opendir, orig_opendir);
  TRY("readdir", fake_readdir, orig_readdir);
  TRY("readdir64", fake_readdir, orig_readdir64);
  TRY("execve", fake_execve, orig_execve);
  TRY("execv", fake_execv, orig_execv);
  TRY("execvp", fake_execvp, orig_execvp);
  TRY("system", fake_system, orig_system);
  TRY("popen", fake_popen, orig_popen);
  TRY("posix_spawn", fake_posix_spawn, orig_posix_spawn);
  TRY("getifaddrs", fake_getifaddrs, orig_getifaddrs);
  TRY("if_nametoindex", fake_if_nametoindex, orig_if_nametoindex);
  TRY("exit", fake_exit, orig_exit);
  TRY("_exit", fake__exit, orig__exit);
  TRY("abort", fake_abort, orig_abort);
  TRY("kill", fake_kill, orig_kill);
#undef TRY

  if (orig_open64 == nullptr) orig_open64 = orig_open;
  if (orig_openat64 == nullptr) orig_openat64 = orig_openat;
  if (orig_readdir64 == nullptr) orig_readdir64 = orig_readdir;

  bool gpu = false;
  if (g_enabled) gpu = hookGpu();

  if (g_enabled && g_anti_detect) collectPatches();

  LOGI("native hooks installed: %d/%d, gpu=%d, anti=%d", n, total, gpu ? 1 : 0,
       g_anti_detect ? 1 : 0);
  if (n == 0) {
    LOGW("no native hook installed — 所有伪装将只走 Java 层");
  }
  return n;
}

void clearFiles() {
  for (size_t i = 0; i < g_files.size(); ++i) delete g_files[i];
  g_files.clear();
}

void applyPayload(const char *payload) {
  pthread_rwlock_wrlock(&g_lock);

  g_props.clear();
  g_hide.clear();
  g_exist.clear();
  clearFiles();
  g_kernel.clear();
  g_arch.clear();
  g_gpu.clear();
  g_time_offset_ms = 0;
  g_time_enable = false;
  g_uptime_enable = false;
  g_block_exec = false;
  g_block_exit = false;
  g_crash_catch = false;
  g_anti_detect = false;
  g_vpn.clear();
  g_vpn_enable = false;

  std::vector<std::string> lines = split(payload != nullptr ? payload : "", '\n');
  for (size_t i = 0; i < lines.size(); ++i) {
    const std::string &line = lines[i];
    if (line.size() < 2) continue;
    std::string body = line.substr(2);

    if (line.compare(0, 2, "K\t") == 0) {
      g_kernel = body;
    } else if (line.compare(0, 2, "A\t") == 0) {
      g_arch = body;
    } else if (line.compare(0, 2, "G\t") == 0) {
      g_gpu = body;
    } else if (line.compare(0, 2, "D\t") == 0) {
      g_cache = body;
    } else if (line.compare(0, 2, "T\t") == 0) {
      g_time_offset_ms = strtoll(body.c_str(), nullptr, 10);
      g_time_enable = true;
    } else if (line.compare(0, 2, "U\t") == 0) {
      g_uptime_target_ms = strtoll(body.c_str(), nullptr, 10);
      g_uptime_enable = g_uptime_target_ms > 0;
    } else if (line.compare(0, 2, "X\t") == 0) {
      g_block_exec = (body == "1");
    } else if (line.compare(0, 2, "E\t") == 0) {
      g_block_exit = (body == "1");
    } else if (line.compare(0, 2, "N\t") == 0) {
      g_crash_catch = (body == "1");
    } else if (line.compare(0, 2, "W\t") == 0) {
      g_anti_detect = (body == "1");
    } else if (line.compare(0, 2, "V\t") == 0) {
      g_vpn.clear();
      std::vector<std::string> parts = split(body, ',');
      for (size_t k = 0; k < parts.size(); ++k) {
        std::string p = parts[k];
        for (char &c : p) c = (char)tolower((unsigned char)c);
        if (!p.empty()) g_vpn.push_back(p);
      }
      g_vpn_enable = !g_vpn.empty();
    } else if (line.compare(0, 2, "P\t") == 0) {
      std::vector<std::string> kv = split(body, '\t');
      if (!kv.empty() && !kv[0].empty()) {
        g_props[kv[0]] = kv.size() >= 2 ? unescape(kv[1]) : std::string();
      }
    } else if (line.compare(0, 2, "H\t") == 0) {
      if (!body.empty()) g_hide.push_back(body);
    } else if (line.compare(0, 2, "S\t") == 0) {
      if (!body.empty()) g_exist.push_back(body);
    } else if (line.compare(0, 2, "C\t") == 0) {
      std::vector<std::string> kv = split(body, '\t');
      if (kv.size() >= 2 && !kv[0].empty()) {
        SpoofFile *f = new SpoofFile();
        f->path = kv[0];
        f->content = unescape(kv[1]);
        g_files.push_back(f);
      }
    }
  }

  g_uptime_base_ms = realUptimeMs();
  g_enabled = true;
  if (!g_gpu.empty()) refreshGpuBuf();

  LOGI("config applied: props=%zu hide=%zu exist=%zu files=%zu gpu=%d vpn=%zu",
       g_props.size(), g_hide.size(), g_exist.size(), g_files.size(),
       g_gpu.empty() ? 0 : 1, g_vpn.size());

  pthread_rwlock_unlock(&g_lock);
}

void clearAll() {
  pthread_rwlock_wrlock(&g_lock);
  g_props.clear();
  g_hide.clear();
  g_exist.clear();
  clearFiles();
  g_dirs.clear();
  g_kernel.clear();
  g_arch.clear();
  g_gpu.clear();
  g_vpn.clear();
  g_vpn_enable = false;
  g_anti_detect = false;
  g_patches.clear();
  g_fdpath.clear();
  g_time_offset_ms = 0;
  g_time_enable = false;
  g_uptime_enable = false;
  g_block_exec = false;
  g_block_exit = false;
  g_crash_catch = false;
  g_gpu_buf[0] = '\0';
  g_gpu_buf_ready = false;
  pthread_rwlock_unlock(&g_lock);
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_sunilxsk_lockperm_NativeBridge_applyConfig(JNIEnv *env, jclass,
                                                          jstring payload) {
  if (env == nullptr || payload == nullptr) return JNI_FALSE;
  const char *s = env->GetStringUTFChars(payload, nullptr);
  if (s == nullptr) return JNI_FALSE;
  applyPayload(s);
  env->ReleaseStringUTFChars(payload, s);
  installHooks();
  if (g_crash_catch) installCrashHandlers();
  return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_sunilxsk_lockperm_NativeBridge_setEnabled(JNIEnv *, jclass,
                                                         jboolean on) {
  g_enabled = (on == JNI_TRUE);
  if (!g_enabled) clearAll();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_sunilxsk_lockperm_NativeBridge_isReady(JNIEnv *, jclass) {
  return (g_installed && g_enabled) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_sunilxsk_lockperm_NativeBridge_nativeVersion(JNIEnv *env, jclass) {
  const char *v = DobbyGetVersion();
  return env->NewStringUTF(v != nullptr ? v : "unknown");
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_sunilxsk_lockperm_NativeBridge_nativeHookCount(JNIEnv *, jclass) {
  int n = 0;
  if (orig_prop_get) n++;
  if (orig_uname) n++;
  if (orig_gettimeofday) n++;
  if (orig_clock_gettime) n++;
  if (orig_sysinfo) n++;
  if (orig_open || orig_open64) n++;
  if (orig_openat || orig_openat64) n++;
  if (orig_fopen) n++;
  if (orig_access) n++;
  if (orig_faccessat) n++;
  if (orig_stat) n++;
  if (orig_lstat) n++;
  if (orig_fstatat) n++;
  if (orig_readlink) n++;
  if (orig_realpath) n++;
  if (orig_opendir) n++;
  if (orig_readdir || orig_readdir64) n++;
  if (orig_execve) n++;
  if (orig_execv) n++;
  if (orig_execvp) n++;
  if (orig_system) n++;
  if (orig_popen) n++;
  if (orig_posix_spawn) n++;
  if (orig_exit) n++;
  if (orig__exit) n++;
  if (orig_abort) n++;
  if (orig_kill) n++;
  if (orig_read) n++;
  if (orig_pread || orig_pread64) n++;
  if (orig_mmap) n++;
  if (orig_close) n++;
  if (orig_getifaddrs) n++;
  if (orig_if_nametoindex) n++;
  if (orig_gl_get_string) n++;
  if (orig_egl_query_string) n++;
  return n;
}

typedef int (*HookFunType)(void *func, void *replace, void **backup);
typedef int (*UnhookFunType)(void *func);
typedef void (*NativeOnModuleLoaded)(const char *name, void *handle);

typedef struct {
  uint32_t version;
  HookFunType hook_func;
  UnhookFunType unhook_func;
} NativeAPIEntries;

static void onModuleLoaded(const char *name, void *handle) {
  (void)handle;
  if (g_enabled && !orig_gl_get_string && name != nullptr &&
      strstr(name, "GLES") != nullptr) {
    hookGpu();
  }
}

extern "C" __attribute__((visibility("default"))) __attribute__((used))
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
  (void)entries;
  return onModuleLoaded;
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *) {
  return JNI_VERSION_1_6;
}