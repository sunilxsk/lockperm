# LockPerm

一个基于 **libxposed API 101** 的 Xposed 模块，把「伪装」和「防护」两件事放在一起：伪装负责让应用看到你想让它看到的东西，防护负责阻止应用滥用权限。

所有功能都以**作用域为单位**生效：在「应用」页把目标应用加入作用域，再到「防护 / 伪装」两个页签里勾选它需要的功能即可。每个应用的配置互相独立，互不干扰。

---

## 功能概览

### 伪装

- **设备信息伪装**：Build 字段（MODEL / BRAND / FINGERPRINT / SERIAL / 系统版本等）一键随机或逐项填写
- **标识伪装**：Android ID、GSF ID、广告 ID、App Set ID、DRM ID、OAID、Firebase ID
- **系统属性伪装**：自定义 `getprop` / `SystemProperties` 返回值，自动展开 `ro.product.*` 系列变体
- **硬件与内核**：内核版本、架构、CPU 信息、温度、电量
- **网络与 SIM**：WiFi SSID / BSSID / MAC、蓝牙 MAC、IMEI / MEID / ICCID / IMSI、运营商、手机号
- **系统环境**：时区、语言、时间偏移、开机时长、伪装开发者选项已关闭
- **Root 伪装**：让应用以为设备已 Root（su 文件存在、命令返回成功）
- **VPN / 代理隐藏**：隐藏 VPN 网卡、传输能力、NetworkInfo、HTTP 代理
- **WiFi 连接状态伪装**：断网时让应用以为已连上 WiFi，可自定义扫描到的热点列表
- **WebView 注入**：向目标应用的 WebView 注入 JavaScript
- **User-Agent 伪装**：按预设随机生成或手动填写 UA
- **权限伪装**：让应用以为你授予了权限，部分权限可返回伪造数据（通讯录、短信、通话记录、存储、位置、日历）

### 防护

- **无障碍防护**：禁止开启无障碍服务，并破坏其读取屏幕、监听通知、模拟操作、按键监听等能力
- **设备管理员防护**：阻止应用滥用设备管理员 / Device Owner 权限，可开启「全部合一」持续解除身份
- **悬浮窗拦截**：阻止系统级悬浮窗创建
- **壁纸拦截**：阻止应用随意替换壁纸
- **音量控制拦截 / 固定音量**：阻止应用调节音量，或把音量锁定在指定值
- **音频输出拦截**：按声音类别（媒体 / 通话 / 铃声 / 通知 / 闹钟 / 系统 / TTS）禁用音频输出
- **剪贴板拦截**：禁止读写剪贴板（可分别控制读 / 写）
- **闪光灯 / 振动拦截**
- **WiFi / 蓝牙 / 亮度控制拦截**
- **传感器拦截**：注册不了监听，列表为空，默认传感器为 null
- **跳转拦截**：阻止应用跳转到其他应用，支持白名单
- **摄像头 / 麦克风实时拦截**：即使应用已拿到权限也打不开硬件
- **安装拦截**：阻止静默安装 / 引导安装 APK
- **打印 / 投屏拦截**
- **通知发送拦截**
- **网络域名过滤**：按黑名单 / 白名单拦截网络请求
- **屏幕捕获拦截**：三种策略 —— 伪造图片 / 视频、伪造静态文字、直接拒绝授权
- **文件创建拦截**：拦截 MediaStore 与 File / NIO 的创建、写入、删除等操作
- **隐藏应用列表**：让应用枚举不到你装了哪些软件，支持白名单 / 黑名单
- **无线调试拦截**：堵死 ADB over Wi-Fi 的各种入口
- **Shizuku 拦截**：阻止向 Shizuku 申请授权并破坏已授权后的调用
- **命令执行拦截**：拦截 Runtime.exec / ProcessBuilder / ProcessImpl
- **退出功能**：倒计时后按指定方式退出（杀进程 / 结束虚拟机 / SIGKILL / 空指针闪退 / 执行命令等）
- **阻止闪退 / 自杀**：拦截应用主动调用 killProcess / exit / halt / 信号等行为，可调强度等级
- **异常捕获器**：记录崩溃信息到剪贴板或私有目录，可选拦截应用抛出的异常
- **隐藏路径 / 文件**：让目标应用用任何方式都探测不到指定路径

### 控制面板

在目标应用左上角挂一个悬浮窗，点里面的按钮可以**立即生效**一系列常用功能，不需要重启目标应用。

### 其他

- 主题颜色自定义
- 桌面图标切换
- 配置页组件缩放
- 整套配置备份与恢复（导出 / 导入 JSON）
- 日志开关（默认关闭，排查问题时打开）
- Native 层 Hook，补 Java 层钩不到的部分

---

## 整体结构

```
LockPerm/
├── app/                       主应用（配置界面 + 模块入口）
│   ├── src/main/java/.../
│   │   ├── XposedModuleEntry   模块入口，按作用域分发 Hook
│   │   ├── XpConfig / XpState  配置键、默认值与快照
│   │   ├── Ui*                 配置页面（Compose）
│   │   ├── *Defender / *Blocker 各功能模块
│   │   └── ...
│   └── src/main/cpp/           原生层的钩子
├── build.gradle
└── settings.gradle
```

- **主应用**：Jetpack Compose 编写的配置界面，负责作用域管理、按应用配置、备份恢复、主题与图标设置。
- **模块入口**：`XposedModuleEntry` 在目标应用进程启动时读取该应用的配置快照，按需安装对应的 Hook。
- **Native 层**：可选的，补足 Java 层无法覆盖的底层调用（属性读取、文件访问、内核信息等）。

---

## 构建

**环境要求**

- Android SDK（compileSdk 36）
- JDK 17+
- Gradle 9.x（随项目 wrapper）
- NDK

**步骤**

```bash
git clone https://github.com/sunilxsk/LockPerm.git
cd LockPerm
./gradlew :app:assembleRelease
```

产物在 `app/build/outputs/apk/release/`。

**使用**

1. 在 LSPosed 管理器中启用本模块。
2. 在「应用」页为目标应用申请作用域。
3. 点该应用的「配置」进入专属配置页，在「防护功能」与「伪装」两个页签里勾选需要的功能。
4. 修改配置后**强制停止目标应用再打开**即可生效。

**注意**

- 模块只在目标应用进程里工作，不会常驻任何后台服务。
- 崩溃日志写在 `/storage/emulated/0/Android/data/{目标包名}/files/error.log`。
- 防护类功能可能影响目标应用的正常使用，请按需开启。
- 禁止对系统进程、金融、游戏、社交类 App 使用，后果自负。

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

---

项目地址：[https://github.com/sunilxsk/LockPerm](https://github.com/sunilxsk/LockPerm)