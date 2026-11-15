-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class app.macra.data.** {
    *** Companion;
}
-keepclasseswithmembers class app.macra.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ML Kit creates its component registrars by reflection. R8 full mode
# removes their constructors unless a rule keeps them.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
