# Live 直播播放源优化与完善实施方案

## 1. 核心需求与架构设计

根据确认的选项，本方案针对直播播放引擎、网速诊断与全屏体验进行全方位升级：
1. **60 秒深度缓冲机制 (60s Deep Buffer LoadControl)**：
   - 定制 ExoPlayer 的 `DefaultLoadControl`，为直播流配置最高达 60 秒的超大缓冲池，并保留 30 秒回退后置缓冲，消除网络抖动引发的卡顿。
2. **实时下载速度监控 (Real-time Bandwidth Meter)**：
   - 接入 ExoPlayer 的 `DefaultBandwidthMeter`，实时统计每个分片的实际下载耗时与字节数，在播放详情页面的「观看统计」中实时动态展示当前下载速率（例如 `1,420 KB/s` / `2.1 MB/s`）。
3. **沉浸式横屏全屏播放与手势控制 (Landscape Fullscreen with Gestures)**：
   - 点击全屏按钮自动旋转至横屏全屏（`SCREEN_ORIENTATION_SENSOR_LANDSCAPE`），退出时恢复系统默认方向。
   - 实现车载与电视级别的屏幕手势交互：
     - **左半屏垂直滑动**：无级调节屏幕亮度，带有亮度百分比 HUD。
     - **右半屏垂直滑动**：无级调节音量，带有音量百分比 HUD。
     - **双击屏幕**：播放 / 暂停切换。
     - **一键锁屏**：锁定所有控制层，防误触。
     - **选台与线路快捷浮层**：横屏下可直接侧滑呼出频道列表与线路切换，无需退出全屏。
4. **主界面、竖屏详情与沉浸式界面的状态打通**：
   - 完善主界面（参考图3）、竖屏详情（参考图2）、竖屏沉浸（参考图1）与横屏全屏之间的一致性体验，支持上一台/下一台平滑切台与线路故障自动容灾。

---

## 2. 详细改动模块

### A. 播放服务层 (Domain & Playback Service)
- **文件**: `/app/src/main/java/com/yuivo9999/mybox123/domain/playback/NativePlaybackService.kt`
  - 构建定制化的 `DefaultLoadControl`：
    - `minBufferMs = 15_000`（最小缓冲 15 秒）
    - `maxBufferMs = 60_000`（最大目标缓冲 60 秒）
    - `bufferForPlaybackMs = 1_500`（1.5 秒极速起播）
    - `bufferForPlaybackAfterRebufferMs = 3_000`（二次缓冲 3 秒即恢复播放）
    - `backBufferDurationMs = 30_000`（保留 30 秒回放后置缓冲）
  - 注入单例 `DefaultBandwidthMeter`，提供每秒网速采样与平滑加权平均计算。

### B. 直播页面与横屏手势层 (UI & Screen Layer)
- **文件**: `/app/src/main/java/com/yuivo9999/mybox123/ui/screens/NativeLiveBrowseScreen.kt`
  - **网速实时计算**：
    - 监听 `BandwidthMeter` 并在 UI 状态中维护 `currentSpeedFormatted: String`（如 `1.8 MB/s`、`650 KB/s`）。
    - 将「网络速度 (估算)」从“暂不可用”更新为真实下载速度，并带网络健康等级指示（超清流畅 / 良好 / 缓冲中）。
  - **横屏全屏组件 (`LiveLandscapePlayerOverlay`)**：
    - 处理 Activity 窗口旋转与全屏 SystemBars 隐藏。
    - 手势识别器 (`pointerInput`)：精确判定左侧/右侧滑动区域，动态调整 `Window.attributes.screenBrightness` 与 `AudioManager` 媒体音量。
    - 横屏控制栏：顶层台标与时间、右侧锁屏按钮、底部进度/线路切换器、铺满/裁剪比例切换、一键退出。
  - **沉浸模式无缝融合**：
    - 保留竖屏沉浸视图（图1）与竖屏详情视图（图2），用户点击「全屏」即可进入全新横屏手势全屏，双向自由切换。

---

## 3. 验证与测试步骤

1. **编译检查**：
   - 运行 `compile_applet` 确保 Media3 扩展参数、手势处理与横屏生命周期逻辑无任何错误。
2. **缓冲机制验证**：
   - 确认播放器在启动后快速解码并在后台持续预读分片至缓冲上限。
3. **网速统计验证**：
   - 确认播放过程中「网络速度 (估算)」实时刷新为真实传输速率（如 `1.2 MB/s`）。
4. **全屏与手势交互测试**：
   - 点击全屏切换至横屏，测试左侧滑动调节亮度、右侧滑动调节音量、锁屏按钮、双击暂停播放，点击退出正常恢复竖屏状态。
