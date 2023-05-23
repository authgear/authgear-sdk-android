package com.oursky.authgear.latte

import androidx.fragment.app.Fragment

interface LatteDelegate {
    @JvmDefault
    fun onTrackingEvent(event: LatteTrackingEvent) {}

    @JvmDefault
    fun onOpenEmailClient(source: Fragment) {
        val context = source.requireContext()
        val intent = EmailClient.makeEmailClientIntentChooser(
            context,
            "Choose Email Client",
            listOf(EmailClient.GMAIL, EmailClient.OUTLOOK)
        )
        context.startActivity(intent)
    }
}
