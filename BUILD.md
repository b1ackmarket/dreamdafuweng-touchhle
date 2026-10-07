# 梦幻富翁 touchHLE Android 构建说明

把《梦幻富翁 iPad HD》(dfw2012_chs_ipad.app, armv7, iOS 5.1.1+) 跑在现代
64 位 Android 上：64 位 touchHLE 宿主直接运行原版 ARMv7 客体二进制，
无需重写游戏。

已验证：Linux Xvfb 下 60 FPS 稳定运行 120 秒+，标题画面正常渲染；
Android APK 在小米 14 Pro 上待真机验证。

## 0. 上游版本（必须对上）

- 上游：https://github.com/hikari-no-yume/touchHLE
- 固定提交：`d34530b` ("Add NSMutableArray initWithObjects")
- 本仓库的 `dfw.patch` 就是相对该提交的完整 diff（含新增文件）。

```bash
git clone https://github.com/hikari-no-yume/touchHLE
cd touchHLE
git checkout d34530b
git apply /path/to/dfw.patch
```

`dfw.patch` 内容（约 20 处兼容层，缺一不可）：

- `src/lib.rs`：Android 点击即玩 —— 首启把 APK assets 里的
  `DreamDaFuWeng.ipa` 复制到 `touchHLE_apps/` 后直接启动，
  参数 `--landscape-right --device-family=ipad`，跳过文件选择器
- `src/frameworks/foundation/ns_operation_queue.rs`（新增）：
  NSOperationQueue
- `src/frameworks/foundation/ns_block.rs`（新增）：Objective-C blocks
- `src/libc/common_crypto.rs`（新增）：CommonCrypto
  HMAC / MD5 / SHA1 / SHA256
- `src/libc/dispatch.rs`（新增）：`dispatch_once`
- `src/libc/cxxabi.rs`：`___stack_chk_guard`
- `src/frameworks/core_foundation/cf_uuid.rs`：`CFUUIDGetUUIDBytes`
- `src/frameworks/core_foundation/cf_run_loop.rs`：
  `CFRunLoopSourceCreate` 等 run loop source API
- `src/frameworks/uikit/ui_view/ui_window.rs`：
  `UIWindow setRootViewController:`
- `src/frameworks/foundation/ns_date.rs`：
  `NSDate dateByAddingTimeInterval:`
- `src/frameworks/foundation/ns_thread.rs`：
  `performSelector:onThread:withObject:waitUntilDone:`、
  `NSThread name`
- `src/frameworks/foundation/ns_url.rs`：NSURL 完整 URL 的 path 解析
- `src/frameworks/foundation/ns_object.rs`、
  `src/frameworks/foundation/ns_notification_center.rs`、
  `src/objc.rs`、`src/objc/messages.rs`、`src/dyld.rs`、
  `src/libc.rs`：`___udivdi3`、`___floatundisf` 等底层符号与
  UIKit line-break mode 容错
- `src/frameworks/uikit/ui_font.rs`：字体相关补齐

## 1. 工具链

- Rust（stable）+ `cargo-ndk`（3.5.4 验证过）+
  `rustup target add aarch64-linux-android`
- Android SDK：platform android-31、build-tools 31.0.0
  （aapt2 / d8 / zipalign / apksigner）
- Android NDK r25c（cargo-ndk 用）
- JDK 17（javac / d8 / apksigner 用）

## 2. 编 Rust 动态库（arm64）

```bash
export ANDROID_NDK_HOME=<ndk-r25c> ANDROID_NDK_ROOT=<ndk-r25c>
cargo ndk --target aarch64-linux-android --platform 21 -- \
  build --release --lib \
  --no-default-features \
  --features "touchHLE_openal_soft_wrapper/static,sdl2/bundled"
```

产物：

- `target/aarch64-linux-android/release/libtouchHLE.so`（约 20 MB）
- `libSDL2.so`（约 2.6 MB，sdl2/bundled 编出，
  在 `target/aarch64-linux-android/release/` 下用
  `find target/aarch64-linux-android -name 'libSDL2.so'` 定位）

## 3. 编 Java → classes.dex

Java 源码（三处拼起来）：

1. 上游 `vendor/SDL/android-project/app/src/main/java/org/libsdl/app/*.java`
  （SDLActivity 等，照抄不用改）
2. 上游 `android/app/src/main/java/org/touchhle/android/MainActivity.java`
  （照抄不用改）
3. 本仓库 `android-build/java/org/touchhle/android/DocumentsProvider.java`
   （50 行极简 stub；上游原版是 292 行 Kotlin，这里用 stub 代替，
   只影响文件共享功能，不影响游戏）

```bash
ANDROID_JAR=<sdk>/platforms/android-31/android.jar
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d classes \
  $(find java -name '*.java')
d8 --min-api 21 --lib "$ANDROID_JAR" --output dex/ \
  $(find classes -name '*.class')
# 得到 dex/classes.dex
```

## 4. 打资源包（aapt2）

资源与 Manifest 在本仓库 `android-build/` 下（已是加工好的最终形态，
占位符已替换；包名 `org.touchhle.dreamdafuweng`，minSdk 21，
targetSdk 31，label `梦幻富翁`）：

- `android-build/AndroidManifest.xml`
- `android-build/res/values/strings.xml`
- `android-build/res/drawable-nodpi/icon.png`（**需自备**：任意 192x192 PNG
  放这里当占位图标；原版图标是 Apple CgBI PNG，aapt2 读不了，
  随便找张图不影响运行）

```bash
aapt2 compile --dir android-build/res -o compiled_res.zip
aapt2 link -o base.apk -I "$ANDROID_JAR" \
  --manifest android-build/AndroidManifest.xml \
  --min-sdk-version 21 --target-sdk-version 31 \
  compiled_res.zip
```

## 5. 组装 APK（签名必须是最后一步）

```bash
mkdir -p apk/lib/arm64-v8a apk/assets
unzip -q -o base.apk -d apk
cp dex/classes.dex apk/classes.dex
cp libtouchHLE.so libSDL2.so apk/lib/arm64-v8a/
# 血泪教训:libtouchHLE.so 的 NEEDED 里有 libc++_shared.so(NDK 自带),
# 必须一起打进 apk/lib/arm64-v8a/,否则启动时 dlopen 直接失败秒崩。
# 从 NDK 取:toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so
cp /path/to/libc++_shared.so apk/lib/arm64-v8a/
# 游戏本体：必须叫这个名字，lib.rs 里写死了从 assets 读它
cp /path/to/DreamDaFuWeng.ipa apk/assets/DreamDaFuWeng.ipa
# 血泪教训2:touchHLE 自带的 dylibs/fonts/默认配置也必须进 assets,
# 缺了启动时直接 panic("Unexpected I/O failure ... libsqlite3.dylib")。
# 注意上游 android/app/src/main/assets 下这三个是软链接,要解引用复制:
cp -rL <touchHLE>/touchHLE_dylibs <touchHLE>/touchHLE_fonts apk/assets/
cp -L <touchHLE>/android/app/src/main/assets/touchHLE_default_options.txt apk/assets/
cd apk && zip -qr ../unsigned.apk . -x 'assets/DreamDaFuWeng.ipa'
# ipa 必须以 STORED（不压缩）方式加入：
zip -qr ../unsigned.apk assets/DreamDaFuWeng.ipa -Z store
cd ..
```

注意：`DreamDaFuWeng.ipa`（29.6 MB）就是游戏本体 zip，
从用户手里的 `梦幻富翁_iPadHD_砸壳版.ipa` 改名即可，
里面是 `dfw2012_chs_ipad.app`（cryptid=0，已解密）。

## 6. 对齐 + 签名

```bash
zipalign -p 4 unsigned.apk aligned.apk
# 用你自己的 keystore；debug key 也能装
apksigner sign --ks my.keystore --out 梦幻富翁.apk aligned.apk
apksigner verify --verbose 梦幻富翁.apk
# 应显示 v1/v2/v3 均为 true（Android 11+ 强制要求 v2+）
```

顺序不能错：**先加完所有文件 → zipalign → 最后签名**。
签名之后再往包里加东西会破坏 v2 签名导致装不上。

## 7. Linux 下先验证游戏能跑（推荐）

```bash
cargo build --release
# 无声卡环境加 ALSOFT_DRIVERS=null；需要 X 或 Xvfb
ALSOFT_DRIVERS=null ./target/release/touchHLE /path/to/dfw2012_chs_ipad.app \
  --device-family=ipad --landscape-right --print-fps
```

应看到标题画面「梦幻富翁」「点击屏幕，开始抢钱！」，
EAGL/Core Animation 约 60 FPS，无 panic。

## 已知将就的两处

1. 图标是临时画的骰子占位图（原 CgBI 图标 aapt2 不认），不影响运行。
2. DocumentsProvider 是极简 stub，只影响 Android 文件共享，
   不影响游戏。

## 真机验收标准

1. 能安装（文件 35,889,839 字节；报"解析包时出现问题"多半是没下全）
2. 点图标直接进游戏，不经过 touchHLE 文件选择器
3. 出现标题画面，触摸可进菜单
4. 黑屏/闪退 → 抓 `adb logcat`，看 `touchHLE` / Rust panic / linker 报错
