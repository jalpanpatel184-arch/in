# Nova Assistant ProGuard Rules

# Keep all data models
-keep class com.nova.assistant.data.** { *; }
-keep class com.nova.assistant.domain.** { *; }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-keep class retrofit2.** { *; }
-keepattributes Signature, Exceptions

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class *

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.JsonDeserializer
-keep class * implements com.google.gson.JsonSerializer

# ML Kit
-keep class com.google.mlkit.** { *; }

# Keep accessibility service
-keep class com.nova.assistant.service.NovaAccessibilityService { *; }

# Keep Vosk
-keep class org.vosk.** { *; }
