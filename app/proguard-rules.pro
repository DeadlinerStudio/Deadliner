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

# WorkManager instantiates its Room-generated database implementation by name at
# startup. R8 cannot see that reflective constructor call in a minified build.
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }

# Compose/Material/Lottie/Navigation 等一般无需额外 keep，除非你有反射/ServiceLoader

# Instantiated by privileged app_process launchers, outside Android component lookup.
-keep class com.aritxonly.deadliner.notification.IslandShizukuService { *; }
-keep class com.aritxonly.deadliner.notification.IslandRootService { *; }

# Persisted KMP notification metadata is read by Gson across APK upgrades and Alarm deliveries.
-keep class com.deadliner.platform.NotificationMetadataEntry { *; }
-keep class com.deadliner.platform.NotificationInteractionState { *; }
-keep class com.deadliner.platform.TaskNotificationResult { *; }
-keep enum com.deadliner.platform.NotificationKind { *; }
-keep enum com.deadliner.platform.NotificationPhase { *; }
-keep enum com.deadliner.platform.NotificationAction { *; }

# ONNX JNI uses Java constructors/members by name; required for minified Release.
# https://onnxruntime.ai/docs/build/android.html
-keep class ai.onnxruntime.** { *; }
