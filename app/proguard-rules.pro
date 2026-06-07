-keep class com.darko.speleov1.model.** { *; }
-keep class com.darko.speleov1.AppCoreModels { *; }
-keep class com.darko.speleov1.AppCoreModels$* { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.darko.speleov1.util.DriveDrawing* { *; }
-keep class com.darko.speleov1.util.DriveDrawingsRepository { *; }
