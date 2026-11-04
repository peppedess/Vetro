package it.peppedess.vetro.notifications

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

@Immutable
data class NotifItem(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val clearable: Boolean,
    val autoCancel: Boolean,
    val intent: PendingIntent?,
)

/** Punto unico in cui il servizio pubblica le notifiche attive e la UI le legge. */
object NotificationHub {

    private val _items = MutableStateFlow<List<NotifItem>>(emptyList())
    val items: StateFlow<List<NotifItem>> = _items.asStateFlow()

    private var service: WeakReference<NotificationListenerService>? = null

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    internal fun attach(listener: NotificationListenerService) {
        service = WeakReference(listener)
    }

    internal fun detach() {
        service = null
    }

    internal fun publish(own: String, notifications: Array<StatusBarNotification>?) {
        _items.value = notifications.orEmpty()
            .filter { sbn ->
                val flags = sbn.notification.flags
                sbn.packageName != own &&
                    flags and Notification.FLAG_ONGOING_EVENT == 0 &&
                    flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
                    flags and Notification.FLAG_FOREGROUND_SERVICE == 0
            }
            .map { sbn ->
                val n = sbn.notification
                val extras = n.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
                    ?.toString().orEmpty()
                NotifItem(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = title,
                    text = text,
                    postTime = sbn.postTime,
                    clearable = sbn.isClearable,
                    autoCancel = n.flags and Notification.FLAG_AUTO_CANCEL != 0,
                    intent = n.contentIntent,
                )
            }
            .filter { it.title.isNotBlank() || it.text.isNotBlank() }
            .sortedByDescending { it.postTime }
    }

    fun dismiss(item: NotifItem) {
        runCatching { service?.get()?.cancelNotification(item.key) }
        _items.value = _items.value.filterNot { it.key == item.key }
    }

    fun dismissAll() {
        _items.value.filter { it.clearable }.forEach { dismiss(it) }
    }

    fun open(context: Context, item: NotifItem) {
        val pending = item.intent ?: return
        val options = if (Build.VERSION.SDK_INT >= 34) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
        } else {
            null
        }
        runCatching { pending.send(context, 0, null, null, null, null, options) }
        if (item.autoCancel) dismiss(item)
    }
}

class VetroNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationHub.attach(this)
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationHub.detach()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        runCatching { NotificationHub.publish(packageName, activeNotifications) }
    }
}
