package com.tailscale.ipn.ui.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getString
import com.tailscale.ipn.App
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.model.Ipn
import com.tailscale.ipn.ui.notifier.Notifier
import com.tailscale.ipn.ui.viewModel.AdvertisedRoutesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// NotificationsManager observes the state of the tunnel and the preferences
// to deliver or cancel system-wide notifications.
class NotificationsManager(notifier: Notifier) {

  private val RUN_AS_EXIT_NODE_NOTIFICATION_CHANNEL = "RunAsExitNode"
  private val RUN_AS_EXIT_NODE_NOTIFICATION_ID = 201

  private val NEEDS_APPROVAL_NOTIFICATION_CHANNEL = "NeedsMachineAuth"
  private val NEEDS_APPROVAL_NOTIFICATION_ID = 202

  private val notificationManager: NotificationManager =
      App.getApplication().getSystemService(NOTIFICATION_SERVICE) as NotificationManager

  init {
    CoroutineScope(Dispatchers.Default).launch {
      notifier.prefs
          .combine(notifier.state) { prefs, state -> Pair(prefs, state) }
          .collect { (prefs, state) ->
            prefs?.let { notifyWithPrefs(it) }
            notifyWithState(state)
          }
    }
  }

  private fun notifyWithPrefs(prefs: Ipn.Prefs) {
    notifyRunningAsExitNode(AdvertisedRoutesHelper.exitNodeOnFromPrefs(prefs))
  }

  private fun notifyWithState(state: Ipn.State) {
    notifyNeedsMachineAuth(state == Ipn.State.NeedsMachineAuth)
  }

  private fun notifyRunningAsExitNode(isOn: Boolean) {
    ensureChannelRegistered(
        id = RUN_AS_EXIT_NODE_NOTIFICATION_CHANNEL,
        name = R.string.running_as_exit_node,
        description =
            R.string
                .other_devices_can_access_the_internet_using_the_ip_address_of_this_device_you_can_turn_this_off_in_the_tailscale_app)

    if (isOn) {
      deliverNotification(
          id = RUN_AS_EXIT_NODE_NOTIFICATION_ID,
          channel = RUN_AS_EXIT_NODE_NOTIFICATION_CHANNEL,
          title = R.string.running_as_exit_node,
          text =
              R.string
                  .other_devices_can_access_the_internet_using_the_ip_address_of_this_device_you_can_turn_this_off_in_the_tailscale_app,
          isOngoing = true,
          isSilent = true)
    } else {
      cancelNotifications(RUN_AS_EXIT_NODE_NOTIFICATION_ID)
    }
  }

  private fun notifyNeedsMachineAuth(needsAuth: Boolean) {
    ensureChannelRegistered(
        id = NEEDS_APPROVAL_NOTIFICATION_CHANNEL,
        name = R.string.awaiting_approval,
        description = R.string.notifications_to_inform_the_user_when_device_approval_is_needed)

    if (needsAuth) {
      deliverNotification(
          id = NEEDS_APPROVAL_NOTIFICATION_ID,
          channel = NEEDS_APPROVAL_NOTIFICATION_CHANNEL,
          title = R.string.awaiting_approval,
          text =
              R.string
                  .this_device_must_be_approved_in_the_tailscale_admin_console_before_it_can_connect)
    } else {
      cancelNotifications(id = NEEDS_APPROVAL_NOTIFICATION_ID)
    }
  }

  private fun deliverNotification(
      id: Int,
      channel: String,
      @StringRes title: Int,
      @StringRes text: Int,
      isOngoing: Boolean = false,
      isSilent: Boolean = false
  ) {
    val builder =
        NotificationCompat.Builder(App.getApplication().applicationContext, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(App.getApplication().applicationContext, title))
            .setOngoing(isOngoing)
            .setContentText(getString(App.getApplication().applicationContext, text))
            .setSilent(isSilent)
    val notification = builder.build()
    if (ActivityCompat.checkSelfPermission(
        App.getApplication().applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED) {
      Log.d("PersistentNotifications", "Missing permission to deliver notifications")
    }
    notificationManager.notify(id, notification)
  }

  private fun cancelNotifications(id: Int) {
    notificationManager.cancel(id)
  }

  private fun ensureChannelRegistered(
      id: String,
      @StringRes name: Int,
      @StringRes description: Int
  ) {
    val nameText = getString(App.getApplication().applicationContext, name)
    val descriptionText = getString(App.getApplication().applicationContext, description)
    val channel =
        NotificationChannel(id, nameText, NotificationManager.IMPORTANCE_DEFAULT).apply {
          this.description = descriptionText
        }
    notificationManager.createNotificationChannel(channel)
  }
}
