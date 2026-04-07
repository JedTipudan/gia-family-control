# ── Keep all app models ──
-keep class com.gia.parentcontrol.model.** { *; }
-keep class com.gia.parentcontrol.network.** { *; }

# ── Retrofit ──
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Gson ──
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# ── OkHttp ──
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# ── Google Maps ──
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.**

# ── Firebase ──
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── ViewBinding ──
-keep class com.gia.parentcontrol.databinding.** { *; }

# ── Kotlin coroutines ──
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Kotlin ──
-keep class kotlin.** { *; }
-dontwarn kotlin.**
