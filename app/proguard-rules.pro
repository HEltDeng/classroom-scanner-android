# Scanner intentionally keeps the network contract model names for Gson.
-keep class com.classroomassessment.scanner.core.network.** { *; }
-keep class com.classroomassessment.scanner.core.db.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
