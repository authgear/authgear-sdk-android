package com.oursky.authgear.latte

import android.content.Context

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
}
