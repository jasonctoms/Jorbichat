package com.jorbital.jorbichat

import android.util.Log

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class JorbichatFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "JorbichatFMService"
        private const val JORBICHAT_ENGAGE_TOPIC = "jorbichat_engage"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // Handle data payload of FCM messages.
        Log.d(TAG, "FCM Message Id: " + remoteMessage!!.messageId!!)
        Log.d(TAG, "FCM Notification Message: " + remoteMessage.notification!!)
        Log.d(TAG, "FCM Data Message: " + remoteMessage.data)
    }

    override fun onNewToken(s: String?) {
        super.onNewToken(s)
        // If you need to handle the generation of a token, initially or
        // after a refresh this is where you should do that.
        Log.d(TAG, "FCM Token: " + s!!)

        // Once a token is generated, we subscribe to topic.
        FirebaseMessaging.getInstance()
                .subscribeToTopic(JORBICHAT_ENGAGE_TOPIC)
    }
}
