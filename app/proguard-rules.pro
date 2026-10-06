-keep class io.github.sunilxsk.lockperm.XposedModuleEntry { *; }
-keep class * extends io.github.libxposed.api.XposedModule { *; }

-keepclassmembers class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

-keep class io.github.sunilxsk.lockperm.NativeBridge { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

-adaptresourcefilenames META-INF/xposed/native_init.list

-keep class io.github.libxposed.service.** { *; }
-keep class io.github.libxposed.api.** { *; }

-keep class android.webkit.WebView { *; }
-keep class android.webkit.WebViewClient { *; }
-keep class android.provider.Settings$Secure { *; }

-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.**

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod