package tw.dynity.wrongbutton.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.system.ExternalPress

/**
 * 桌面小工具：一顆 The Button。點它會打開遊戲，並記下一次「從桌面按下」（第 9 章〈放在桌面〉）。
 * 不定時更新、不讀任何資料、不需要任何權限。
 */
class ButtonWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val intent = Intent(Intent.ACTION_VIEW, ExternalPress.uri(ExternalPress.FROM_WIDGET), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val views = RemoteViews(context.packageName, R.layout.widget_button).apply {
            setOnClickPendingIntent(R.id.widget_root, pending)
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}
