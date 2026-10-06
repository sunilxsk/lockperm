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
#include <sys/statvfs.h>
#include <sys/vfs.h>
#include <sys/syscall.h>
#include <sys/sysinfo.h>
#include <sys/system_properties.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <sys/utsname.h>
#include <time.h>
#include <unistd.h>

#define GRP_PROP (1u << 0)
#define GRP_UNAME (1u << 1)
#define GRP_TIME (1u << 2)
#define GRP_FILE (1u << 3)
#define GRP_STAT (1u << 4)
#define GRP_EXEC (1u << 6)
#define GRP_EXIT (1u << 7)
#define GRP_NET (1u << 8)
#define GRP_READ (1u << 9)
#define GRP_MMAP (1u << 10)
#define GRP_GPU (1u << 11)
#define GRP_SENSOR (1u << 12)

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

namespace {

volatile bool g_enabled = false;
volatile bool g_installed = false;
uint32_t g_groups = 0;
bool g_sensor_block = false;

pthread_rwlock_t g_lock = PTHREAD_RWLOCK_INITIALIZER;

std::map<std::string, std::string> g_props;
std::vector<std::string> g_exist;
std::string g_kernel;
std::string g_arch;
std::string g_gpu;
std::string g_gpu_vendor;
std::string g_gpu_gl_version;
std::string g_gpu_glsl;
uint32_t g_gpu_vk_api_cfg = 0;
uint32_t g_gpu_driver_cfg = 0;
uint32_t g_gpu_vendor_id_cfg = 0;
uint32_t g_gpu_device_id_cfg = 0;
uint32_t g_gpu_max_dim_cfg = 0;
uint32_t g_gpu_max_cube_cfg = 0;
uint32_t g_gpu_layers_cfg = 0;
uint32_t g_gpu_push_cfg = 0;
int g_gpu_memory_mb_cfg = 0;
std::string g_cache;

std::vector<std::string> g_vpn;
volatile bool g_vpn_enable = false;

volatile bool g_anti_detect = false;

struct PatchRec {
  uint64_t foff = 0;
  std::vector<uint8_t> bytes;
};

std::map<std::string, std::vector<PatchRec> *> g_path_patches;
std::vector<std::vector<PatchRec> *> g_patch_pool;

std::map<std::string, std::string> g_basename_index;

std::vector<uintptr_t> g_hook_addrs;

struct MapSeg {
  uintptr_t vs = 0;
  uintptr_t ve = 0;
  uint64_t foff = 0;
  bool exec = false;
  std::string path;
};
std::vector<MapSeg> g_maps;
volatile bool g_maps_ready = false;

std::map<int, std::string> g_fdpath;

struct SpoofFile;
std::map<int, SpoofFile *> g_fdspoof;

uint64_t g_stor_total = 0;
uint64_t g_stor_avail = 0;
bool g_stor_enable = false;

std::map<FILE *, SpoofFile *> g_fspoof;
std::map<FILE *, size_t> g_fspoof_pos;
std::map<int, size_t> g_fdspoof_pos;

volatile bool g_need_fdtrack = false;

volatile int64_t g_time_offset_ms = 0;
volatile bool g_time_enable = false;
volatile int64_t g_uptime_base_ms = 0;
volatile int64_t g_uptime_target_ms = 0;
volatile bool g_uptime_enable = false;

volatile bool g_block_exec = false;
volatile bool g_block_exit = false;
volatile bool g_crash_catch = false;

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

static inline bool pathInteresting(const char *p) {
  if (p == nullptr || p[0] != '/') return false;
  const char c1 = p[1];
  if (c1 == 'p') return strncmp(p, "/proc/", 6) == 0;
  if (c1 == 's') return strncmp(p, "/sys/", 5) == 0;
  if (c1 == 'd') return strncmp(p, "/dev/", 5) == 0;
  if (c1 == 'v') return strncmp(p, "/vendor/", 8) == 0;
  if (c1 == 'a') return strncmp(p, "/apex/", 6) == 0;
  return false;
}

bool isForceExist(const char *p) {
  if (!g_enabled || p == nullptr || *p == '\0') return false;
  if (!pathInteresting(p)) return false;
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
  if (!pathInteresting(p)) return nullptr;
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
  if (f->ready && !f->real.empty()) return f->real.c_str();

  std::vector<std::string> dirs;
  pthread_rwlock_rdlock(&g_lock);
  if (!g_cache.empty()) dirs.push_back(g_cache);
  pthread_rwlock_unlock(&g_lock);
  const char *tmp = getenv("TMPDIR");
  if (tmp != nullptr && *tmp != '\0') dirs.push_back(tmp);
  dirs.push_back("/data/local/tmp");
  dirs.push_back("/data/local");

  char name[64];
  snprintf(name, sizeof(name), ".lk_%08x", fnv(f->path.c_str()));
  for (size_t i = 0; i < dirs.size(); ++i) {
    std::string p = dirs[i] + "/" + name;
    if (rawWriteFile(p.c_str(), f->content) != 0) continue;
    f->real = p;
    f->ready = true;
    pthread_rwlock_wrlock(&g_lock);
    if (g_cache.empty()) g_cache = dirs[i];
    pthread_rwlock_unlock(&g_lock);
    return f->real.c_str();
  }
  f->real.clear();
  return nullptr;
}

bool spoofCommand(const char *cmd, std::string &out);
bool spoofArgv(char *const argv[], std::vector<std::string> &keep,
               std::vector<char *> &ptrs);

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
          seg.exec = (strchr(perm, 'x') != nullptr);
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

std::string baseNameOf(const std::string &p) {
  size_t i = p.rfind('/');
  return (i == std::string::npos) ? p : p.substr(i + 1);
}

void collectPatches() {
  ensureMaps();

  pthread_rwlock_wrlock(&g_lock);
  g_path_patches.clear();
  for (size_t i = 0; i < g_patch_pool.size(); ++i) g_patch_pool[i]->clear();
  g_basename_index.clear();
  const size_t kSample = 48;
  size_t n = 0;
  for (size_t i = 0; i < g_hook_addrs.size(); ++i) {
    uintptr_t addr = g_hook_addrs[i];
    if (addr == 0) continue;
    if (addr < 4096) continue;

    const MapSeg *best = nullptr;
    for (size_t si = 0; si < g_maps.size(); ++si) {
      const MapSeg &seg = g_maps[si];
      if (addr < seg.vs || addr >= seg.ve) continue;
      if (seg.exec) { best = &seg; break; }
      if (best == nullptr) best = &seg;
    }
    if (best == nullptr) continue;

    PatchRec rec;
    rec.foff = best->foff + (uint64_t)(addr - best->vs);
    rec.bytes.resize(kSample);
    memcpy(rec.bytes.data(), (const void *)addr, kSample);

    std::vector<PatchRec> *&vec = g_path_patches[best->path];
    if (vec == nullptr) {
      vec = new std::vector<PatchRec>();
      g_patch_pool.push_back(vec);
    }
    vec->push_back(rec);
    ++n;
  }

  std::map<std::string, int> cnt;
  for (std::map<std::string, std::vector<PatchRec> *>::const_iterator it =
           g_path_patches.begin();
       it != g_path_patches.end(); ++it) {
    ++cnt[baseNameOf(it->first)];
  }
  for (std::map<std::string, std::vector<PatchRec> *>::const_iterator it =
           g_path_patches.begin();
       it != g_path_patches.end(); ++it) {
    const std::string &bn = baseNameOf(it->first);
    if (cnt[bn] == 1) g_basename_index[bn] = it->first;
  }
  LOGI("anti-detect: collected %zu patched entries in %zu files", n,
       g_path_patches.size());
  pthread_rwlock_unlock(&g_lock);
}

std::string fdPath(int fd) {
  pthread_rwlock_rdlock(&g_lock);
  std::string p;
  std::map<int, std::string>::const_iterator it = g_fdpath.find(fd);
  if (it != g_fdpath.end()) p = it->second;
  pthread_rwlock_unlock(&g_lock);
  return p;
}

void trackFd(int fd, const std::string &path, SpoofFile *spoof) {
  if (fd < 0) return;
  const bool wantPath = g_need_fdtrack && !path.empty() &&
                        (g_anti_detect || spoof != nullptr);
  if (!wantPath && spoof == nullptr) return;
  pthread_rwlock_wrlock(&g_lock);
  if (wantPath) g_fdpath[fd] = path;
  if (spoof != nullptr) {
    g_fdspoof[fd] = spoof;
    g_fdspoof_pos[fd] = 0;
  }
  pthread_rwlock_unlock(&g_lock);
}

SpoofFile *spoofOf(int fd) {
  pthread_rwlock_rdlock(&g_lock);
  SpoofFile *sf = nullptr;
  std::map<int, SpoofFile *>::const_iterator it = g_fdspoof.find(fd);
  if (it != g_fdspoof.end()) sf = it->second;
  pthread_rwlock_unlock(&g_lock);
  return sf;
}

const std::vector<PatchRec> *patchesForPath(const std::string &path) {
  if (path.empty()) return nullptr;
  pthread_rwlock_rdlock(&g_lock);
  const std::vector<PatchRec> *res = nullptr;
  std::map<std::string, std::vector<PatchRec> *>::const_iterator it =
      g_path_patches.find(path);
  if (it != g_path_patches.end()) {
    res = it->second;
  } else {
    std::map<std::string, std::string>::const_iterator bit =
        g_basename_index.find(baseNameOf(path));
    if (bit != g_basename_index.end()) {
      std::map<std::string, std::vector<PatchRec> *>::const_iterator it2 =
          g_path_patches.find(bit->second);
      if (it2 != g_path_patches.end()) res = it2->second;
    }
  }
  pthread_rwlock_unlock(&g_lock);
  return res;
}

const std::vector<PatchRec> *patchesForFd(int fd) {
  return patchesForPath(fdPath(fd));
}

void syncBuffer(int fd, void *buf, size_t count, int64_t foff) {
  if (!g_enabled || !g_anti_detect) return;
  if (buf == nullptr || count == 0 || foff < 0) return;

  const std::vector<PatchRec> *ps = patchesForFd(fd);
  if (ps == nullptr || ps->empty()) return;

  for (size_t pi = 0; pi < ps->size(); ++pi) {
    const PatchRec &rec = (*ps)[pi];
    int64_t p = (int64_t)rec.foff;
    if (p + (int64_t)rec.bytes.size() <= foff) continue;
    if (p >= foff + (int64_t)count) continue;
    for (size_t i = 0; i < rec.bytes.size(); ++i) {
      int64_t f = p + (int64_t)i;
      if (f < foff || f >= foff + (int64_t)count) continue;
      ((uint8_t *)buf)[f - foff] = rec.bytes[i];
    }
  }
}

void syncMapped(void *addr, size_t len, int fd, int64_t foff, bool shared) {
  if (!g_enabled || !g_anti_detect) return;
  if (addr == nullptr || addr == MAP_FAILED || len == 0 || fd < 0 || foff < 0) return;
  if (shared) return;

  const std::vector<PatchRec> *ps = patchesForFd(fd);
  if (ps == nullptr || ps->empty()) return;

  bool touched = false;
  for (size_t pi = 0; pi < ps->size(); ++pi) {
    const PatchRec &rec = (*ps)[pi];
    int64_t p = (int64_t)rec.foff;
    if (p < foff || p >= foff + (int64_t)len) continue;
    size_t offInMap = (size_t)(p - foff);
    if (offInMap + rec.bytes.size() > len) continue;
    if (!touched) {
      syscall(__NR_mprotect, addr, len, PROT_READ | PROT_WRITE);
      touched = true;
    }
    memcpy((uint8_t *)addr + offInMap, rec.bytes.data(), rec.bytes.size());
  }

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
#if defined(__NR_clock_gettime)
  if (syscall(__NR_clock_gettime, CLOCK_MONOTONIC, &ts) != 0) return 0;
#else
  if (orig_clock_gettime != nullptr) {
    if (orig_clock_gettime(CLOCK_MONOTONIC, &ts) != 0) return 0;
  } else {
    return 0;
  }
#endif
  return (int64_t)ts.tv_sec * 1000LL + ts.tv_nsec / 1000000LL;
}

typedef int (*prop_get_t)(const char *, char *);
prop_get_t orig_prop_get = nullptr;

bool propLookup(const char *name, std::string &out) {
  if (!g_enabled || name == nullptr) return false;
  pthread_rwlock_rdlock(&g_lock);
  std::map<std::string, std::string>::const_iterator it = g_props.find(name);
  bool has = (it != g_props.end());
  if (has) out = it->second;
  pthread_rwlock_unlock(&g_lock);
  return has;
}

int fake_prop_get(const char *name, char *value) {
  std::string v;
  if (propLookup(name, v)) {
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

typedef const void *(*prop_find_t)(const char *);
typedef void (*prop_read_cb_t)(const void *, void (*)(void *, const char *,
                                                      const char *, uint32_t),
                               void *);
typedef int (*prop_read_t)(const void *, char *, char *);

prop_find_t orig_prop_find = nullptr;
prop_read_cb_t orig_prop_read_callback = nullptr;
prop_read_t orig_prop_read = nullptr;

std::map<const void *, std::string> g_prop_name;
pthread_mutex_t g_prop_name_lock = PTHREAD_MUTEX_INITIALIZER;

const void *fake_prop_find(const char *name) {
  const void *pi = orig_prop_find != nullptr ? orig_prop_find(name) : nullptr;
  if (pi == nullptr || name == nullptr || !g_enabled) return pi;
  std::string v;
  if (!propLookup(name, v)) return pi;
  pthread_mutex_lock(&g_prop_name_lock);
  g_prop_name[pi] = std::string(name);
  pthread_mutex_unlock(&g_prop_name_lock);
  return pi;
}

const char *nameOfPropInfo(const void *pi) {
  if (pi == nullptr) return nullptr;
  static thread_local std::string slot;
  pthread_mutex_lock(&g_prop_name_lock);
  std::map<const void *, std::string>::const_iterator it = g_prop_name.find(pi);
  if (it == g_prop_name.end()) {
    pthread_mutex_unlock(&g_prop_name_lock);
    return nullptr;
  }
  slot = it->second;
  pthread_mutex_unlock(&g_prop_name_lock);
  return slot.c_str();
}

void fake_prop_read_callback(const void *pi,
                             void (*cb)(void *, const char *, const char *,
                                        uint32_t),
                             void *cookie) {
  const char *name = nameOfPropInfo(pi);
  std::string v;
  if (name != nullptr && propLookup(name, v) && cb != nullptr) {
    cb(cookie, v.c_str(), v.c_str(), 0u);
    return;
  }
  if (orig_prop_read_callback != nullptr) orig_prop_read_callback(pi, cb, cookie);
}

int fake_prop_read(const void *pi, char *key, char *value) {
  const char *name = nameOfPropInfo(pi);
  std::string v;
  if (name != nullptr && propLookup(name, v)) {
    if (key != nullptr) strncpy(key, name, PROP_NAME_MAX - 1);
    if (value != nullptr) {
      size_t n = v.size();
      if (n >= PROP_VALUE_MAX) n = PROP_VALUE_MAX - 1;
      memcpy(value, v.data(), n);
      value[n] = '\0';
    }
    return (int)v.size();
  }
  return orig_prop_read != nullptr ? orig_prop_read(pi, key, value) : -1;
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
typedef time_t (*time_t_fn)(time_t *);
typedef clock_t (*clock_t_fn)(void);

gettimeofday_t orig_gettimeofday = nullptr;
clock_gettime_t orig_clock_gettime = nullptr;
sysinfo_t orig_sysinfo = nullptr;
time_t_fn orig_time = nullptr;
clock_t_fn orig_clock = nullptr;

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

time_t fake_time(time_t *out) {
  time_t r = orig_time != nullptr ? orig_time(nullptr) : (time_t)-1;
  if (r == (time_t)-1) return r;
  if (g_enabled && g_time_enable && g_time_offset_ms != 0) {
    r = r + (time_t)(g_time_offset_ms / 1000);
  }
  if (out != nullptr) *out = r;
  return r;
}

clock_t fake_clock(void) {
  if (g_enabled && g_uptime_enable) {
    int64_t delta = realUptimeMs() - g_uptime_base_ms;
    if (delta < 0) delta = 0;
    int64_t ms = g_uptime_target_ms + delta;
    return (clock_t)(ms * (CLOCKS_PER_SEC / 1000));
  }
  return orig_clock != nullptr ? orig_clock() : (clock_t)-1;
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

std::string resolvePath(int dirfd, const char *path) {
  if (path == nullptr) return std::string();
  if (path[0] == '/' || dirfd == AT_FDCWD) return std::string(path);
  std::string base = fdPath(dirfd);
  if (base.empty()) return std::string(path);
  if (base[base.size() - 1] != '/') base += '/';
  return base + path;
}

bool isSensorPath(const char *p) {
  if (p == nullptr || p[0] != '/') return false;
  const bool scoped = strncmp(p, "/sys/", 5) == 0 || strncmp(p, "/dev/", 5) == 0 ||
                      strncmp(p, "/proc/", 6) == 0 || strncmp(p, "/vendor/", 8) == 0;
  if (!scoped) return false;
  if (strstr(p, "/class/thermal/") != nullptr) return false;
  if (strstr(p, "sensor") != nullptr) return true;
  if (strstr(p, "/iio:device") != nullptr) return true;
  if (strstr(p, "/bus/iio/") != nullptr) return true;
  return false;
}

int fake_open(const char *path, int flags, ...) {
  mode_t mode = 0;
  if (flags & O_CREAT) {
    va_list ap;
    va_start(ap, flags);
    mode = (mode_t)va_arg(ap, int);
    va_end(ap);
  }
  SpoofFile *f = findSpoof(path);
  std::string abs;
  if (path != nullptr && (f != nullptr || (g_anti_detect && g_need_fdtrack))) {
    abs.assign(path);
  }
  if (f != nullptr) {
    const char *rp = materialize(f);
    if (rp != nullptr) {
      int fd = orig_open != nullptr
                   ? orig_open(rp, flags, mode)
                   : (int)syscall(__NR_openat, AT_FDCWD, rp, flags, mode);
      trackFd(fd, abs, f);
      return fd;
    }
  }
  if (g_sensor_block && isSensorPath(path)) {
    errno = EACCES;
    return -1;
  }
  int fd = orig_open != nullptr
               ? orig_open(path, flags, mode)
               : (int)syscall(__NR_openat, AT_FDCWD, path, flags, mode);
  trackFd(fd, abs, f);
  return fd;
}

int fake_openat(int dirfd, const char *path, int flags, ...) {
  mode_t mode = 0;
  if (flags & O_CREAT) {
    va_list ap;
    va_start(ap, flags);
    mode = (mode_t)va_arg(ap, int);
    va_end(ap);
  }
  SpoofFile *f = findSpoof(path);
  const bool needAbs =
      (f != nullptr) || g_sensor_block ||
      (g_anti_detect && g_need_fdtrack) ||
      (path != nullptr && path[0] != '/' && dirfd != AT_FDCWD);
  std::string abs;
  if (needAbs) {
    abs = resolvePath(dirfd, path);
    if (f == nullptr) f = findSpoof(abs.c_str());
  }
  if (f != nullptr) {
    const char *rp = materialize(f);
    if (rp != nullptr) {
      int fd = orig_openat != nullptr
                   ? orig_openat(dirfd, rp, flags, mode)
                   : (int)syscall(__NR_openat, AT_FDCWD, rp, flags, mode);
      trackFd(fd, abs, f);
      return fd;
    }
  }
  if (g_sensor_block && (isSensorPath(path) || isSensorPath(abs.c_str()))) {
    errno = EACCES;
    return -1;
  }
  int fd = orig_openat != nullptr
               ? orig_openat(dirfd, path, flags, mode)
               : (int)syscall(__NR_openat, dirfd, path, flags, mode);
  trackFd(fd, abs, f);
  return fd;
}

FILE *fake_fopen(const char *path, const char *mode) {
  std::string abs = (path != nullptr) ? std::string(path) : std::string();
  SpoofFile *f = findSpoof(path);
  if (f != nullptr) {
    const char *rp = materialize(f);
    if (rp != nullptr && orig_fopen != nullptr) {
      FILE *fp = orig_fopen(rp, mode);
      if (fp != nullptr) {
        trackFd(fileno(fp), abs, f);
        pthread_rwlock_wrlock(&g_lock);
        g_fspoof[fp] = f;
        g_fspoof_pos[fp] = 0;
        pthread_rwlock_unlock(&g_lock);
      }
      return fp;
    }
  }
  if (g_sensor_block && isSensorPath(path)) {
    errno = EACCES;
    return nullptr;
  }
  FILE *fp = orig_fopen != nullptr ? orig_fopen(path, mode) : nullptr;
  if (fp != nullptr) {
    trackFd(fileno(fp), abs, f);
    if (f != nullptr) {
      pthread_rwlock_wrlock(&g_lock);
      g_fspoof[fp] = f;
      g_fspoof_pos[fp] = 0;
      pthread_rwlock_unlock(&g_lock);
    }
  }
  return fp;
}

typedef int (*statvfs_t)(const char *, struct statvfs *);
typedef int (*statvfs64_t)(const char *, struct statvfs64 *);
typedef int (*statfs_t)(const char *, struct statfs *);
typedef int (*statfs64_t)(const char *, struct statfs64 *);

statvfs_t orig_statvfs = nullptr;
statvfs64_t orig_statvfs64 = nullptr;
statfs_t orig_statfs = nullptr;
statfs64_t orig_statfs64 = nullptr;

static bool isStoragePath(const char *p) {
  if (p == nullptr) return false;
  if (strncmp(p, "/data", 5) == 0) return true;
  if (strncmp(p, "/storage", 8) == 0) return true;
  if (strncmp(p, "/sdcard", 7) == 0) return true;
  if (strncmp(p, "/mnt/sdcard", 11) == 0) return true;
  if (strncmp(p, "/mnt/user", 9) == 0) return true;
  return false;
}

int fake_statvfs(const char *path, struct statvfs *st) {
  int r = orig_statvfs != nullptr ? orig_statvfs(path, st) : -1;
  if (r == 0 && g_stor_enable && st != nullptr && isStoragePath(path)) {
    unsigned long bs = st->f_bsize ? st->f_bsize : 4096UL;
    st->f_frsize = bs;
    st->f_blocks = (fsblkcnt_t)(g_stor_total / bs);
    st->f_bfree = (fsblkcnt_t)(g_stor_avail / bs);
    st->f_bavail = (fsblkcnt_t)(g_stor_avail / bs);
  }
  return r;
}

int fake_statvfs64(const char *path, struct statvfs64 *st) {
  int r = orig_statvfs64 != nullptr ? orig_statvfs64(path, st) : -1;
  if (r == 0 && g_stor_enable && st != nullptr && isStoragePath(path)) {
    unsigned long bs = st->f_bsize ? st->f_bsize : 4096UL;
    st->f_frsize = bs;
    st->f_blocks = g_stor_total / bs;
    st->f_bfree = g_stor_avail / bs;
    st->f_bavail = g_stor_avail / bs;
  }
  return r;
}

int fake_statfs(const char *path, struct statfs *st) {
  int r = orig_statfs != nullptr ? orig_statfs(path, st) : -1;
  if (r == 0 && g_stor_enable && st != nullptr && isStoragePath(path)) {
    unsigned long bs = st->f_bsize ? (unsigned long)st->f_bsize : 4096UL;
    st->f_blocks = g_stor_total / bs;
    st->f_bfree = g_stor_avail / bs;
    st->f_bavail = g_stor_avail / bs;
  }
  return r;
}

int fake_statfs64(const char *path, struct statfs64 *st) {
  int r = orig_statfs64 != nullptr ? orig_statfs64(path, st) : -1;
  if (r == 0 && g_stor_enable && st != nullptr && isStoragePath(path)) {
    unsigned long bs = st->f_bsize ? (unsigned long)st->f_bsize : 4096UL;
    st->f_blocks = g_stor_total / bs;
    st->f_bfree = g_stor_avail / bs;
    st->f_bavail = g_stor_avail / bs;
  }
  return r;
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
  if (isForceExist(path)) return 0;
  return orig_access != nullptr ? orig_access(path, mode) : -1;
}

int fake_faccessat(int dirfd, const char *path, int mode, int flags) {
  if (isForceExist(path)) return 0;
  return orig_faccessat != nullptr ? orig_faccessat(dirfd, path, mode, flags) : -1;
}

int fake_stat(const char *path, struct stat *buf) {
  return orig_stat != nullptr ? orig_stat(path, buf) : -1;
}

int fake_lstat(const char *path, struct stat *buf) {
  return orig_lstat != nullptr ? orig_lstat(path, buf) : -1;
}

int fake_fstatat(int dirfd, const char *path, struct stat *buf, int flags) {
  return orig_fstatat != nullptr ? orig_fstatat(dirfd, path, buf, flags) : -1;
}

ssize_t fake_readlink(const char *path, char *buf, size_t size) {
  return orig_readlink != nullptr ? orig_readlink(path, buf, size) : -1;
}

char *fake_realpath(const char *path, char *resolved) {
  return orig_realpath != nullptr ? orig_realpath(path, resolved) : nullptr;
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
  if (g_enabled && g_block_exec) {
    errno = EACCES;
    return -1;
  }
  if (isKillCmd(path, argv)) {
    LOGW("blocked kill command: %s", path != nullptr ? path : "(null)");
    errno = EACCES;
    return -1;
  }
  {
    std::vector<std::string> keep;
    std::vector<char *> ptrs;
    if (spoofArgv(argv, keep, ptrs)) {
      return orig_execve != nullptr ? orig_execve(path, ptrs.data(), envp) : -1;
    }
  }
  return orig_execve != nullptr ? orig_execve(path, argv, envp) : -1;
}

int fake_execv(const char *path, char *const argv[]) {
  if (g_enabled && g_block_exec) {
    errno = EACCES;
    return -1;
  }
  if (isKillCmd(path, argv)) {
    LOGW("blocked kill command: %s", path != nullptr ? path : "(null)");
    errno = EACCES;
    return -1;
  }
  {
    std::vector<std::string> keep;
    std::vector<char *> ptrs;
    if (spoofArgv(argv, keep, ptrs)) {
      return orig_execv != nullptr ? orig_execv(path, ptrs.data()) : -1;
    }
  }
  return orig_execv != nullptr ? orig_execv(path, argv) : -1;
}

int fake_execvp(const char *file, char *const argv[]) {
  if (g_enabled && g_block_exec) {
    errno = EACCES;
    return -1;
  }
  if (isKillCmd(file, argv)) {
    LOGW("blocked kill command: %s", file != nullptr ? file : "(null)");
    errno = EACCES;
    return -1;
  }
  {
    std::vector<std::string> keep;
    std::vector<char *> ptrs;
    if (spoofArgv(argv, keep, ptrs)) {
      return orig_execvp != nullptr ? orig_execvp(file, ptrs.data()) : -1;
    }
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

bool spoofCommand(const char *cmd, std::string &out) {
  if (!g_enabled || cmd == nullptr || *cmd == '\0') return false;
  std::string s(cmd);
  bool changed = false;
  pthread_rwlock_rdlock(&g_lock);
  std::vector<SpoofFile *> snap = g_files;
  pthread_rwlock_unlock(&g_lock);
  for (size_t i = 0; i < snap.size(); ++i) {
    SpoofFile *f = snap[i];
    if (f->path.empty()) continue;
    size_t pos = 0;
    while ((pos = s.find(f->path, pos)) != std::string::npos) {
      const char *real = materialize(f);
      if (real == nullptr || *real == '\0') break;
      s.replace(pos, f->path.size(), real);
      pos += strlen(real);
      changed = true;
    }
  }
  if (changed) out = s;
  return changed;
}

bool spoofArgv(char *const argv[], std::vector<std::string> &keep,
               std::vector<char *> &ptrs) {
  if (!g_enabled || argv == nullptr) return false;
  bool changed = false;
  for (int i = 0; argv[i] != nullptr; ++i) {
    keep.push_back(argv[i] != nullptr ? argv[i] : "");
  }
  pthread_rwlock_rdlock(&g_lock);
  std::vector<SpoofFile *> snap = g_files;
  pthread_rwlock_unlock(&g_lock);
  for (size_t k = 0; k < keep.size(); ++k) {
    for (size_t i = 0; i < snap.size(); ++i) {
      SpoofFile *f = snap[i];
      if (f->path.empty() || keep[k] != f->path) continue;
      const char *real = materialize(f);
      if (real != nullptr && *real != '\0') {
        keep[k] = real;
        changed = true;
      }
      break;
    }
  }
  if (!changed) return false;
  ptrs.clear();
  for (size_t k = 0; k < keep.size(); ++k) ptrs.push_back((char *)keep[k].c_str());
  ptrs.push_back(nullptr);
  return true;
}

int fake_system(const char *cmd) {
  if (g_enabled && g_block_exec) return 1;
  if (cmdIsKill(cmd)) {
    LOGW("blocked kill command: %s", cmd);
    return 1;
  }
  std::string patched;
  if (spoofCommand(cmd, patched)) {
    return orig_system != nullptr ? orig_system(patched.c_str()) : -1;
  }
  return orig_system != nullptr ? orig_system(cmd) : -1;
}

FILE *fake_popen(const char *cmd, const char *type) {
  if ((g_enabled && g_block_exec) || cmdIsKill(cmd)) {
    if (cmdIsKill(cmd)) LOGW("blocked kill command: %s", cmd != nullptr ? cmd : "");
    ScopedBypass bp;
    return orig_popen != nullptr ? orig_popen("true", type) : nullptr;
  }
  std::string patched;
  if (spoofCommand(cmd, patched)) {
    return orig_popen != nullptr ? orig_popen(patched.c_str(), type) : nullptr;
  }
  return orig_popen != nullptr ? orig_popen(cmd, type) : nullptr;
}

int fake_posix_spawn(pid_t *pid, const char *path,
                     const posix_spawn_file_actions_t *fa,
                     const posix_spawnattr_t *attr, char *const argv[],
                     char *const envp[]) {
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
typedef ssize_t (*readv_t)(int, const struct iovec *, int);
typedef void *(*mmap_t)(void *, size_t, int, int, int, off_t);
typedef int (*close_t)(int);
typedef long (*syscall_t)(long, ...);
typedef ssize_t (*read_chk_t)(int, void *, size_t, size_t);
typedef ssize_t (*pread_chk_t)(int, void *, size_t, off_t, size_t);

read_t orig_read = nullptr;
pread_t orig_pread = nullptr;
pread_t orig_pread64 = nullptr;
readv_t orig_readv = nullptr;
mmap_t orig_mmap = nullptr;
close_t orig_close = nullptr;
syscall_t orig_syscall = nullptr;
read_chk_t orig_read_chk = nullptr;
pread_chk_t orig_pread_chk = nullptr;
pread_chk_t orig_pread64_chk = nullptr;

ssize_t serveSpoof(int fd, SpoofFile *sf, void *buf, size_t count) {
  pthread_rwlock_wrlock(&g_lock);
  size_t &p = g_fdspoof_pos[fd];
  const std::string &c = sf->content;
  size_t n = 0;
  if (p < c.size()) {
    n = c.size() - p;
    if (n > count) n = count;
    memcpy(buf, c.data() + p, n);
    p += n;
  }
  pthread_rwlock_unlock(&g_lock);
  return (ssize_t)n;
}

SpoofFile *spoofOfStream(FILE *fp) {
  if (fp == nullptr || !g_enabled) return nullptr;
  pthread_rwlock_rdlock(&g_lock);
  std::map<FILE *, SpoofFile *>::const_iterator it = g_fspoof.find(fp);
  SpoofFile *sf = (it != g_fspoof.end()) ? it->second : nullptr;
  pthread_rwlock_unlock(&g_lock);
  return sf;
}

size_t serveSpoofStream(FILE *fp, SpoofFile *sf, char *buf, size_t count) {
  pthread_rwlock_wrlock(&g_lock);
  size_t &p = g_fspoof_pos[fp];
  const std::string &c = sf->content;
  size_t n = 0;
  if (p < c.size()) {
    n = c.size() - p;
    if (n > count) n = count;
    memcpy(buf, c.data() + p, n);
    p += n;
  }
  pthread_rwlock_unlock(&g_lock);
  return n;
}

typedef size_t (*fread_t)(void *, size_t, size_t, FILE *);
typedef char *(*fgets_t)(char *, int, FILE *);
typedef ssize_t (*getline_t)(char **, size_t *, FILE *);
typedef ssize_t (*getdelim_t)(char **, size_t *, int, FILE *);

fread_t orig_fread = nullptr;
fgets_t orig_fgets = nullptr;
getline_t orig_getline = nullptr;
getdelim_t orig_getdelim = nullptr;
fread_t orig_fread_unlocked = nullptr;
fgets_t orig_fgets_unlocked = nullptr;

size_t fake_fread(void *ptr, size_t size, size_t nmemb, FILE *stream) {
  SpoofFile *sf = spoofOfStream(stream);
  if (sf == nullptr || ptr == nullptr || size == 0 || nmemb == 0) {
    return orig_fread != nullptr
               ? orig_fread(ptr, size, nmemb, stream)
               : 0;
  }
  size_t want = size * nmemb;
  size_t got = serveSpoofStream(stream, sf, (char *)ptr, want);
  return got / size;
}

char *fake_fgets(char *buf, int n, FILE *stream) {
  SpoofFile *sf = spoofOfStream(stream);
  if (sf == nullptr || buf == nullptr || n <= 0) {
    return orig_fgets != nullptr ? orig_fgets(buf, n, stream) : nullptr;
  }
  pthread_rwlock_wrlock(&g_lock);
  size_t &p = g_fspoof_pos[stream];
  const std::string &c = sf->content;
  if (p >= c.size()) {
    pthread_rwlock_unlock(&g_lock);
    return nullptr;
  }
  size_t i = p;
  while (i < c.size() && c[i] != '\n' && (int)(i - p) < n - 1) ++i;
  size_t len = i - p;
  if (i < c.size() && c[i] == '\n' && (int)(i - p) < n - 1) {
    len = i - p + 1;
    ++i;
  }
  memcpy(buf, c.data() + p, len);
  buf[len] = '\0';
  p = i;
  pthread_rwlock_unlock(&g_lock);
  return buf;
}

static ssize_t streamGetDelim(char **lineptr, size_t *n, int delim,
                              FILE *stream) {
  SpoofFile *sf = spoofOfStream(stream);
  if (sf == nullptr) return -2;
  if (lineptr == nullptr || n == nullptr) return -1;
  std::string c;
  size_t p = 0;
  pthread_rwlock_wrlock(&g_lock);
  c = sf->content;
  p = g_fspoof_pos[stream];
  pthread_rwlock_unlock(&g_lock);
  if (p >= c.size()) return -1;
  size_t i = p;
  while (i < c.size() && c[i] != (char)delim) ++i;
  size_t len = i - p;
  if (i < c.size()) ++i;

  if (*lineptr == nullptr || *n < len + 1) {
    size_t cap = len + 1;
    char *nb = (char *)realloc(*lineptr, cap);
    if (nb == nullptr) return -1;
    *lineptr = nb;
    *n = cap;
  }
  if (len > 0) memcpy(*lineptr, c.data() + p, len);
  (*lineptr)[len] = '\0';
  pthread_rwlock_wrlock(&g_lock);
  g_fspoof_pos[stream] = i;
  pthread_rwlock_unlock(&g_lock);
  return (ssize_t)len;
}

ssize_t fake_getline(char **lineptr, size_t *n, FILE *stream) {
  ssize_t r = streamGetDelim(lineptr, n, '\n', stream);
  if (r == -2) return orig_getline != nullptr ? orig_getline(lineptr, n, stream) : -1;
  return r;
}

ssize_t fake_getdelim(char **lineptr, size_t *n, int delim, FILE *stream) {
  ssize_t r = streamGetDelim(lineptr, n, delim, stream);
  if (r == -2) {
    return orig_getdelim != nullptr ? orig_getdelim(lineptr, n, delim, stream) : -1;
  }
  return r;
}

size_t fake_fread_unlocked(void *ptr, size_t size, size_t nmemb, FILE *stream) {
  SpoofFile *sf = spoofOfStream(stream);
  if (sf == nullptr || ptr == nullptr || size == 0 || nmemb == 0) {
    return orig_fread_unlocked != nullptr
               ? orig_fread_unlocked(ptr, size, nmemb, stream)
               : 0;
  }
  size_t got = serveSpoofStream(stream, sf, (char *)ptr, size * nmemb);
  return got / size;
}

char *fake_fgets_unlocked(char *buf, int n, FILE *stream) {
  SpoofFile *sf = spoofOfStream(stream);
  if (sf == nullptr) {
    return orig_fgets_unlocked != nullptr ? orig_fgets_unlocked(buf, n, stream) : nullptr;
  }
  return fake_fgets(buf, n, stream);
}

ssize_t fake_read(int fd, void *buf, size_t count) {
  SpoofFile *sf = spoofOf(fd);
  if (sf != nullptr) return serveSpoof(fd, sf, buf, count);

  ssize_t n = orig_read != nullptr ? orig_read(fd, buf, count) : -1;
  if (n > 0) {
    off_t pos = lseek(fd, 0, SEEK_CUR);
    if (pos >= 0) syncBuffer(fd, buf, (size_t)n, (int64_t)(pos - n));
  }
  return n;
}

ssize_t fake_pread(int fd, void *buf, size_t count, off_t offset) {
  SpoofFile *sf = spoofOf(fd);
  if (sf != nullptr) {
    if (offset < 0) return -1;
    pthread_rwlock_rdlock(&g_lock);
    const std::string &c = sf->content;
    size_t o = (size_t)offset;
    size_t n = 0;
    if (o < c.size()) {
      n = c.size() - o;
      if (n > count) n = count;
      memcpy(buf, c.data() + o, n);
    }
    pthread_rwlock_unlock(&g_lock);
    return (ssize_t)n;
  }
  ssize_t n = orig_pread != nullptr ? orig_pread(fd, buf, count, offset) : -1;
  if (n > 0) syncBuffer(fd, buf, (size_t)n, (int64_t)offset);
  return n;
}

ssize_t fake_pread64(int fd, void *buf, size_t count, off_t offset) {
  SpoofFile *sf = spoofOf(fd);
  if (sf != nullptr) {
    if (offset < 0) return -1;
    pthread_rwlock_rdlock(&g_lock);
    const std::string &c = sf->content;
    size_t o = (size_t)offset;
    size_t n = 0;
    if (o < c.size()) {
      n = c.size() - o;
      if (n > count) n = count;
      memcpy(buf, c.data() + o, n);
    }
    pthread_rwlock_unlock(&g_lock);
    return (ssize_t)n;
  }
  ssize_t n = orig_pread64 != nullptr ? orig_pread64(fd, buf, count, offset) : -1;
  if (n > 0) syncBuffer(fd, buf, (size_t)n, (int64_t)offset);
  return n;
}

ssize_t fake_readv(int fd, const struct iovec *iov, int iovcnt) {
  ssize_t n = orig_readv != nullptr ? orig_readv(fd, iov, iovcnt) : -1;
  if (n <= 0 || iov == nullptr || iovcnt <= 0) return n;
  off_t pos = lseek(fd, 0, SEEK_CUR);
  if (pos < 0) return n;
  int64_t start = (int64_t)(pos - n);
  int64_t cur = start;
  for (int i = 0; i < iovcnt && cur < start + (int64_t)n; ++i) {
    size_t len = iov[i].iov_len;
    if (len == 0 || iov[i].iov_base == nullptr) continue;
    int64_t left = start + (int64_t)n - cur;
    size_t take = (size_t)(left < (int64_t)len ? left : (int64_t)len);
    syncBuffer(fd, iov[i].iov_base, take, cur);
    cur += (int64_t)take;
  }
  return n;
}

ssize_t fake_read_chk(int fd, void *buf, size_t count, size_t buf_size) {
  SpoofFile *sf = spoofOf(fd);
  if (sf != nullptr) {
    if (count > buf_size) return -1;
    return serveSpoof(fd, sf, buf, count);
  }
  ssize_t n = orig_read_chk != nullptr ? orig_read_chk(fd, buf, count, buf_size) : -1;
  if (n > 0) {
    off_t pos = lseek(fd, 0, SEEK_CUR);
    if (pos >= 0) syncBuffer(fd, buf, (size_t)n, (int64_t)(pos - n));
  }
  return n;
}

ssize_t fake_pread_chk(int fd, void *buf, size_t count, off_t offset, size_t buf_size) {
  SpoofFile *sf = spoofOf(fd);
  if (sf != nullptr) {
    if (count > buf_size) return -1;
    pthread_rwlock_rdlock(&g_lock);
    const std::string &c = sf->content;
    size_t o = offset < 0 ? 0 : (size_t)offset;
    size_t n = 0;
    if (o < c.size()) {
      n = c.size() - o;
      if (n > count) n = count;
      memcpy(buf, c.data() + o, n);
    }
    pthread_rwlock_unlock(&g_lock);
    return (ssize_t)n;
  }
  ssize_t n = orig_pread_chk != nullptr
                  ? orig_pread_chk(fd, buf, count, offset, buf_size)
                  : -1;
  if (n > 0) syncBuffer(fd, buf, (size_t)n, (int64_t)offset);
  return n;
}

ssize_t fake_pread64_chk(int fd, void *buf, size_t count, off_t offset, size_t buf_size) {
  SpoofFile *sf = spoofOf(fd);
  if (sf != nullptr) {
    if (count > buf_size) return -1;
    pthread_rwlock_rdlock(&g_lock);
    const std::string &c = sf->content;
    size_t o = offset < 0 ? 0 : (size_t)offset;
    size_t n = 0;
    if (o < c.size()) {
      n = c.size() - o;
      if (n > count) n = count;
      memcpy(buf, c.data() + o, n);
    }
    pthread_rwlock_unlock(&g_lock);
    return (ssize_t)n;
  }
  ssize_t n = orig_pread64_chk != nullptr
                  ? orig_pread64_chk(fd, buf, count, offset, buf_size)
                  : -1;
  if (n > 0) syncBuffer(fd, buf, (size_t)n, (int64_t)offset);
  return n;
}

long fake_syscall(long nr, ...) {
  va_list ap;
  va_start(ap, nr);
  long a0 = va_arg(ap, long);
  long a1 = va_arg(ap, long);
  long a2 = va_arg(ap, long);
  long a3 = va_arg(ap, long);
  long a4 = va_arg(ap, long);
  long a5 = va_arg(ap, long);
  va_end(ap);

  long r = orig_syscall != nullptr ? orig_syscall(nr, a0, a1, a2, a3, a4, a5) : -1;
  if (!g_enabled || t_bypass || r < 0) return r;

  if (nr == __NR_openat) {
    const char *p = (const char *)a1;
    std::string abs = resolvePath((int)a0, p);
    SpoofFile *f = findSpoof(p);
    trackFd((int)r, abs, f);
    return r;
  }
#if defined(__NR_open)
  if (nr == __NR_open) {
    const char *p = (const char *)a0;
    std::string abs = (p != nullptr) ? std::string(p) : std::string();
    SpoofFile *f = findSpoof(p);
    trackFd((int)r, abs, f);
    return r;
  }
#endif
  if (!g_anti_detect) return r;

  if (nr == __NR_read) {
    off_t pos = lseek((int)a0, 0, SEEK_CUR);
    if (pos >= 0) syncBuffer((int)a0, (void *)a1, (size_t)r, (int64_t)(pos - r));
  } else if (nr == __NR_pread64) {
    syncBuffer((int)a0, (void *)a1, (size_t)r, (int64_t)a3);
  }
#if defined(__NR_pread)
  else if (nr == __NR_pread) {
    syncBuffer((int)a0, (void *)a1, (size_t)r, (int64_t)a3);
  }
#endif
  return r;
}

void *fake_mmap(void *addr, size_t len, int prot, int flags, int fd, off_t off) {
  void *r = orig_mmap != nullptr ? orig_mmap(addr, len, prot, flags, fd, off) : MAP_FAILED;
  if (r != MAP_FAILED && fd >= 0 && (prot & PROT_READ) != 0) {
    syncMapped(r, len, fd, (int64_t)off, (flags & MAP_SHARED) != 0);
  }
  return r;
}

int fake_close(int fd) {
  if (g_enabled) {
    pthread_rwlock_wrlock(&g_lock);
    g_fdpath.erase(fd);
    g_fdspoof.erase(fd);
    g_fdspoof_pos.erase(fd);
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

static char g_gpu_glver_buf[96] = {0};
static char g_gpu_glsl_buf[96] = {0};
static uint32_t g_gpu_vk_api = 0;
static uint32_t g_gpu_driver = 0;
static uint32_t g_gpu_vendor_id = 0;
static uint32_t g_gpu_device_id = 0;
static uint32_t g_gpu_max_dim = 0;
static uint32_t g_gpu_max_cube = 0;
static uint32_t g_gpu_layers = 0;
static uint32_t g_gpu_push = 0;
static int64_t g_gpu_memory_bytes = 0;

void refreshGpuBuf() {
  pthread_rwlock_rdlock(&g_lock);
  std::string gpu = g_gpu;
  std::string vendor = g_gpu_vendor;
  std::string glver = g_gpu_gl_version;
  std::string glsl = g_gpu_glsl;
  pthread_rwlock_unlock(&g_lock);

  g_gpu_vk_api = g_gpu_vk_api_cfg;
  g_gpu_driver = g_gpu_driver_cfg;
  g_gpu_vendor_id = g_gpu_vendor_id_cfg;
  g_gpu_device_id = g_gpu_device_id_cfg;
  g_gpu_max_dim = g_gpu_max_dim_cfg;
  g_gpu_max_cube = g_gpu_max_cube_cfg;
  g_gpu_layers = g_gpu_layers_cfg;
  g_gpu_push = g_gpu_push_cfg;
  g_gpu_memory_bytes = (int64_t)g_gpu_memory_mb_cfg * 1024LL * 1024LL;

  if (gpu.empty()) {
    g_gpu_buf_ready = false;
    g_gpu_buf[0] = '\0';
  } else {
    size_t n = gpu.size();
    if (n >= sizeof(g_gpu_buf)) n = sizeof(g_gpu_buf) - 1;
    memcpy(g_gpu_buf, gpu.data(), n);
    g_gpu_buf[n] = '\0';
    g_gpu_buf_ready = true;
  }

  std::string v = vendor;
  if (v.empty()) {
    const char *d = g_gpu_buf_ready ? g_gpu_buf : gpu.c_str();
    v = strstr(d, "Adreno") != nullptr ? "Qualcomm"
        : strstr(d, "Mali") != nullptr ? "ARM"
        : strstr(d, "Immortalis") != nullptr ? "ARM"
        : strstr(d, "Maleoon") != nullptr ? "HiSilicon"
        : strstr(d, "PowerVR") != nullptr ? "Imagination Technologies"
        : "ARM";
  }
  strncpy(g_gpu_vendor_buf, v.c_str(), sizeof(g_gpu_vendor_buf) - 1);
  g_gpu_vendor_buf[sizeof(g_gpu_vendor_buf) - 1] = '\0';

  std::string gv = glver.empty() ? std::string("OpenGL ES 3.2 V@0502.0") : glver;
  strncpy(g_gpu_glver_buf, gv.c_str(), sizeof(g_gpu_glver_buf) - 1);
  g_gpu_glver_buf[sizeof(g_gpu_glver_buf) - 1] = '\0';

  std::string gs = glsl.empty() ? std::string("OpenGL ES GLSL ES 3.20") : glsl;
  strncpy(g_gpu_glsl_buf, gs.c_str(), sizeof(g_gpu_glsl_buf) - 1);
  g_gpu_glsl_buf[sizeof(g_gpu_glsl_buf) - 1] = '\0';
}

const unsigned char *fake_gl_get_string(unsigned int name) {
  if (g_enabled && g_gpu_buf_ready) {
    if (name == 0x1F01) return (const unsigned char *)g_gpu_buf;
    if (name == 0x1F00) return (const unsigned char *)g_gpu_vendor_buf;
  }
  if (g_enabled) {
    if (name == 0x1F02 && g_gpu_glver_buf[0] != '\0') {
      return (const unsigned char *)g_gpu_glver_buf;
    }
    if (name == 0x8B8C && g_gpu_glsl_buf[0] != '\0') {
      return (const unsigned char *)g_gpu_glsl_buf;
    }
  }
  return orig_gl_get_string != nullptr ? orig_gl_get_string(name) : nullptr;
}

const char *fake_egl_query_string(void *dpy, int name) {
  if (g_enabled && g_gpu_buf_ready && name == 0x305D) {
    return g_gpu_buf;
  }
  if (g_enabled && name == 0x3053 && g_gpu_vendor_buf[0] != '\0') {
    return g_gpu_vendor_buf;
  }
  return orig_egl_query_string != nullptr ? orig_egl_query_string(dpy, name) : nullptr;
}

typedef void (*gl_get_integerv_t)(unsigned int, int *);
gl_get_integerv_t orig_gl_get_integerv = nullptr;

void fake_gl_get_integerv(unsigned int pname, int *data) {
  if (orig_gl_get_integerv != nullptr) orig_gl_get_integerv(pname, data);
  if (!g_enabled || data == nullptr) return;
  uint32_t v = 0;
  switch (pname) {
    case 0x0D33:
    case 0x84E8:
      v = g_gpu_max_dim;
      break;
    case 0x851C:
      v = g_gpu_max_cube;
      break;
    case 0x88FF:
      v = g_gpu_layers;
      break;
    default:
      return;
  }
  if (v != 0) *data = (int)v;
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

typedef void (*vk_get_props_t)(void *, void *);

vk_get_props_t orig_vk_get_props = nullptr;
vk_get_props_t orig_vk_get_props2 = nullptr;

static const size_t VK_NAME_OFF = 20;
static const size_t VK_LIMITS_OFF = 292;

void patchVkProps(void *props, size_t base) {
  if (props == nullptr || !g_enabled) return;
  uint8_t *p = (uint8_t *)props + base;

  if (g_gpu_vk_api != 0) *((uint32_t *)(p + 0)) = g_gpu_vk_api;
  if (g_gpu_driver != 0) *((uint32_t *)(p + 4)) = g_gpu_driver;
  if (g_gpu_vendor_id != 0) *((uint32_t *)(p + 8)) = g_gpu_vendor_id;
  if (g_gpu_device_id != 0) *((uint32_t *)(p + 12)) = g_gpu_device_id;

  if (g_gpu_buf_ready) {
    char *name = (char *)(p + VK_NAME_OFF);
    strncpy(name, g_gpu_buf, 255);
    name[255] = '\0';
  }

  uint8_t *lim = p + VK_LIMITS_OFF;
  if (g_gpu_max_dim != 0) {
    *((uint32_t *)(lim + 0)) = g_gpu_max_dim;
    *((uint32_t *)(lim + 4)) = g_gpu_max_dim;
    *((uint32_t *)(lim + 8)) = g_gpu_max_dim;
  }
  if (g_gpu_max_cube != 0) *((uint32_t *)(lim + 12)) = g_gpu_max_cube;
  if (g_gpu_layers != 0) *((uint32_t *)(lim + 16)) = g_gpu_layers;
  if (g_gpu_push != 0) *((uint32_t *)(lim + 32)) = g_gpu_push;
}

void fake_vk_get_props(void *phys, void *props) {
  if (orig_vk_get_props != nullptr) orig_vk_get_props(phys, props);
  patchVkProps(props, 0);
}

void fake_vk_get_props2(void *phys, void *props) {
  if (orig_vk_get_props2 != nullptr) orig_vk_get_props2(phys, props);
  patchVkProps(props, 16);
}

typedef void (*vk_get_mem_props_t)(void *, void *);
vk_get_mem_props_t orig_vk_get_mem_props = nullptr;

void fake_vk_get_mem_props(void *phys, void *props) {
  if (orig_vk_get_mem_props != nullptr) orig_vk_get_mem_props(phys, props);
  if (props == nullptr || !g_enabled || g_gpu_memory_bytes <= 0) return;
  uint8_t *p = (uint8_t *)props;
  uint32_t heapCount = *((uint32_t *)(p + 260));
  if (heapCount > 16) heapCount = 16;
  for (uint32_t i = 0; i < heapCount; ++i) {
    *((uint64_t *)(p + 264 + (size_t)i * 16)) = (uint64_t)g_gpu_memory_bytes;
  }
}

typedef void *(*vk_get_proc_addr_t)(void *, const char *);

vk_get_proc_addr_t orig_vk_gipa = nullptr;
vk_get_proc_addr_t orig_vk_gdpa = nullptr;

const char *vkNameOf(const char *name) {
  if (name == nullptr) return nullptr;
  if (strcmp(name, "vkGetPhysicalDeviceProperties") == 0) return "props";
  if (strcmp(name, "vkGetPhysicalDeviceProperties2") == 0) return "props2";
  if (strcmp(name, "vkGetPhysicalDeviceProperties2KHR") == 0) return "props2";
  return nullptr;
}

void *fake_vk_gipa(void *instance, const char *name) {
  void *p = orig_vk_gipa != nullptr ? orig_vk_gipa(instance, name) : nullptr;
  if (!g_enabled || !g_gpu_buf_ready || p == nullptr) return p;
  const char *which = vkNameOf(name);
  if (which == nullptr) return p;
  if (strcmp(which, "props") == 0 && orig_vk_get_props != nullptr) {
    return (void *)fake_vk_get_props;
  }
  if (strcmp(which, "props2") == 0 && orig_vk_get_props2 != nullptr) {
    return (void *)fake_vk_get_props2;
  }
  return p;
}

void *fake_vk_gdpa(void *device, const char *name) {
  void *p = orig_vk_gdpa != nullptr ? orig_vk_gdpa(device, name) : nullptr;
  if (!g_enabled || !g_gpu_buf_ready || p == nullptr) return p;
  const char *which = vkNameOf(name);
  if (which == nullptr) return p;
  if (strcmp(which, "props") == 0 && orig_vk_get_props != nullptr) {
    return (void *)fake_vk_get_props;
  }
  if (strcmp(which, "props2") == 0 && orig_vk_get_props2 != nullptr) {
    return (void *)fake_vk_get_props2;
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
  if (orig_vk_get_props == nullptr) {
    void *v = resolveGfx("libvulkan.so", "vkGetPhysicalDeviceProperties", true);
    if (v != nullptr) {
      DobbyHook(v, (dobby_dummy_func_t)fake_vk_get_props,
                (dobby_dummy_func_t *)&orig_vk_get_props);
    }
  }
  if (orig_vk_get_props2 == nullptr) {
    void *v = resolveGfx("libvulkan.so", "vkGetPhysicalDeviceProperties2", true);
    if (v == nullptr) {
      v = resolveGfx("libvulkan.so", "vkGetPhysicalDeviceProperties2KHR", true);
    }
    if (v != nullptr) {
      DobbyHook(v, (dobby_dummy_func_t)fake_vk_get_props2,
                (dobby_dummy_func_t *)&orig_vk_get_props2);
    }
  }
  if (orig_vk_get_mem_props == nullptr) {
    void *v = resolveGfx("libvulkan.so", "vkGetPhysicalDeviceMemoryProperties", true);
    if (v != nullptr) {
      DobbyHook(v, (dobby_dummy_func_t)fake_vk_get_mem_props,
                (dobby_dummy_func_t *)&orig_vk_get_mem_props);
    }
  }
  if (orig_gl_get_integerv == nullptr) {
    void *g = resolveGfx("libGLESv2.so", "glGetIntegerv", true);
    if (g == nullptr) g = resolveGfx("libGLESv1_CM.so", "glGetIntegerv", true);
    if (g != nullptr) {
      DobbyHook(g, (dobby_dummy_func_t)fake_gl_get_integerv,
                (dobby_dummy_func_t *)&orig_gl_get_integerv);
    }
  }
  if (orig_vk_gipa == nullptr) {
    void *v = resolveGfx("libvulkan.so", "vkGetInstanceProcAddr", true);
    if (v != nullptr) {
      DobbyHook(v, (dobby_dummy_func_t)fake_vk_gipa,
                (dobby_dummy_func_t *)&orig_vk_gipa);
    }
  }
  if (orig_vk_gdpa == nullptr) {
    void *v = resolveGfx("libvulkan.so", "vkGetDeviceProcAddr", true);
    if (v != nullptr) {
      DobbyHook(v, (dobby_dummy_func_t)fake_vk_gdpa,
                (dobby_dummy_func_t *)&orig_vk_gdpa);
    }
  }
  if (!g_gpu_buf_ready) refreshGpuBuf();
  return orig_gl_get_string != nullptr || orig_egl_query_string != nullptr ||
         orig_vk_get_props != nullptr || orig_vk_get_props2 != nullptr;
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

  const uint32_t G = g_groups;
  if (G == 0) {
    LOGI("native hooks skipped: no native feature enabled");
    return 0;
  }
  g_installed = true;

  int n = 0, total = 0;
#define TRY(sym, fake, orig)                                                   \
  do {                                                                         \
    ++total;                                                                   \
    if (hookSym(sym, (void *)(fake), (void **)&(orig))) ++n;                    \
  } while (0)

  if (G & GRP_PROP) {
    TRY("__system_property_get", fake_prop_get, orig_prop_get);
    TRY("__system_property_find", fake_prop_find, orig_prop_find);
    TRY("__system_property_read_callback", fake_prop_read_callback,
        orig_prop_read_callback);
    TRY("__system_property_read", fake_prop_read, orig_prop_read);
  }
  if (G & GRP_UNAME) TRY("uname", fake_uname, orig_uname);
  if (G & GRP_TIME) {
    TRY("gettimeofday", fake_gettimeofday, orig_gettimeofday);
    TRY("clock_gettime", fake_clock_gettime, orig_clock_gettime);
    TRY("sysinfo", fake_sysinfo, orig_sysinfo);
    TRY("time", fake_time, orig_time);
    TRY("clock", fake_clock, orig_clock);
  }
  if ((G & GRP_FILE) || (G & GRP_SENSOR)) {
    TRY("open", fake_open, orig_open);
    TRY("open64", fake_open, orig_open64);
    TRY("openat", fake_openat, orig_openat);
    TRY("openat64", fake_openat, orig_openat64);
    TRY("fopen", fake_fopen, orig_fopen);
    TRY("close", fake_close, orig_close);
  }
  if (G & GRP_STAT) {
    TRY("access", fake_access, orig_access);
    TRY("faccessat", fake_faccessat, orig_faccessat);
    TRY("stat", fake_stat, orig_stat);
    TRY("lstat", fake_lstat, orig_lstat);
    TRY("fstatat", fake_fstatat, orig_fstatat);
    TRY("readlink", fake_readlink, orig_readlink);
    TRY("realpath", fake_realpath, orig_realpath);
  }
  if (G & GRP_EXEC) {
    TRY("execve", fake_execve, orig_execve);
    TRY("execv", fake_execv, orig_execv);
    TRY("execvp", fake_execvp, orig_execvp);
    TRY("system", fake_system, orig_system);
    TRY("popen", fake_popen, orig_popen);
    TRY("posix_spawn", fake_posix_spawn, orig_posix_spawn);
  }
  if (G & GRP_NET) {
    TRY("getifaddrs", fake_getifaddrs, orig_getifaddrs);
    TRY("if_nametoindex", fake_if_nametoindex, orig_if_nametoindex);
  }
  if (G & GRP_EXIT) {
    TRY("exit", fake_exit, orig_exit);
    TRY("_exit", fake__exit, orig__exit);
    TRY("abort", fake_abort, orig_abort);
    TRY("kill", fake_kill, orig_kill);
  }
  if (G & GRP_FILE) {
    TRY("fread", fake_fread, orig_fread);
    TRY("fgets", fake_fgets, orig_fgets);
    TRY("getline", fake_getline, orig_getline);
    TRY("getdelim", fake_getdelim, orig_getdelim);
    TRY("fread_unlocked", fake_fread_unlocked, orig_fread_unlocked);
    TRY("fgets_unlocked", fake_fgets_unlocked, orig_fgets_unlocked);
    TRY("statvfs", fake_statvfs, orig_statvfs);
    TRY("statvfs64", fake_statvfs64, orig_statvfs64);
    TRY("statfs", fake_statfs, orig_statfs);
    TRY("statfs64", fake_statfs64, orig_statfs64);
  }
  if (G & GRP_READ) {
    TRY("read", fake_read, orig_read);
    TRY("pread", fake_pread, orig_pread);
    TRY("pread64", fake_pread64, orig_pread64);
    TRY("readv", fake_readv, orig_readv);
    TRY("syscall", fake_syscall, orig_syscall);
    TRY("__read_chk", fake_read_chk, orig_read_chk);
    TRY("__pread_chk", fake_pread_chk, orig_pread_chk);
    TRY("__pread64_chk", fake_pread64_chk, orig_pread64_chk);
  }
  if (G & GRP_MMAP) TRY("mmap", fake_mmap, orig_mmap);
  if (G & GRP_SENSOR) {
    TRY("fopen", fake_fopen, orig_fopen);
  }
#undef TRY

  g_sensor_block = (G & GRP_SENSOR) != 0;

  if (orig_open64 == nullptr) orig_open64 = orig_open;
  if (orig_openat64 == nullptr) orig_openat64 = orig_openat;
  if (orig_pread64 == nullptr) orig_pread64 = orig_pread;
  if (orig_pread64_chk == nullptr) orig_pread64_chk = orig_pread_chk;

  bool gpu = false;
  if (g_enabled && (G & GRP_GPU)) gpu = hookGpu();

  if (g_enabled && g_anti_detect) collectPatches();

  LOGI("native hooks installed: %d/%d, groups=0x%x, gpu=%d, anti=%d", n, total,
       G, gpu ? 1 : 0, g_anti_detect ? 1 : 0);
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
  g_exist.clear();
  clearFiles();
  g_kernel.clear();
  g_arch.clear();
  g_gpu.clear();
  g_gpu_vendor.clear();
  g_gpu_gl_version.clear();
  g_gpu_glsl.clear();
  g_gpu_vk_api_cfg = 0;
  g_gpu_driver_cfg = 0;
  g_gpu_vendor_id_cfg = 0;
  g_gpu_device_id_cfg = 0;
  g_gpu_max_dim_cfg = 0;
  g_gpu_max_cube_cfg = 0;
  g_gpu_layers_cfg = 0;
  g_gpu_push_cfg = 0;
  g_gpu_memory_mb_cfg = 0;
  g_time_offset_ms = 0;
  g_time_enable = false;
  g_stor_total = 0;
  g_stor_avail = 0;
  g_stor_enable = false;
  g_uptime_enable = false;
  g_block_exec = false;
  g_block_exit = false;
  g_crash_catch = false;
  g_anti_detect = false;
  g_vpn.clear();
  g_vpn_enable = false;
  g_groups = 0;
  g_sensor_block = false;

  std::vector<std::string> lines = split(payload != nullptr ? payload : "", '\n');
  for (size_t i = 0; i < lines.size(); ++i) {
    const std::string &line = lines[i];
    if (line.size() < 2) continue;
    std::string body = line.substr(2);

    if (line.compare(0, 2, "M\t") == 0) {
      g_groups = (uint32_t)strtoul(body.c_str(), nullptr, 10);
    } else if (line.compare(0, 2, "K\t") == 0) {
      g_kernel = body;
    } else if (line.compare(0, 2, "A\t") == 0) {
      g_arch = body;
    } else if (line.compare(0, 2, "g\t") == 0) {
      std::string rest = line.substr(2);
      size_t tab = rest.find('\t');
      if (tab == std::string::npos) continue;
      std::string field = rest.substr(0, tab);
      std::string val = rest.substr(tab + 1);
      if (field == "vendor") g_gpu_vendor = val;
      else if (field == "glversion") g_gpu_gl_version = val;
      else if (field == "glsl") g_gpu_glsl = val;
      else if (field == "vkapi") g_gpu_vk_api_cfg = (uint32_t)strtoul(val.c_str(), nullptr, 10);
      else if (field == "driver") g_gpu_driver_cfg = (uint32_t)strtoul(val.c_str(), nullptr, 10);
      else if (field == "vendorid") g_gpu_vendor_id_cfg = (uint32_t)strtoul(val.c_str(), nullptr, 10);
      else if (field == "deviceid") g_gpu_device_id_cfg = (uint32_t)strtoul(val.c_str(), nullptr, 10);
      else if (field == "memory") g_gpu_memory_mb_cfg = (int)strtol(val.c_str(), nullptr, 10);
      else if (field == "maxdim") g_gpu_max_dim_cfg = (uint32_t)strtoul(val.c_str(), nullptr, 10);
      else if (field == "maxcube") g_gpu_max_cube_cfg = (uint32_t)strtoul(val.c_str(), nullptr, 10);
      else if (field == "layers") g_gpu_layers_cfg = (uint32_t)strtoul(val.c_str(), nullptr, 10);
      else if (field == "push") g_gpu_push_cfg = (uint32_t)strtoul(val.c_str(), nullptr, 10);
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
    } else if (line.compare(0, 2, "V\t") == 0) {
      std::vector<std::string> kv = split(body, '\t');
      if (kv.size() >= 2) {
        g_stor_total = strtoull(kv[0].c_str(), nullptr, 10);
        g_stor_avail = strtoull(kv[1].c_str(), nullptr, 10);
        g_stor_enable = (g_stor_total > 0);
      }
    } else if (line.compare(0, 2, "P\t") == 0) {
      std::vector<std::string> kv = split(body, '\t');
      if (!kv.empty() && !kv[0].empty()) {
        g_props[kv[0]] = kv.size() >= 2 ? unescape(kv[1]) : std::string();
      }
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
  g_need_fdtrack = g_anti_detect || !g_files.empty();

  LOGI("config applied: groups=0x%x props=%zu exist=%zu files=%zu gpu=%d vpn=%zu",
       g_groups, g_props.size(), g_exist.size(), g_files.size(),
       g_gpu.empty() ? 0 : 1, g_vpn.size());

  pthread_rwlock_unlock(&g_lock);
}

void clearAll() {
  pthread_rwlock_wrlock(&g_lock);
  g_props.clear();
  g_exist.clear();
  clearFiles();
  g_kernel.clear();
  g_arch.clear();
  g_gpu.clear();
  g_vpn.clear();
  g_vpn_enable = false;
  g_anti_detect = false;
  g_groups = 0;
  g_sensor_block = false;
  g_need_fdtrack = false;
  g_path_patches.clear();
  for (size_t i = 0; i < g_patch_pool.size(); ++i) g_patch_pool[i]->clear();
  g_basename_index.clear();
  pthread_mutex_lock(&g_prop_name_lock);
  g_prop_name.clear();
  pthread_mutex_unlock(&g_prop_name_lock);
  g_fdpath.clear();
  g_fdspoof.clear();
  g_fdspoof_pos.clear();
  g_fspoof.clear();
  g_fspoof_pos.clear();
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

namespace {

void probeAppend(std::string &out, const char *key, const char *val) {
  if (val == nullptr) return;
  out += key;
  out += '\t';
  for (const char *p = val; *p; ++p) {
    if (*p == '\n' || *p == '\t' || *p == '\r') out += ' ';
    else out += *p;
  }
  out += '\n';
}

void probeProp(std::string &out, const char *key, const char *prop) {
  char buf[PROP_VALUE_MAX + 1];
  buf[0] = '\0';
  if (__system_property_get(prop, buf) > 0) probeAppend(out, key, buf);
}

std::string probeLine(const std::string &text, const char *prefix) {
  size_t pos = 0;
  while (pos < text.size()) {
    size_t end = text.find('\n', pos);
    if (end == std::string::npos) end = text.size();
    std::string line = text.substr(pos, end - pos);
    if (line.compare(0, strlen(prefix), prefix) == 0) {
      size_t i = line.find(':');
      if (i != std::string::npos) {
        std::string v = line.substr(i + 1);
        size_t a = v.find_first_not_of(" \t");
        size_t b = v.find_last_not_of(" \t");
        if (a == std::string::npos) return std::string();
        return v.substr(a, b - a + 1);
      }
    }
    pos = end + 1;
  }
  return std::string();
}

static void trimEol(std::string &s) {
  while (!s.empty() && (s.back() == '\n' || s.back() == '\r' || s.back() == ' ')) {
    s.pop_back();
  }
  size_t a = s.find_first_not_of(" \t");
  if (a == std::string::npos) { s.clear(); return; }
  s = s.substr(a);
}

int probeCount(const std::string &text, const char *prefix) {
  int n = 0;
  size_t pos = 0;
  while (pos < text.size()) {
    size_t end = text.find('\n', pos);
    if (end == std::string::npos) end = text.size();
    std::string line = text.substr(pos, end - pos);
    if (line.compare(0, strlen(prefix), prefix) == 0) ++n;
    pos = end + 1;
  }
  return n;
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_sunilxsk_lockperm_NativeBridge_nativeProbe(JNIEnv *env, jclass) {
  std::string out;

  struct utsname u;
  memset(&u, 0, sizeof(u));
  if (uname(&u) == 0) {
    probeAppend(out, "uname.sysname", u.sysname);
    probeAppend(out, "uname.nodename", u.nodename);
    probeAppend(out, "uname.release", u.release);
    probeAppend(out, "uname.version", u.version);
    probeAppend(out, "uname.machine", u.machine);
  }

  std::string cpu;
  if (readWholeFile("/proc/cpuinfo", cpu) && !cpu.empty()) {
    std::string hw = probeLine(cpu, "Hardware");
    if (!hw.empty()) probeAppend(out, "cpuinfo.hardware", hw.c_str());
    char nbuf[32];
    snprintf(nbuf, sizeof(nbuf), "%d", probeCount(cpu, "processor"));
    probeAppend(out, "cpuinfo.processors", nbuf);
    std::string model = probeLine(cpu, "model name");
    if (!model.empty()) probeAppend(out, "cpuinfo.model", model.c_str());
  }

  std::string ver;
  if (readWholeFile("/proc/version", ver) && !ver.empty()) {
    probeAppend(out, "proc.version", ver.c_str());
  }

  std::string mem;
  if (readWholeFile("/proc/meminfo", mem) && !mem.empty()) {
    std::string total = probeLine(mem, "MemTotal");
    if (!total.empty()) {
      size_t sp = total.find(' ');
      if (sp != std::string::npos) total = total.substr(0, sp);
      probeAppend(out, "meminfo.total_kb", total.c_str());
    }
  }

  std::string soc;
  if (readWholeFile("/sys/devices/soc0/machine", soc) && !soc.empty()) {
    std::string v = soc;
    while (!v.empty() && (v.back() == '\n' || v.back() == '\r')) v.pop_back();
    probeAppend(out, "soc0.machine", v.c_str());
  }
  soc.clear();
  if (readWholeFile("/sys/devices/soc0/family", soc) && !soc.empty()) {
    while (!soc.empty() && (soc.back() == '\n' || soc.back() == '\r')) soc.pop_back();
    probeAppend(out, "soc0.family", soc.c_str());
  }
  soc.clear();
  if (readWholeFile("/sys/devices/soc0/hardware", soc) && !soc.empty()) {
    while (!soc.empty() && (soc.back() == '\n' || soc.back() == '\r')) soc.pop_back();
    probeAppend(out, "soc0.hardware", soc.c_str());
  }

  probeProp(out, "prop.abi", "ro.product.cpu.abi");
  probeProp(out, "prop.abilist", "ro.product.cpu.abilist");
  probeProp(out, "prop.hardware", "ro.hardware");
  probeProp(out, "prop.board_platform", "ro.board.platform");
  probeProp(out, "prop.soc_model", "ro.soc.model");
  probeProp(out, "prop.soc_manufacturer", "ro.soc.manufacturer");
  probeProp(out, "prop.radio", "ro.build.radio");
  probeProp(out, "prop.baseband", "gsm.version.baseband");
  probeProp(out, "prop.kernel_version", "ro.kernel.version");
  probeProp(out, "prop.bootloader", "ro.bootloader");
  probeProp(out, "prop.serialno", "ro.serialno");
  probeProp(out, "prop.build_product", "ro.build.product");

  static const char *kExtraProps[] = {
      "ro.build.id",
      "ro.build.display.id",
      "ro.build.version.incremental",
      "ro.build.version.release",
      "ro.build.version.sdk",
      "ro.build.version.security_patch",
      "ro.build.version.release_or_codename",
      "ro.build.type",
      "ro.build.tags",
      "ro.build.user",
      "ro.build.host",
      "ro.build.fingerprint",
      "ro.build.characteristics",
      "ro.build.date.utc",
      "ro.product.manufacturer",
      "ro.product.brand",
      "ro.product.model",
      "ro.product.device",
      "ro.product.name",
      "ro.product.board",
      "ro.product.locale",
      "ro.product.first_api_level",
      "ro.system.build.fingerprint",
      "ro.vendor.build.fingerprint",
      "ro.odm.build.fingerprint",
      "ro.product.build.fingerprint",
      "ro.system_ext.build.fingerprint",
      "ro.bootimage.build.fingerprint",
      "ro.boot.hardware",
      "ro.boot.bootloader",
      "ro.boot.serialno",
      "ro.debuggable",
      "ro.secure",
      "ro.build.selinux",
      "ro.adb.secure",
      "ro.allow.mock.location",
      "ro.opengles.version",
      "ro.hardware.egl",
      "ro.hardware.vulkan",
      "ro.sf.lcd_density",
      "ro.config.ringtone",
      "ro.config.alarm_alert",
      "ro.config.notification_sound",
      "dalvik.vm.heapsize",
      "dalvik.vm.heapstartsize",
      "dalvik.vm.heapgrowthlimit",
      "persist.sys.timezone",
      "persist.sys.locale",
      "persist.sys.language",
      "persist.sys.country",
      "persist.sys.usb.config",
      "sys.usb.config",
      "net.bt.name",
      "gsm.current.phone-type",
      "ril.serialnumber",
  };
  for (size_t i = 0; i < sizeof(kExtraProps) / sizeof(kExtraProps[0]); ++i) {
    probeProp(out, (std::string("prop.") + kExtraProps[i]).c_str(), kExtraProps[i]);
  }

  {
    std::string maxs, mins, curs;
    for (int i = 0; i < 16; ++i) {
      char path[160];
      std::string v;
      snprintf(path, sizeof(path),
               "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i);
      if (!readWholeFile(path, v) || v.empty()) break;
      trimEol(v);
      if (!maxs.empty()) maxs += ",";
      maxs += v;

      snprintf(path, sizeof(path),
               "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_min_freq", i);
      v.clear();
      if (readWholeFile(path, v) && !v.empty()) {
        trimEol(v);
        if (!mins.empty()) mins += ",";
        mins += v;
      }
      snprintf(path, sizeof(path),
               "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_cur_freq", i);
      v.clear();
      if (readWholeFile(path, v) && !v.empty()) {
        trimEol(v);
        if (!curs.empty()) curs += ",";
        curs += v;
      }
    }
    if (!maxs.empty()) probeAppend(out, "cpufreq.max_list", maxs.c_str());
    if (!mins.empty()) probeAppend(out, "cpufreq.min_list", mins.c_str());
    if (!curs.empty()) probeAppend(out, "cpufreq.cur_list", curs.c_str());
  }

  probeAppend(out, "native.abi",
#if defined(__aarch64__)
              "arm64-v8a"
#elif defined(__arm__)
              "armeabi-v7a"
#elif defined(__i386__)
              "x86"
#elif defined(__x86_64__)
              "x86_64"
#else
              ""
#endif
  );

  return env->NewStringUTF(out.c_str());
}

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
  if (orig_time) n++;
  if (orig_clock) n++;
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
  if (orig_read_chk) n++;
  if (orig_pread_chk || orig_pread64_chk) n++;
  if (orig_syscall) n++;
  if (orig_mmap) n++;
  if (orig_close) n++;
  if (orig_getifaddrs) n++;
  if (orig_if_nametoindex) n++;
  if (orig_gl_get_string) n++;
  if (orig_egl_query_string) n++;
  if (orig_vk_get_props) n++;
  if (orig_vk_get_props2) n++;
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
  if (g_enabled && (g_groups & GRP_GPU) && !orig_gl_get_string &&
      name != nullptr && strstr(name, "GLES") != nullptr) {
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