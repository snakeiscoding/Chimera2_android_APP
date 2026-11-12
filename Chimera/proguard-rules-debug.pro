# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html


# Generic android
-dontwarn android.test.**
-dontwarn org.junit.**
-dontwarn android.support.**
-dontpreverify # preverification not needed for dex compiler/Dalvik VM
-dontwarn javax.annotation.Nonnull
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.NotThreadSafe
-dontwarn org.slf4j.impl.StaticLoggerBinder
# Vortex display JAR classes
-dontwarn android.os.IRkDisplayDeviceManagementService$Stub
-dontwarn android.os.IRkDisplayDeviceManagementService
-dontwarn android.os.ServiceManager

# Obfuscation level settings
-repackageclasses '' # aggressive obfuscation
-allowaccessmodification # aggressive obfuscation
-optimizations !code/simplification/arithmetic #Dalvik 1.0 and 1.5 can not handle this simplification


# Uncomment this to preserve the line number information for
# debugging stack traces.
# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,SourceFile,LineNumberTable,*Annotation*

# Retain all android errorLogData attributes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
      public <init>(android.content.Context);
      public <init>(android.content.Context, android.util.AttributeSet);
      public <init>(android.content.Context, android.util.AttributeSet, int);
      public void set*(...);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Is this needed ??? We don'thave any of these classes.  Maybe third party libraries.
-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}

# Keep native calls
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep enumeration classes
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Needed for Parcelable/SafeParcelable Creators to not get stripped
-keepnames class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Handle Auto-Generate R classes and other resource files
-keepclassmembers class **.R$* {
    public static <fields>;
}
-adaptresourcefilenames    **.properties,**.gif,**.jpg
-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF

# Accomodate use of aws sdk for crash library
-keep class com.amazonaws.** { *; }
-keepnames class com.amazonaws.** { *; }

-dontwarn com.amazonaws.**
-dontwarn com.fasterxml.**

# Need all android.support.v4.view classes untouched
-keep class android.support.v4.view.** { *; }

# Make sure our javascript API functions do not change.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class org.videolan.libvlc.** { *; }

-keep class mf.org.apache.** { *; }
-keep class mf.javax.xml.** { *; }
-keep class javax.xml.** { *; }
-keep class org.videolan.libvlc.** { *; }
-keep class androidx.** { *; }
-keep class com.jht.** { *; }
