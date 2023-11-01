package com.oursky.authgear.latte

import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.content.pm.PackageManager

data class EmailClient(val packageName: String) {
    companion object {
        @JvmField
        val GMAIL = EmailClient("com.google.android.gm")
        @JvmField
        val OUTLOOK = EmailClient("com.microsoft.office.outlook")

        @JvmStatic
        fun makeEmailClientIntentChooser(
            ctx: Context,
            title: String,
            clients: List<EmailClient>
        ): Intent? {
            val launchIntents: MutableList<Intent> = mutableListOf()
            val pm: PackageManager = ctx.packageManager
            for (client in clients) {
                val intent = pm.getLaunchIntentForPackage(client.packageName) ?: continue
                val info = pm.resolveActivity(intent, 0) ?: continue
                launchIntents.add(
                    LabeledIntent(
                        intent,
                        client.packageName,
                        info.loadLabel(pm),
                        info.icon
                    )
                )
            }
            if (launchIntents.size == 0) {
                return null
            }
            val firstIntent = launchIntents[0]
            val chooser = Intent.createChooser(firstIntent, title)
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, launchIntents.toTypedArray())
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return chooser
        }
    }
}
