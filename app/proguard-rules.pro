# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
# -keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
# }

# Uncomment this to preserve the line number information for
# debugging stack traces.
# -keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
# -renamesourcefileattribute SourceFile

# --- Keep rules for production build (R8) ---

# Retrofit + OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Moshi (JSON serialization)
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keepclassmembers class * {
    @com.squareup.moshi.** <fields>;
}

# Room (database entities & DAOs)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Dao class * { *; }

# Gemini API / Retrofit models
-keep class com.example.api.** { *; }
-keepclassmembers class com.example.api.** {
    @com.squareup.moshi.* <fields>;
}

# Keep all public API classes (for reflection)
-keep public class * {
    public protected *;
}

# Allow R8 to optimize more aggressively
-dontwarn okio.**
-dontwarn kotlinx.coroutines.**