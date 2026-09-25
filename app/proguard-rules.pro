# =============================================================================
# Dalil Mit Ghamr — ProGuard / R8 Rules
# =============================================================================

# Preserve stack trace information for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signatures for Kotlin generics / Gson / Moshi reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# =============================================================================
# Firebase Firestore
# =============================================================================
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }
-keep class com.example.data.fcm.** { *; }

# Firebase App Check
-keep class com.google.firebase.appcheck.** { *; }

# =============================================================================
# Room Database
# =============================================================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.**

# =============================================================================
# Moshi Kotlin (JSON serialization)
# =============================================================================
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * { *; }
# Kotlin metadata needed by Moshi
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# =============================================================================
# App Data Models (used by Moshi + Room)
# =============================================================================
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.remote.** { *; }

# =============================================================================
# Jetpack Compose
# =============================================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# =============================================================================
# Coil (Image Loading)
# =============================================================================
-keep class coil.** { *; }
-dontwarn coil.**

# =============================================================================
# OkHttp / Retrofit
# =============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# =============================================================================
# Kotlin Coroutines
# =============================================================================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# =============================================================================
# Google Credential Manager / Identity
# =============================================================================
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

# =============================================================================
# Generative AI (Gemini)
# =============================================================================
-keep class com.google.ai.** { *; }
-dontwarn com.google.ai.**

# =============================================================================
# Enums (Room stores enum names as strings)
# =============================================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
