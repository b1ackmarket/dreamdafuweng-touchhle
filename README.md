# 梦幻富翁 touchHLE 移植（Android arm64）

把 iOS 老游戏《梦幻富翁 iPad HD》（armv7 单架构，iOS 5.1.1+）
搬到现代 64 位 Android：**64 位 touchHLE 宿主直接运行原版 ARMv7
客体**，不是重写。

## 仓库内容

| 文件 | 说明 |
|---|---|
| `dfw.patch` | 相对上游 touchHLE `d34530b` 的完整补丁，`git apply` 即可 |
| `BUILD.md` | 从源码编出 APK 的完整步骤 |
| `android-build/` | 手工打包用的最终形态 Android 文件（Manifest/资源/Java stub） |

## 需要你自己准备的两个文件（不在仓库里）

1. **`DreamDaFuWeng.ipa`**（约 29.6 MB）：游戏本体。就是砸壳版 ipa
   改名，里面是 `dfw2012_chs_ipad.app`（cryptid=0，已解密）。
   打包时放到 APK 的 `assets/` 下，名字必须 exactly
   `DreamDaFuWeng.ipa`（lib.rs 里写死了从 assets 读这个名字）。
2. **`android-build/res/drawable-nodpi/icon.png`**（192x192 PNG）：
   应用图标。原版图标是 Apple CgBI 格式 aapt2 读不了，
   随便找张 PNG 放这里当占位即可，不影响运行。

## 快速开始（给 AI）

1. 读 `BUILD.md`，按步骤来。
2. 先在 Linux 下把游戏跑起来（标题画面 + 60 FPS），再打 Android 包。
3. 签名必须是组装完所有文件后的最后一步，否则 v2 签名失效装不上。

## 状态

- Linux Xvfb：120 秒+ 稳定运行，60 FPS，标题画面正常。
- Android APK：已构建（35 MB，`org.touchhle.dreamdafuweng`），
  小米 14 Pro 真机安装验证中。
- 两处将就：临时占位图标、极简 DocumentsProvider stub（见 BUILD.md）。

上游：https://github.com/hikari-no-yume/touchHLE
参考：https://github.com/moleworld-dev/MoleWorld-5.5.0-touchHLE-offline
