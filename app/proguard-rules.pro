# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ✅ 只保留你自己的代码（不混淆、不删除、不优化）
-keep class com.aritxonly.deadliner.** { *; }
-keepnames class com.aritxonly.deadliner.**
-keepattributes *                 # 行号、注解、签名等按需保留
# 如果你想让堆栈更好读，也可以加：
-keepattributes SourceFile,LineNumberTable

# ✅ 为了让库可被强力裁剪，别全局 -keep **；按需给库补规则
# Gson（你已遇到过的问题）
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class ** extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }

# Compose/Material/Lottie/Navigation 等一般无需额外 keep，除非你有反射/ServiceLoader

# FFI/JNA：按你的要求，FFI 相关全部不混淆。
# 1) UniFFI 绑定（Kotlin）不混淆（即使上面已有 com.aritxonly.deadliner.**，这里显式声明便于维护）
-keep class com.aritxonly.deadliner.lifi.** { *; }

# 2) JNA 相关类和内部类全部不混淆/不裁剪
-keep class com.sun.jna.** { *; }
-keep class com.sun.jna.internal.** { *; }

# 3) JNA 含 AWT/Swing 分支，Android 上不会使用；忽略对应缺类告警以通过 R8
-dontwarn java.awt.**
-dontwarn javax.swing.**
