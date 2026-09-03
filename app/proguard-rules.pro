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

-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes SourceFile,LineNumberTable

-keep class com.hexis.bi.data.user.UserProfile { *; }
-keep class com.hexis.bi.data.user.UserSettings { *; }
-keep class com.hexis.bi.data.healthconnections.HealthConnection { *; }

-keep class co.tryterra.** { *; }
-dontwarn co.tryterra.**

-keep class com.look.** { *; }
-dontwarn com.look.**

-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

-keepclassmembers enum * { *; }

-keepnames class * extends java.lang.Throwable
