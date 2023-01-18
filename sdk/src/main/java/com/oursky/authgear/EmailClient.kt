package com.oursky.authgear

data class EmailClient(val packageName: String) {
    companion object {
        @JvmField val GMAIL = EmailClient("com.google.android.gm")
        @JvmField val OUTLOOK = EmailClient("com.microsoft.office.outlook")
    }
}
