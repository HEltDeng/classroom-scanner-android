# Scanner intentionally keeps the network contract model names for Gson.
-keep class com.classroomassessment.scanner.core.network.** { *; }
-keep class com.classroomassessment.scanner.core.db.** { *; }
# ML Kit discovers BarcodeRegistrar through Firebase Components metadata and
# reflectively calls its public no-argument constructor. R8 9.x can otherwise
# remove that constructor from release builds, making BarcodeScanning.getClient
# fail before CameraX is bound.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**
