# Proguard / R8 Optimization Rules for HAVEN Family OS

# Keep Compose stable classes
-keepclassmembers class * {
    @androidx.compose.runtime.Stable <fields>;
    @androidx.compose.runtime.Immutable <fields>;
}

# Keep Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep Data Models
-keep class com.example.haven.data.model.** { *; }
-keep class com.example.haven.core.network.** { *; }

# Keep WorkManager & Workers
-keep class androidx.work.** { *; }
-keep class com.example.haven.data.sync.** { *; }

