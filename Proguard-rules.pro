# الحفاظ على جميع الكلاسات
-keep class com.eyepool.master.** { *; }

# الحفاظ على سمات Android
-keep class android.support.annotation.Keep
-keep @android.support.annotation.Keep class * {*;}