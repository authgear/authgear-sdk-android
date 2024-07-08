package com.oursky.authgear

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.shaded.protobuf.InvalidProtocolBufferException
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

class PersistentInterAppSharedStorage(val context: Context) : InterAppSharedStorage {
    companion object {
        private const val LOGTAG = "Authgear"
        private const val IDToken = "idToken"
        private const val DeviceSecret = "deviceSecret"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private fun setItem(namespace: String, key: String, value: String) {
        try {
            getPref(namespace).edit().putString(key, value).commit()
        } catch (e: Exception) {
            val handled = this.handleBackupProblem(e, namespace)
            if (handled) {
                this.setItem(namespace, key, value)
                return
            }
            throw e
        }
    }

    private fun getItem(namespace: String, key: String): String? {
        try {
            return getPref(namespace).getString(key, null)
        } catch (e: Exception) {
            val handled = this.handleBackupProblem(e, namespace)
            if (handled) {
                return this.getItem(namespace, key)
            }
            throw e
        }
    }

    private fun deleteItem(namespace: String, key: String) {
        try {
            getPref(namespace).edit().remove(key).commit()
        } catch (e: Exception) {
            val handled = this.handleBackupProblem(e, namespace)
            if (handled) {
                this.deleteItem(namespace, key)
                return
            }
            throw e
        }
    }

    override fun setIDToken(namespace: String, idToken: String) {
        setItem(namespace, IDToken, idToken)
    }

    override fun getIDToken(namespace: String): String? {
        return getItem(namespace, IDToken)
    }

    override fun deleteIDToken(namespace: String) {
        deleteItem(namespace, IDToken)
    }

    override fun setDeviceSecret(namespace: String, deviceSecret: String) {
        setItem(namespace, DeviceSecret, deviceSecret)
    }

    override fun getDeviceSecret(namespace: String): String? {
        return getItem(namespace, DeviceSecret)
    }

    override fun deleteDeviceSecret(namespace: String) {
        deleteItem(namespace, DeviceSecret)
    }

    private fun handleBackupProblem(e: Exception, namespace: String): Boolean {
        // NOTE(backup): Explanation on the backup problem.
        // EncryptedSharedPreferences depends on a master key stored in AndroidKeyStore.
        // The master key is not backed up.
        // However, the EncryptedSharedPreferences is backed up.
        // When the app is re-installed, and restored from a backup.
        // A new master key is created, but it cannot decrypt the restored EncryptedSharedPreferences.
        // This problem is persistence until the EncryptedSharedPreferences is deleted.
        //
        // The official documentation of EncryptedSharedPreferences tell us to
        // exclude the EncryptedSharedPreferences from a backup.
        // But defining a backup rule is not very appropriate in a SDK.
        // So we try to fix this in our code instead.
        //
        // This fix is tested against security-crypto@1.1.0-alpha06 and tink-android@1.8.0
        // Upgrading to newer versions may result in the library throwing a different exception that we fail to catch,
        // making this fix buggy.
        //
        // To reproduce the problem, you have to follow the steps here https://developer.android.com/identity/data/testingbackup#TestingBackup
        // The example app has been configured to back up the EncryptedSharedPreferences and nothing else.
        // One reason is to reproduce the problem, and another reason is that some platform, some Flutter,
        // store large files in the data directory. That will prevent the backup from working.
        //
        // The fix is to observe what exception was thrown by the underlying library
        // when the problem was re-produced.
        // When we catch the exception, we delete the EncryptedSharedPreferences and re-create it.
        //
        // Some references on how other fixed the problem.
        // https://github.com/stytchauth/stytch-android/blob/0.23.0/0.1.0/sdk/src/main/java/com/stytch/sdk/common/EncryptionManager.kt#L50
        if (e is InvalidProtocolBufferException || e is GeneralSecurityException || e is IOException) {
            Log.w(LOGTAG, "try to recover from backup problem in PersistentTokenStorage", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(namespace)
            } else {
                context.getSharedPreferences(namespace, Context.MODE_PRIVATE).edit().clear().apply()
                val dir = File(context.applicationInfo.dataDir, "shared_prefs")
                File(dir, "$namespace.xml").delete()
            }
            return true
        }
        return false
    }

    private fun getPref(namespace: String): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context, namespace, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}