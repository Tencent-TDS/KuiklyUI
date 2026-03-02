-keep class com.tencent.kuikly.core.android.KuiklyCoreEntry { *; }
-keep class com.tencent.kuikly.core.IKuiklyCoreEntry { *; }
-keep class com.tencent.kuikly.core.IKuiklyCoreEntry$Delegate { *; }
-keep class com.tencent.kuikly.core.log.KLog { *; }

-keep class com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreContextScheduler {
    com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreContextScheduler INSTANCE;
    void scheduleTask(long,java.lang.Runnable);
}

# Keep RecyclerView.setScrollState method for reflection access
-keepclassmembers class androidx.recyclerview.widget.RecyclerView {
    void setScrollState(int);
}