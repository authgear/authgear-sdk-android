package com.oursky.authgear.latte

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.oursky.authgear.latte.fragment.LatteFragment
import com.oursky.authgear.latte.fragment.LatteFragment.Companion.KEY_RESULT
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith
import java.io.Serializable

class LatteHandle<T> internal constructor(fragment: LatteFragment<T>) : Serializable {
    private val latteFragment = fragment

    val id = fragment.latteID
    val fragment: Fragment
        get() = latteFragment

    private val result = CompletableDeferred<T>()

    suspend fun await(manager: FragmentManager): T {
        manager.setFragmentResultListener(id, fragment) { _, bundle ->
            val output = bundle.getSerializable(KEY_RESULT) as? Result<T>
            if (output != null) {
                result.completeWith(output)
            }
        }
        return result.await()
    }
}
