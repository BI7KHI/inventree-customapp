# Keep kotlinx.serialization and Retrofit signatures.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class xyz.bi7khi.inventree.mobile.data.api.** {
    *** Companion;
}
-keepclasseswithmembers class xyz.bi7khi.inventree.mobile.data.api.** {
    kotlinx.serialization.KSerializer serializer(...);
}
