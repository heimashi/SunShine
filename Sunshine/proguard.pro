# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.content.ContentProvider
-keep public class  android.media.ThumbnailUtils
-keep public class  android.os.SystemProperties
-keep public class  android.hardware.Camera
-keep public class  android.provider.Settings
-keep public class  android.provider.Settings.System
-keep public class  android.media.AudioManager
