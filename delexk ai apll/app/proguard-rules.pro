-keep class dev.rikka.shizuku.** { *; }
-keep interface dev.rikka.shizuku.** { *; }

-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

-keep class org.opencv.** { *; }

-dontwarn org.opencv.**
-dontwarn dev.rikka.shizuku.**
-dontwarn com.google.mlkit.**

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

-renamesourcefileattribute SourceFile
