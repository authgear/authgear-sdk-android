package com.oursky.authgear.latte

import android.content.Context
import android.content.Intent

interface LatteDelegate {
    @JvmDefault
    fun onTrackingEvent(event: LatteTrackingEvent) {}

    @JvmDefault
    fun onOpenEmailClient(context: Context?) {
        context ?: return
        val intent = EmailClient.makeEmailClientIntentChooser(
            context,
            "Choose Email Client",
            listOf(EmailClient.GMAIL, EmailClient.OUTLOOK)
        )
        context.startActivity(intent)
    }

    @JvmDefault
    fun onOpenSMSClient(context: Context?) {
        context ?: return
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_MESSAGING)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
