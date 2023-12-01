package com.oursky.authgear.latte

import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.content.pm.PackageManager
import android.net.Uri

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
            val mailtoIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            val packageInfos = pm.queryIntentActivities(mailtoIntent, PackageManager.GET_RESOLVED_FILTER)
            // We need linked set to preserve the insertion order
            val packageNames = linkedSetOf<String>()
            for (client in clients) {
                packageNames.add(client.packageName)
            }
            for (info in packageInfos) {
                packageNames.add(info.activityInfo.packageName)
            }
            for (packageName in packageNames) {
                val intent = pm.getLaunchIntentForPackage(packageName) ?: continue
                val info = pm.resolveActivity(intent, 0) ?: continue
                launchIntents.add(
                    LabeledIntent(
                        intent,
                        packageName,
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
            var extraIntents = mutableListOf<Intent>()
            if (launchIntents.size > 1) {
                extraIntents = launchIntents.subList(1, launchIntents.size)
            }
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return chooser
        }
    }
}