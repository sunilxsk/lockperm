[中文](#zh) | [English](#en)

---

<a id="zh"></a>
# LockPerm

<p align="center">
<img src="https://raw.githubusercontent.com/sunilxsk/lockperm/refs/heads/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="LockPerm" width="128" height="128">
</p>

<h1 align="center">LockPerm</h1>

<p align="center">
<img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform">
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose">
<img src="https://img.shields.io/badge/License-AGPL--3.0-orange" alt="License">
</p>
<p align="center">
<img src="https://img.shields.io/badge/Version-5.4.0-blue" alt="Version">
<img src="https://img.shields.io/badge/compileSdk-37-brightgreen" alt="compileSdk">
<img src="https://img.shields.io/badge/minSdk-28-brightgreen" alt="minSdk">
<img src="https://img.shields.io/badge/JDK-17%2B-red" alt="JDK">
</p>

<p align="center">
<b>伪装 · 防护</b><br>
一个基于 Libxposed API 102 的 Xposed 模块，把「伪装」「防护」「控制」三件事放在一起。
</p>

---

LockPerm 是一个以**应用为单位**工作的隐私模块：它让目标应用读到你想让它读到的内容（伪装），并阻止它做你不允许它做的事（防护），还能在应用运行时挂一个悬浮面板直接动手改界面（控制）。

在「应用」页把目标应用加入作用域，点「配置」进入它的专属配置页，在 **防护功能 / 权限伪装 / 设备伪装 / 设备模板 / 其他功能** 五个页签里勾选需要的功能即可。每个应用的配置互相独立，互不干扰。

---

## 🚀 快速上手

1. 在 LSPosed 管理器中启用本模块（也支持 LSPatch）。
2. 在「应用」页为目标应用申请作用域。
3. 在「应用」页给已加入作用域的应用点「配置」，进入它的配置页。
4. 在五个页签里勾选这个应用需要的功能，页签可左右滑动。
5. **强制停止目标应用再打开**，配置才会生效。

---

## 🛡 防护功能

### 权限与设备控制权
- **无障碍防护**：禁止开启无障碍服务，也可单独禁用读屏、监听通知、监听窗口变化、读取输入内容、模拟操作、无障碍悬浮窗、按键 / 手势监听；清空无障碍服务列表、伪装无障碍开启状态。
- **设备管理员 / Device Owner 防护**：阻止主动申请成为设备管理员，并按需禁用锁屏、改密码、恢复出厂、停用相机、应用管理、系统设置、权限授予、加密等能力。
- **悬浮窗**：可拦掉系统级悬浮窗；也可「允许显示并改造」—— 穿透点击、完全透明、限制占屏大小、自定义窗口标志、给所有窗口加 / 去 FLAG_SECURE。
- **壁纸拦截**：无论应用用哪种方式改壁纸都拦下。
- **安装 / 卸载拦截**：阻止静默安装或引导安装，可伪装成「操作成功」直接回执。
- **打印 / 投屏拦截**：阻止应用发起打印或投屏。
- **命令执行拦截**：拦截应用执行命令（Java/应用层 + 原生层）。

### 硬件与感知
- **摄像头 / 麦克风实时拦截**：即使已拿到权限也打不开硬件。
- **传感器拦截**：禁用全部传感器，或逐项设置「不处理 / 禁用 / 伪装值 / 动态值」。
- **硬件能力拦截**：红外发射、生物识别、BLE 广播与主动扫描、点亮屏幕。
- **闪光灯 / 振动拦截**：禁止控制手电筒与震动。
- **屏幕捕获拦截**：拒绝授权、伪造图片 / 视频、伪造静态文字，画面可自适应。
- **音频输出拦截**：按媒体、通话、铃声、通知、闹钟、系统、TTS 分别拦截，也可只拦「有 / 无音频焦点」的播放。
- **音量控制**：总开关拦截、固定音量值、只允许调小、阻止改响铃模式与静音。

### 数据与踪迹
- **隐私数据拦截**：剪贴板读写（可分别控制）、通知发送、通知隐藏。
- **文件防护**：拦截 MediaStore 与 File / NIO 的创建、写入、删除、重命名等操作，可选「内部存储」或「所有路径」；文件重定向。
- **系统下载拦截**：拦掉系统下载与直接写入下载库。
- **隐藏应用列表**：让应用PackageManager枚举不到你装了哪些软件，支持白名单 / 黑名单。
- **网络请求域名过滤**：按黑白名单过滤域名（部分生效）。
- **屏幕与电源**：禁止应用使屏幕常亮、禁止息屏但不锁屏、释放屏幕常亮锁、通话息屏、禁止从后台任务中隐藏、禁止后台弹出界面（含 PendingIntent 弹窗）。
- **按键拦截**：禁止应用消费按键事件，可单独放行返回键。

### 跨应用与常驻
- **跳转拦截**：阻止跳转到其他应用，支持白名单、未知包名处理策略。
- **IPC 拦截**：显式 Intent、FileProvider、ContentProvider、BroadcastReceiver 四类通道，收发双向可分别控制，支持严格模式与 authority 白名单。
- **禁止前台常驻服务**：不允许应用挂着常驻通知活着。
- **无线调试拦截**：堵死 ADB over Wi-Fi 的开关、配对、发现、跳转与系统属性入口(部分有效)。
- **Shizuku 拦截**：阻止申请授权，也禁用已授权后的调用。

### 稳定性与退出
- **退出功能**：倒计时、指定时刻、内存超阈值、CPU 超阈值、存储增量、断网 / 联网、电量或充电状态、指定文件变动、长时间未触摸、卡顿累计、音量变化 —— 条件可并行监听，触发后按指定方式退出。
- **阻止闪退 / 自杀**：强度可调，分项覆盖进程自杀、退出指令、信号、kill 命令、系统服务杀进程、未捕获异常、消息循环异常、退后台结束任务、等待调试器、线程守护；原生层退出与崩溃拦截能力有限。
- **应用层异常捕获器**：崩溃时复制堆栈到剪贴板、写入应用私有目录、拦截应用抛出的异常、崩溃后弹出详情界面（可倒计时自动退出）。
- **拦截自动返回操作**：关界面、退回上一页、退后台都拦下。

---

## 🎭 伪装功能

### 设备信息（Build 字段）
- 型号、品牌、厂商、指纹、序列号、硬件、板型、设备名、SoC、产品、Bootloader、Radio、Display、安全补丁、增量版本、代号、Base OS、编译时间 / 主机 / 标签 / 类型 / 用户等项。
- 支持**一键随机**生成与全部清空。

### 标识伪装
- Android ID、GSF ID、广告 ID（GAID）、App Set ID、DRM ID、DRM System ID、Widevine 等级。
- OAID / ODID / VAID / AAID、Firebase FID / IID。

### 硬件与内核
- 内核版本、架构、平台、硬件厂商标识。
- CPU：芯片型号、板型代号、核心数、每核频率。
- GPU：厂商、渲染器、GL 版本与 GLSL、Vulkan API、驱动信息、Vendor / Device ID、显存与纹理上限。
- 设备温度、电量百分比（可模拟自动掉电）、电池细节（充电状态、电流、电压、设计容量）。

### 显示与存储
- 应用 DPI、屏幕分辨率与刷新率（含支持的刷新率列表）。
- 内存与存储：总运行内存、可用内存、存储总容量、可用容量。
- 相机分辨率：按像素数生成前后摄支持的拍照尺寸。

### 网络与 SIM
- WiFi / 蓝牙 MAC、SSID / BSSID、IMEI / MEID / ICCID、SIM 序列号、订阅 ID、运营商与名称、国家码、手机号。
- eSIM 支持、移动数据、漫游状态伪装。
- VPN / 抓包代理隐藏：VPN 网卡接口、传输能力、旧版 NetworkInfo、HTTP 代理、相关系统设置，接口名可自定义。
- 网络身份：内网 IPv4 / IPv6、网关、DNS、WiFi 标准，并覆盖 Socket、LinkAddress、InetAddress 与原生层取地址的多种路径。

### WiFi / 蓝牙状态伪装
- 断网时把网络状态报成「已连接 WiFi」，SSID、BSSID、信号、速率、IP、频率可自定义，可伪造扫描热点列表、屏蔽已保存网络查询。
- 蓝牙：伪装开启状态、本机名称与 MAC、已配对设备、已连接设备、扫描列表与设备类型。

### 系统环境
- 时区、语言 / 地区、开机时长、vbmeta digest、boot id。
- 系统时间：指定时刻（继续走秒 / 定住不走）或偏移分钟，可同步伪装原生层。
- 伪装开发者选项已关闭，USB 调试与无线调试单独控制。

### 位置与基站
- 坐标（纬度 / 经度 / 海拔 / 精度），可改写所有 Location 对象字段。
- 按通道拦截：GPS（含 GNSS 原始数据：卫星状态、测量值、NMEA）、网络定位（含 WiFi 扫描结果）、融合定位、电话服务。
- 基站信息：MCC / MNC / LAC / CID / PCI；电话与短信存储（sms / mms / call_log）。

### 服务与 Root
- **伪装拥有 Google Play 服务**：包查询、可用性检查、GAID、版本信息。
- **伪装拥有华为移动服务（HMS）**：包查询、可用性检查、OAID、版本信息。
- **Root 伪装**：伪装 su 文件存在，屏蔽 Permission denied 伪装返回执行成功，可切换「有 Root / 无 Root(效果有限)」。

### 权限伪装
- 覆盖通讯录、短信、通话记录、电话、存储、位置、日历、相机、麦克风、身体传感器、通知、附近设备、运动识别、健康数据、无障碍、设备管理员、通知监听、使用情况、悬浮窗、安装未知应用、所有文件访问、精确闹钟、电池优化、自启动、VPN、修改系统设置、后台弹出界面、媒体管理、全屏通知、音频设置、锁屏显示、动态壁纸、勿扰、画中画、付费短信等权限组。
- 勾选即伪装成「已授权」，部分权限可返回**伪造数据**：假联系人、假短信、假通话记录、假文件目录、固定坐标、假日程、符合量程的传感器值、假通知、假蓝牙设备、假使用统计等。

### 网页 / WebView 伪装
- User-Agent：预设随机或手动填写。
- WebView 指纹：屏幕与像素比、CPU 核数、内存、触摸点数、平台与厂商、WebGL 厂商与渲染器、`navigator.languages`、时区偏移。
- 时间与地理：改写 JS 的 `Date`、时区与 `Intl.timeZone`、伪造地理位置。
- 隐藏 webdriver / selenium 标记、声明 DNT、WebRTC 防护或直接停用。
- 指纹噪声：Canvas / WebGL / Audio 取值加随机扰动，强度可调。
- 基础开关：强制启用 JavaScript、忽略证书错误、注入自定义 JavaScript。

---

## 🧰 其他功能

- **文字替换**：网页、View、绘制 / Compose、其它组件（通知、磁贴、Toast、媒体等）四类范围，支持精确 / 正则 / 宽容匹配、忽略大小写、长文本，并带替换诊断。
- **点击内容**：页面出现指定文字或控件 id 就自动点击，支持包含 / 精确匹配、重复间隔与次数。
- **控件拦截**：按控件 ID、文字、资源名、类名、内容描述、坐标区域隐藏控件，带预设类型与状态，可输出到日志。
- **去除弹窗**：弹窗左上角挂圆钮，点击后选择处理方式。
- **拦截方法执行**：按类名 + 方法名拦截任意方法，可指定返回值、列出参数类型、输出日志。
- **命令执行记录**：记录应用实际执行过的命令（含原生层），支持弹窗提示、悬浮窗实时显示、静默记录，并可记录输出内容。
- **全局代理**：把勾选应用的流量送进隧道，支持白名单。
- **设备模板**：把一整套设备参数存成模板，一键套用到任意应用，支持导入 / 导出、复制、源码模式编辑。
- **测试检测**：用于测试应用的检测强度，总开关 + 字节一致性、类名检测、内存（maps / smaps）、注入痕迹、虚拟化、IO 重定向、日志特征过滤或清空；可全局默认，也可按应用覆盖。
- **原生层钩子**：覆盖属性、uname、时间、文件、stat、执行、退出、网络、读取、mmap、GPU、传感器等层面，可按应用单独开关。

---

## 🎛 控制面板

在目标应用屏幕左上角挂一个悬浮球，点开即展开面板，**不需要重启目标应用就能立即生效**：

- **日志分区**：多个日志区可切换，支持保存与复制。
- **增强拾取**：手指划过高亮、点击选定控件，然后对它移除、隐藏、改文字、改颜色、换图片、存图片、控件转图片、整页截图；还能用父控件 / 子控件 / 上一个 / 下一个切换。
- **持久化**：带「持久化」的改动会被记下来，下次进入应用自动生效。
- **一键功能**。
- **命令区**：共用一个 `sh` 会话，直接输入并执行命令。
- **控件信息**：查看控件属性、当前生效配置、权限自检、线程堆栈与分辨率等运行信息。
- **小细节**：面板可最小化或隐藏，隐藏后按音量键唤出；被对话框压住时，在屏幕任意位置 1 秒内连点五下即可重新置顶。

---

## 📱 模块自身

- **外观**：语言、深色模式与切换时间、主题颜色与配色风格、对比度与文字描边、动态壁纸、模糊与透明度。
- **界面**：主页切换动画、配置页向上弹出、圆角形状、配置页组件缩放与应用 DPI。
- **图标**：用任意图片在桌面创建自定义快捷方式图标，或禁用所有启动图标隐藏本模块。
- **其它**：配置备份导出与导入、检查更新、触摸震动、开屏动画、预测性返回手势。
- **LSPatch 模式**：激活后，应用页可直接配置（作用域在 LSPatch 里添加）。

---

## ⚠️ 使用须知

- 模块只在目标应用进程里工作，不会常驻任何后台服务。
- 部分修改配置后需要**强制停止目标应用再打开**才会生效。
- 防护类功能可能会影响目标应用的正常使用，请按需开启。
- 崩溃日志写在 `/storage/emulated/0/Android/data/{目标包名}/files/` 下。
- 测试检测会带来额外开销，效果有限，且默认关闭，按需开启。
- 🚫 请勿将本模块用于 Android 系统程序，禁止对涉及金融、账号安全、财产等关键场景使用本模块。本模块仅供学习研究与合法合规的技术测试使用，禁止用于规避风控、绕过授权、侵犯隐私、诈骗或其他违法用途；违规使用产生的一切后果由使用者自行承担。
- 若显示未激活、作用域不同步或未更新，重启本模块尝试解决。

---

## 开源许可

**…**

**第三方开源组件**
- [Dobby](https://github.com/jmpews/Dobby) — Apache-2.0
- [libxposed api / service](https://github.com/libxposed/libxposed) — Apache-2.0
- [AndroidX](https://github.com/androidx/androidx)（Core / Activity / Lifecycle / Compose / Media3）— Apache-2.0
- [Material Icons Extended](https://github.com/google/material-design-icons) — Apache-2.0
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) — Apache-2.0
- [Coil](https://github.com/coil-kt/coil) — Apache-2.0

---

<a id="en"></a>
# LockPerm (English)

**Spoof · Protect**  
An Xposed module based on Libxposed API 102 that brings together spoofing, protection, and control.

---

LockPerm is a privacy module that works on a **per-app basis**: it lets target apps read only what you want them to read (spoofing), blocks them from doing things you do not allow (protection), and provides an in-app floating panel for making live UI changes while the app is running (control).

On the **Apps** page, add the target app to the scope, tap **Configure**, and enable the features you need under the five tabs: **Protection / Permission Spoofing / Device Spoofing / Device Templates / Other Features**. Each app has its own independent configuration.

---

## 🚀 Quick Start

1. Enable this module in LSPosed Manager (LSPatch is also supported).
2. On the **Apps** page, request scope for the target app.
3. On the **Apps** page, tap **Configure** for an app that has been added to scope.
4. Enable the features you need under the five tabs; the tabs can be swiped left and right.
5. **Force stop the target app and reopen it** for the configuration to take effect.

---

## 🛡 Protection Features

### Permissions & Device Control
- **Accessibility protection**: Block enabling accessibility services; individually disable screen reading, notification listening, window-change listening, input-content reading, simulated actions, accessibility overlays, and key/gesture listening; clear the accessibility service list; spoof the accessibility enabled state.
- **Device Admin / Device Owner protection**: Prevent the app from actively requesting device admin privileges, and optionally disable lock screen, password change, factory reset, camera disable, app management, system settings, permission granting, encryption, and other capabilities.
- **Overlays**: Block system-level overlays, or "allow and modify" them — click-through, fully transparent, limit screen coverage, customize window flags, and add/remove FLAG_SECURE for all windows.
- **Wallpaper interception**: Intercept wallpaper changes regardless of the method used by the app.
- **Install / uninstall interception**: Block silent or guided installation, and optionally return a fake "operation succeeded" result.
- **Print / screen-cast interception**: Prevent the app from initiating printing or screen casting.
- **Command execution interception**: Intercept commands executed by the app (Java/app layer + native layer).

### Hardware & Perception
- **Camera / microphone real-time interception**: Block hardware access even if permissions have already been granted.
- **Sensor interception**: Disable all sensors, or configure each sensor as "do not handle / disable / spoof value / dynamic value".
- **Hardware capability interception**: Infrared emitter, biometrics, BLE advertising and active scanning, and screen wake-up.
- **Flashlight / vibration interception**: Prevent control of the flashlight and vibration.
- **Screen capture interception**: Deny authorization, spoof images/videos, spoof static text, with adaptive content.
- **Audio output interception**: Intercept media, calls, ringtones, notifications, alarms, system audio, and TTS separately; optionally intercept only playback with/without audio focus.
- **Volume control**: Master interception, fixed volume value, allow only lowering volume, and block changes to ringer mode and mute state.

### Data & Traces
- **Private data interception**: Clipboard read/write (controlled separately), notification sending, and notification hiding.
- **File protection**: Intercept MediaStore and File/NIO create, write, delete, rename, and other operations; choose "internal storage" or "all paths"; file redirection.
- **System download interception**: Block system downloads and direct writes to the Downloads collection.
- **Hide app list**: Prevent the app's PackageManager from enumerating installed apps; supports allowlist/denylist.
- **Network request domain filtering**: Filter domains by allowlist/denylist (partially effective).
- **Screen & power**: Prevent the app from keeping the screen on, prevent screen-off without locking, release screen-on locks, screen-off during calls, prevent hiding from recents, and prevent background UI pop-ups (including PendingIntent pop-ups).
- **Key interception**: Prevent the app from consuming key events; optionally allow the Back key.

### Cross-App & Persistence
- **Navigation interception**: Block jumps to other apps; supports allowlists and unknown-package handling policies.
- **IPC interception**: Explicit Intent, FileProvider, ContentProvider, and BroadcastReceiver channels; send and receive can be controlled separately; supports strict mode and authority allowlists.
- **Block persistent foreground services**: Do not allow the app to stay alive with a persistent notification.
- **Wireless debugging interception**: Block ADB over Wi-Fi toggles, pairing, discovery, navigation, and system property entry points (partially effective).
- **Shizuku interception**: Block authorization requests and disable calls after authorization.

### Stability & Exit
- **Exit features**: Countdown, specific time, memory threshold, CPU threshold, storage delta, network disconnect/connect, battery or charging state, specific file change, long idle without touch, accumulated lag, volume change — conditions can be monitored in parallel and trigger the configured exit method.
- **Prevent crashes / self-termination**: Adjustable strength; covers process self-kill, exit commands, signals, kill commands, system-service process killing, uncaught exceptions, message-loop exceptions, task finishing after backgrounding, waiting for debugger, and thread guard; native-layer exit and crash interception are limited.
- **App-layer exception catcher**: On crash, copy the stack trace to clipboard, write it to the app's private directory, intercept exceptions thrown by the app, and show a details screen after a crash (with optional countdown auto-exit).
- **Intercept automatic back actions**: Block closing screens, going back to the previous page, and going to background.

---

## 🎭 Spoofing Features

### Device Info (Build Fields)
- Model, brand, manufacturer, fingerprint, serial number, hardware, board, device name, SoC, product, bootloader, radio, display, security patch, incremental version, codename, Base OS, build time/host/tags/type/user, and more.
- Supports **one-tap random generation** and clearing all fields.

### Identifier Spoofing
- Android ID, GSF ID, advertising ID (GAID), App Set ID, DRM ID, DRM System ID, Widevine level.
- OAID / ODID / VAID / AAID, Firebase FID / IID.

### Hardware & Kernel
- Kernel version, architecture, platform, and hardware vendor identifiers.
- CPU: chip model, board codename, core count, per-core frequency.
- GPU: vendor, renderer, GL version and GLSL, Vulkan API, driver info, Vendor/Device ID, VRAM and texture limits.
- Device temperature, battery percentage (can simulate automatic drain), battery details (charging state, current, voltage, design capacity).

### Display & Storage
- App DPI, screen resolution and refresh rate (including supported refresh-rate list).
- Memory and storage: total RAM, available memory, total storage, available storage.
- Camera resolution: generate supported photo sizes for front and rear cameras based on pixel count.

### Network & SIM
- Wi-Fi/Bluetooth MAC, SSID/BSSID, IMEI/MEID/ICCID, SIM serial number, subscription ID, carrier and name, country code, phone number.
- eSIM support, mobile data, and roaming state spoofing.
- VPN / proxy hiding: VPN network interface, transport capabilities, legacy NetworkInfo, HTTP proxy, related system settings, with customizable interface name.
- Network identity: LAN IPv4/IPv6, gateway, DNS, Wi-Fi standard; covers Socket, LinkAddress, InetAddress, and multiple native-layer address lookup paths.

### Wi-Fi / Bluetooth State Spoofing
- When offline, report the network state as "connected to Wi-Fi"; customize SSID, BSSID, signal, rate, IP, and frequency; spoof scanned hotspot lists; block saved-network queries.
- Bluetooth: spoof enabled state, local name and MAC, paired devices, connected devices, scan list, and device types.

### System Environment
- Time zone, language/locale, uptime, vbmeta digest, boot id.
- System time: specific time (keep ticking / freeze) or minute offset; can also spoof the native layer.
- Spoof developer options as disabled; control USB debugging and wireless debugging separately.

### Location & Cell Towers
- Coordinates (latitude/longitude/altitude/accuracy); can rewrite all Location object fields.
- Intercept by channel: GPS (including GNSS raw data: satellite status, measurements, NMEA), network location (including Wi-Fi scan results), fused location, and telephony.
- Cell tower info: MCC/MNC/LAC/CID/PCI; phone and SMS storage (sms/mms/call_log).

### Services & Root
- **Spoof Google Play services as installed**: package query, availability check, GAID, version info.
- **Spoof Huawei Mobile Services (HMS) as installed**: package query, availability check, OAID, version info.
- **Root spoofing**: spoof su file existence, mask "Permission denied" as successful execution, and switch between "rooted / not rooted (limited effect)".

### Permission Spoofing
- Covers contacts, SMS, call logs, phone, storage, location, calendar, camera, microphone, body sensors, notifications, nearby devices, activity recognition, health data, accessibility, device admin, notification listener, usage access, overlays, install unknown apps, all files access, exact alarms, battery optimization, autostart, VPN, modify system settings, background pop-ups, media management, full-screen notifications, audio settings, lock-screen display, live wallpaper, do-not-disturb, picture-in-picture, premium SMS, and other permission groups.
- When enabled, permissions are spoofed as "granted"; some permissions can return **fake data**: fake contacts, fake SMS, fake call logs, fake file directories, fixed coordinates, fake calendar events, in-range sensor values, fake notifications, fake Bluetooth devices, fake usage stats, and more.

### Web / WebView Spoofing
- User-Agent: randomized presets or manual input.
- WebView fingerprint: screen and pixel ratio, CPU cores, memory, touch points, platform and vendor, WebGL vendor and renderer, `navigator.languages`, timezone offset.
- Time and geolocation: rewrite JS `Date`, timezone and `Intl.timeZone`, spoof geolocation.
- Hide webdriver/selenium markers, declare DNT, WebRTC protection or disable it entirely.
- Fingerprint noise: add random perturbation to Canvas/WebGL/Audio values with adjustable strength.
- Basic switches: force-enable JavaScript, ignore certificate errors, inject custom JavaScript.

---

## 🧰 Other Features

- **Text replacement**: Four scopes — web, View, drawing/Compose, and other components (notifications, tiles, Toast, media, etc.); supports exact/regex/lenient matching, case-insensitive matching, long text, and replacement diagnostics.
- **Tap content**: Automatically tap when specified text or control ID appears on screen; supports contains/exact matching, repeat interval, and repeat count.
- **Control interception**: Hide controls by control ID, text, resource name, class name, content description, or coordinate area; includes preset types and states; can output to logs.
- **Remove dialogs**: Attach a round button to the top-left corner of dialogs; tap it to choose how to handle the dialog.
- **Method execution interception**: Intercept any method by class name + method name; specify return value, list parameter types, and output logs.
- **Command execution recording**: Record commands actually executed by the app (including native layer); supports pop-up prompts, real-time floating display, silent recording, and optional output capture.
- **Global proxy**: Route traffic from selected apps into a tunnel; supports allowlists.
- **Device templates**: Save a full set of device parameters as a template and apply it to any app with one tap; supports import/export, copy, and source-mode editing.
- **Detection testing**: For testing an app's detection strength — master switch + byte consistency, class-name detection, memory (maps/smaps), injection traces, virtualization, IO redirection, log signature filtering or clearing; can be a global default or overridden per app.
- **Native-layer hooks**: Cover properties, uname, time, files, stat, exec, exit, network, read, mmap, GPU, sensors, and more; can be toggled per app.

---

## 🎛 Control Panel

A floating ball is attached to the top-left corner of the target app's screen. Tap it to open the panel, and changes take effect immediately **without restarting the target app**:

- **Log sections**: Multiple log sections can be switched; supports saving and copying.
- **Enhanced picker**: Swipe to highlight and tap to select a control, then remove it, hide it, change text, change color, replace image, save image, convert control to image, or take a full-page screenshot; also switch between parent/child/previous/next controls.
- **Persistence**: Changes marked as "persistent" are remembered and applied automatically the next time the app starts.
- **One-tap functions**.
- **Command section**: Share one `sh` session for entering and executing commands directly.
- **Control info**: View control properties, currently effective configuration, permission self-check, thread stacks, resolution, and other runtime information.
- **Details**: The panel can be minimized or hidden; after hiding, press a volume key to bring it back; if covered by a dialog, tap five times within 1 second anywhere on the screen to bring it to front.

---

## 📱 The Module Itself

- **Appearance**: Language, dark mode and switch time, theme color and color style, contrast and text outline, live wallpaper, blur and transparency.
- **Interface**: Home transition animation, config page slide-up, corner shape, config page component scale, and app DPI.
- **Icons**: Create a custom shortcut icon on the home screen from any image, or disable all launcher icons to hide the module.
- **Other**: Configuration backup export/import, check for updates, touch vibration, splash animation, predictive back gesture.
- **LSPatch mode**: After activation, apps can be configured directly on the Apps page (add scope in LSPatch).

---

## ⚠️ Notes

- The module only works inside the target app's process and does not run any persistent background service.
- Some configuration changes require **force stopping the target app and reopening it** to take effect.
- Protection features may affect the target app's normal use; enable them as needed.
- Crash logs are written to `/storage/emulated/0/Android/data/{target package name}/files/`.
- Detection testing adds overhead, has limited effectiveness, is disabled by default, and should be enabled only when needed.
- 🚫 Do not use this module for Android system programs. Use of this module in critical scenarios involving finance, account security, property, etc. is prohibited. This module is intended solely for learning, research, and lawful and compliant technical testing. It is prohibited to use it to evade risk controls, bypass authorization, infringe on privacy, commit fraud, or for other illegal purposes; all consequences arising from misuse shall be borne by the user.
- If the module shows inactive, scope is out of sync, or it is not updated, try restarting the module.

---

## License

**…**

**Third-party open-source components**
- [Dobby](https://github.com/jmpews/Dobby) — Apache-2.0
- [libxposed api / service](https://github.com/libxposed/libxposed) — Apache-2.0
- [AndroidX](https://github.com/androidx/androidx) (Core / Activity / Lifecycle / Compose / Media3) — Apache-2.0
- [Material Icons Extended](https://github.com/google/material-design-icons) — Apache-2.0
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) — Apache-2.0
- [Coil](https://github.com/coil-kt/coil) — Apache-2.0
