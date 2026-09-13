# kotlinx.serialization: сохраняем сгенерированные сериализаторы.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$* *;
}
-keepclassmembers class **$* implements kotlinx.serialization.internal.GeneratedSerializer {
    *** serializer(...);
}

# Всё, что система и Glance поднимают по имени класса, а не по ссылке.
# Без этих правил R8 переименует классы, а строковые имена в RemoteViews
# и в базе WorkManager останутся старыми — кнопки виджета молча перестанут
# работать, причём только в release-сборке.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
