# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep SQLCipher classes (required when minification is enabled)
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Keep Room entities
-keep class com.invisusnova.privadot.data.HistoryEntity { *; }

# Keep Accessibility Service metadata
-keep class com.invisusnova.privadot.service.PrivaDoTService { *; }
-keep class com.invisusnova.privadot.service.BootReceiver { *; }
