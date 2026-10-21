# Pico 电量提示音

轻量 Android 应用，使用原生 Java / Android SDK，无网络权限，无广告或分析 SDK。最低 Android 8.0。

## 功能

- 插上充电器播放一次提示音。开启监听时如果已经接上电源，不补播充电音。
- 低电量默认 20%，超低电量默认 5%，阈值可调整，超低阈值必须低于低电量阈值。
- 接电与未接电时均检查低电量，支持有线串流时边充边掉电。各档提醒播放一次，回升到对应阈值以上 2 个百分点后重新允许提醒。若直接下降到超低档，只播放超低提示音。
- 插线只播放充电音，不补播插线时已经处于的低电量档位；继续下降到尚未触发的超低档仍会提醒。拔线后重置并重新检查低电量。
- 电源插拔和电量变化收到系统广播后立即处理，不添加等待。调试日志记录原始电源类型与电量，方便核对实际插拔与系统状态变化。
- 开启监听时若已处于低电量，立即播放对应提醒。
- 各档可以单独开关，试听，或选择手机上的音频文件。内置两个 OGG 提示音，低电量和超低电量共用同一声音。
- 音量分为充电和低电量两组（0–150%），低电量与超低电量共用音量；两组均提供试听，也适用于自定义音频。升级时保留原音量。0% 静音，100% 原始音量，超过 100% 使用当前音频会话的 LoudnessEnhancer 增益，150% 约为 +3.52 dB。设备不支持增强时提示并按 100% 播放。设置自动保存，不修改系统音量，实际响度仍受系统通知音量与勿扰影响。
- 常驻通知显示电量，并提供停止按钮；不绕过静音或勿扰，音频最多播放 10 秒。

## 使用

亮屏、息屏/休眠各有一套独立音量，每套都包含充电与低电量（含超低电量），范围 0–150%。升级时息屏音量复制当前亮屏设置，之后独立保存。每次播放时通过 PowerManager.isInteractive 和默认内置屏幕状态选择音量，不使用 VR 应用虚拟屏幕状态，也不唤醒屏幕或阻止休眠。音量设置区的试听固定使用对应组，便于在亮屏时调整息屏音量。

从 [Releases](https://github.com/Hrenact/PicoBatterySound/releases) 下载 APK（或自行构建 `outputs/PicoBatterySound-debug.apk`），打开应用，点击“开启电量提醒”，允许通知权限。
打开“开机恢复提醒”后，重启并首次解锁手机时恢复上次已开启的监听。应用覆盖更新后也会恢复监听。
请在“自启与后台运行”中申请电池优化豁免，并在 vivo / iQOO 系统设置中允许自启动和后台运行。
手动关闭监听会持久保存关闭状态，开机或更新不会重新开启。关闭再开启监听会开始新的监听周期。
服务使用常驻前台通知、START_STICKY 和 stopWithTask=false；滑走界面不主动停止服务，系统回收后请求重建。
提醒周期状态会持久保存，服务重建或开机恢复不会仅因内存状态丢失重复提醒。
系统强制停止、Android 任务管理器的停止操作以及厂商清理仍可能阻断运行；重新打开应用会尝试恢复已启用的监听。
不使用循环闹钟、无声音乐或永久唤醒锁。系统恢复时间和厂商保活效果不能保证。

## 本机构建

```powershell
.\build.ps1
```

- 安装 JDK 17 和 Android SDK（Android SDK Platform 36、Build Tools 35.0.0），设置 `JAVA_HOME` 和 `ANDROID_HOME`，或在不入库的 `local.properties` 中指定 `sdk.dir`。Android Studio 可用于安装 SDK 和编辑工程，也可仅使用命令行工具。
- 仓库包含 Gradle 8.13 Wrapper，首次构建会下载 Gradle 和依赖；Android Gradle Plugin 为 8.13.0。
- Gradle 缓存、Android 用户目录及构建临时目录分别位于 `.gradle-home`、`.android`、`work`。
- macOS / Linux 可运行 `./gradlew :app:assembleDebug :app:lintDebug`，APK 位于 `app/build/outputs/apk/debug/`。
- 构建脚本执行 `assembleDebug` 和 `lintDebug`，然后复制 APK 到 `outputs`。
- 当前 APK 为开发调试签名，不是应用商店发布包。请保留项目内 `.android` 中的调试签名，以便后续覆盖升级。

## 逻辑测试

```powershell
javac -d work/test-classes app/src/main/java/com/pico/batterysound/BatteryPolicy.java tests/BatteryPolicyTest.java
java -cp work/test-classes com.pico.batterysound.BatteryPolicyTest
```

测试包含充电去重、阈值边界、跨档、阈值附近波动、低电量启动和充电重置。
内置音频位于 `app/src/main/res/raw/`：`charging_started.ogg` 用于充电，`low_battery.ogg` 用于低电量和超低电量。

## 开源许可

应用原创代码采用 [MIT License](LICENSE)。内置提示音来自 Android Open Source Project，保留 Apache License 2.0；Gradle Wrapper 同样采用 Apache License 2.0。具体来源、文件哈希及许可见 [第三方声明](THIRD_PARTY_NOTICES.md)。本项目为独立开发，与 PICO 官方无隶属关系。
