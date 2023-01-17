package com.oursky.authgear

data class EmailClient(val packageName: String) {
    companion object {
        @JvmStatic val gmail = EmailClient("com.google.android.gm")
        @JvmStatic val outlook = EmailClient("com.microsoft.office.outlook")
    }
}
