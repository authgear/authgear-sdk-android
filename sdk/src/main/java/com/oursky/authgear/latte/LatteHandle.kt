package com.oursky.authgear.latte

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.oursky.authgear.latte.fragment.LatteFragment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith

class LatteHandle<T> internal constructor(fragment: LatteFragment<T>) {
    private val latteFragment = fragment

    val id = fragment.latteID
    val fragment: Fragment
        get() = latteFragment

    private val result = CompletableDeferred<T>()

    suspend fun await(manager: FragmentManager): T {
        manager.setFragmentResultListener(id, fragment) { requestKey, bundle ->
            if (requestKey == "result") {
                val output = bundle.getSerializable(requestKey) as? Result<T>
                if (output != null) {
                    result.completeWith(output)
                }
            }
        }
        return result.await()
    }
}
