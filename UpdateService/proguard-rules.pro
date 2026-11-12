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


# Obfuscation level settings
# Removed -dontpreverify and -optimizations; R8 ignores both.
-repackageclasses '' # aggressive obfuscation
-allowaccessmodification # aggressive obfuscation


# Uncomment this to preserve the line number information for
# debugging stack traces.
# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,SourceFile,LineNumberTable,*Annotation*

# Retain all android base attributes
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

-assumenosideeffects class android.util.Log {
  public static *** v(...);
  public static *** i(...);
}

# Keep data matrix keys as the enum names are the keys.
-keep class com.jht.datamatrixlib.** { *; }

# Make sure our javascript API functions do not change.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.jht.chimera.lib.common.Audio* { *; }
-keep class android.os.UpdateEngineCallback { *; }

# Keep Options
-keep class com.jht.serialport.** { *; }
-keep class com.jht.serialcommunication.** { *; }
-keep class com.jht.peripheral.** { *; }