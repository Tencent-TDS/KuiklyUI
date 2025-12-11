-keep class com.tencent.kuikly.core.android.KuiklyCoreEntry { *; }
-keep class com.tencent.kuikly.core.IKuiklyCoreEntry { *; }
-keep class com.tencent.kuikly.core.IKuiklyCoreEntry$Delegate { *; }
-keep class com.tencent.kuikly.core.log.KLog { *; }

# Keep RecyclerView.setScrollState method for reflection access
-keepclassmembers class androidx.recyclerview.widget.RecyclerView {
    void setScrollState(int);
}