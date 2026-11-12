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
-dontwarn javax.annotation.Nonnull
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.NotThreadSafe
-dontwarn org.slf4j.impl.StaticLoggerBinder
# Vortex display JAR classes
-dontwarn android.os.IRkDisplayDeviceManagementService$Stub
-dontwarn android.os.IRkDisplayDeviceManagementService
-dontwarn android.os.ServiceManager

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

# obfuscating awssdk seem to cause a hard crash
-keep class software.amazon.awssdk.** { *; }

-keep class com.jht.chimera.service.hal.lib.hardware.Driver* { *; }

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
# NOTE: do NOT adapt **.properties: commonmark (used by compose-markdown/Markwon for the
# legal documents) loads /org/commonmark/internal/util/entities.properties via a hardcoded
# path in a static initializer. With -repackageclasses, adapting moves the file out of that
# path, getResourceAsStream() returns null, and the app crashes with
# ExceptionInInitializerError the first time a document containing an HTML entity
# (e.g. &quot; in the Arabic legal docs) is parsed.
-adaptresourcefilenames    **.gif,**.jpg
-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF

# Accomodate use of aws sdk for crash library
# Removed the -keepnames on this pattern; -keep already prevents renaming.
-keep class com.amazonaws.** { *; }

-dontwarn com.amazonaws.**
-dontwarn com.fasterxml.**

# Removed -keep class android.support.v4.view.** { *; }; it matched 0 entries in seeds.txt.

# Keep Options
-keep class com.jht.serialport.** { *; }
-keep class com.jht.serialcommunication.** { *; }
-keep class com.jht.peripheral.** { *; }

-assumenosideeffects class android.util.Log {
  public static *** v(...);
  public static *** i(...);
}

# Keep data matrix keys as the enum names are the keys.
# Added the missing dot; the undotted form also matched siblings like datamatrixlibExtra.
-keep class com.jht.datamatrixlib.** { *; }

# Make sure our javascript API functions do not change.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.jht.chimera.lib.common.Audio* { *; }

-keep class com.jht.chimera.drivers.audio.Audio* { *; }

-keep class org.videolan.libvlc.** { *; }

-keep class mf.org.apache.** { *; }
-keep class mf.javax.xml.** { *; }
-keep class javax.xml.** { *; }

# Added the missing dot, as above.
-keep class com.jht.virtualactive.** { *; }

-keep class com.jht.chimera.lib.common.analytic.AnalyticStats {<fields>;}
-keep class com.jht.chimera.lib.common.analytic.AnalyticsMachineInfo {<fields>;}
-keep class com.jht.chimera.service.logger.PerformanceMetricsManager$PerformanceMetrics {<fields>;}
-keep class com.jht.chimera.lib.common.stats.RunningAverageDuration {<fields>;}
# Removed -keep class androidx.** { *; }, 55.9% of all seed entries; aapt_rules.txt and the
# artifacts' own consumer rules already cover it. Concierge keeps removed, those models moved.
