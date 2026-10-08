# 梦幻富翁 iPad HD - touchHLE Android 移植

将 iOS 经典游戏《梦幻富翁 iPad HD》(v1.0.2) 通过 touchHLE 模拟器移植到 Android arm64。

- 上游: hikari-no-yume/touchHLE@d34530b
- 目标设备: 小米 14 Pro (Android arm64)
- 游戏: dfw2012_chs_ipad.app (armv7, OpenGL ES 1.1, 自研引擎, 非 cocos2d)

## 当前进展 (2026-10-08, v24)

### 可玩状态
- ✅ 正常进入标题画面
- ✅ 点击进入主菜单 → 单人游戏 → 通关模式
- ✅ 进入游戏棋盘，掷骰子走棋
- ✅ 点击游戏右侧不再崩溃 (v22 修复 UIGestureRecognizer)

### 已知问题
- ⚠️ AI 角色会在两局内破产 (原因未找到；已排除随机数和 NSDecimalNumber)
- ⚠️ 部分数字显示反转、部分汉字乱码 (疑似 CoreText 未实现)
- ⚠️ 游戏胜利后可能闪退 ("left == right" 断言，需复现抓日志)
- ⚠️ 画面为 4:3 居中，两侧有黑边 (用户要求保持比例，不拉伸)

### 技术要点
- 动态链接 libSDL2.so (静态链接会导致 SDL JNI 未初始化崩溃)
- 补齐 guest intrinsic: `__divmodsi4`, `__floatundisf`, `__fixsfdi` 等
- mutex 重复 unlock 改为容错 (不 panic)
- `UIGestureRecognizer` 及其子类走 FakeClass 兼容路径
- 新增 `NSDecimalNumber` 最小实现 (金钱计算)
- 新增 GCD `dispatch_async/sync/get_global_queue` (同步执行代替)

## 文件说明

- `dfw.patch`: 相对上游 d34530b 的完整补丁，`git apply` 即用
- `BUILD.md`: 完整编译步骤
- `build-v17.sh`: Android arm64 构建脚本
- `android-build/`: 加工好的 Manifest/资源/Java stub

> 注意: `DreamDaFuWeng.ipa` (29MB) 因体积原因未进仓库，需自备放入 `android/app/src/main/assets/`

## 构建

```bash
# 1. 应用补丁
cd touchHLE && git apply ../dfw.patch

# 2. 编译 (见 build-v17.sh)
cargo ndk --target aarch64-linux-android --platform 21 -- \
  build --release \
  --no-default-features \
  --features sdl2/bundled,touchHLE_openal_soft_wrapper/static
# RUSTFLAGS 需指向 sdl2-sys 编出的 libSDL2.so

# 3. 打包 APK (手动路线: javac + d8 + aapt2 + apksigner)
```

详见 `BUILD.md`。

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v3 | 2026-10-07 | 稳定基线，能进主界面 |
| v17 | 2026-10-07 | 恢复动态 SDL 链接，越过 divmod 死点 |
| v18 | 2026-10-08 | 补浮点 intrinsic，进主界面但 mutex panic |
| v19 | 2026-10-08 | mutex 容错 |
| v20 | 2026-10-08 | 补 `__fixsfdi` 等；ADB 验证进入棋盘 |
| v21 | 2026-10-08 | 强制拉伸全屏 (用户否定：比例更重要，已废弃) |
| v22 | 2026-10-08 | UIGestureRecognizer 防崩 + 恢复 4:3 比例 |
| v23 | 2026-10-08 | 与 v22 内容相同 (重新打包，无新修复) |
| v24 | 2026-10-08 | 新增 NSDecimalNumber / GCD / UIGraphics 桩 |
