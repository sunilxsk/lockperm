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
  <img src="https://img.shields.io/badge/compileSdk-36-brightgreen" alt="compileSdk">
  <img src="https://img.shields.io/badge/JDK-17%2B-red" alt="JDK">

</p>

<p align="center">
  <b>伪装 · 防护 </b><br>
  一个基于 libxposed 的 Xposed 模块，把「伪装」和「防护」两件事放在一起。
</p>

---

一个基于 **libxposed** 的 Xposed 模块，主要做两件事：**伪装**（改变应用读到的内容）和**防护**（阻止应用滥用权限）。

在「应用」页把目标应用加入作用域，再到「防护 / 伪装」两个页签里勾选需要的功能即可。每个应用的配置互相独立，互不干扰。

---

## 功能概览

### 伪装

- **设备信息伪装**：让应用读到随机或自定义的 Build 字段（型号、品牌、指纹、序列号、系统版本等）。
- **标识伪装**：伪造 Android ID、GSF ID、广告 ID、App Set ID、DRM ID、OAID、Firebase ID 等设备标识。
- **硬件与内核**：伪造内核版本、架构、CPU 信息、温度和电量。
- **网络与 SIM**：伪造 WiFi、蓝牙、IMEI、ICCID、运营商、手机号等网络与 SIM 信息。
- **系统环境**：伪造时区、语言、开机时长，并让应用以为开发者选项已关闭。
- **Root 伪装**：让应用以为设备已 Root。
- **VPN / 代理隐藏**：隐藏 VPN 网卡、传输能力、NetworkInfo 和 HTTP 代理。
- **WiFi 连接状态伪装**：断网时让应用以为已连上 WiFi，并可自定义扫描到的热点列表。
- **User-Agent 伪装**：按预设随机生成或手动填写 UA。
- **权限伪装**：让应用以为你授予了权限，部分权限还能返回伪造的通讯录、短信、通话记录、存储、位置、日历数据。

### 防护

- **无障碍防护**：禁止开启无障碍服务，并禁用其读屏、监听通知、模拟操作、按键监听等能力。
- **设备管理员防护**：阻止应用滥用设备管理员 / Device Owner 权限。
- **悬浮窗拦截**：阻止系统级悬浮窗创建。
- **壁纸拦截**：阻止应用随意替换壁纸。
- **音量控制拦截 / 固定音量**：阻止应用调节音量，或把音量锁定在指定值。
- **音频输出拦截**：按声音类别拦截音频输出。
- **剪贴板拦截**：禁止读写剪贴板，读写可分别控制。
- **闪光灯 / 振动拦截**：阻止应用控制闪光灯或振动。
- **WiFi / 蓝牙 / 亮度控制拦截**：阻止应用开关 WiFi、蓝牙或调节亮度。
- **传感器拦截**：让应用拿不到任何传感器。
- **跳转拦截**：阻止应用跳转到其他应用，支持白名单。
- **摄像头 / 麦克风实时拦截**：即使应用已拿到权限也打不开硬件。
- **安装拦截**：阻止静默安装或引导安装 APK。
- **打印 / 投屏拦截**：阻止应用发起打印或投屏。
- **通知发送拦截**：阻止应用发送通知。
- **屏幕捕获拦截**：可伪造图片 / 视频、伪造静态文字，或直接拒绝授权。
- **文件创建拦截**：拦截 MediaStore 与 File / NIO 的创建、写入、删除等操作。
- **隐藏应用列表**：让应用枚举不到你装了哪些软件，支持白名单 / 黑名单。
- **无线调试拦截**：堵死 ADB over Wi-Fi 的各种入口。
- **Shizuku 拦截**：阻止向 Shizuku 申请授权，并禁用已授权后的调用。
- **命令执行拦截**：拦截应用执行命令。
- **退出功能**：倒计时后按指定方式退出应用。
- **阻止闪退**：拦截应用主动调用退出，强度可调。
- **异常捕获器**：把崩溃信息记录到剪贴板或私有目录。
- **隐藏路径 / 文件**：让目标应用用一些方式探测不到指定路径。

### 控制面板

在目标应用左上角挂一个悬浮窗，点里面的按钮可以**立即生效**一系列常用功能，不需要重启目标应用。

---

## 开源许可

本项目采用 **GNU AGPL v3.0** 许可证。
详见 [https://www.gnu.org/licenses/agpl-3.0.html](https://www.gnu.org/licenses/agpl-3.0.html)。

**第三方开源组件**

- [Dobby](https://github.com/jmpews/Dobby) — Apache-2.0
- [libxposed api / service](https://github.com/libxposed/libxposed) — Apache-2.0
- [AndroidX](https://github.com/androidx/androidx)（Core / Activity / Lifecycle / Compose / Media3）— Apache-2.0
- [Material Icons Extended](https://github.com/google/material-design-icons) — Apache-2.0
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) — Apache-2.0
- [Coil](https://github.com/coil-kt/coil) — Apache-2.0

。
