package com.oursky.authgear

import android.os.Handler
import android.os.Looper

/**
 * A wrapper class for a listener and handler pair.
 */
internal data class ListenerPair<T> @JvmOverloads constructor(
    val listener: T,
    val handler: Handler = Handler(Looper.getMainLooper())
)
