# Proguard rules for FireCalc (.aab & .apk)
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

-keep class com.firecalc.tablet.** { *; }
