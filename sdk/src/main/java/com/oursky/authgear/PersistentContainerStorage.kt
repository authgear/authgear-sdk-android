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

internal class PersistentContainerStorage(val context: Context) : ContainerStorage {
    companion object {
        const val Verifier = "verifier"
        const val AnonymousKeyId = "anonymousKeyId"
        const val BiometricKeyId = "biometricKeyId"
        const val App2AppDeviceKeyId = "app2appDeviceKeyId"
        const val LOGTAG = "Authgear"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    override fun setOidcCodeVerifier(namespace: String, verifier: String) {
        this.setString(namespace, Verifier, verifier)
    }

    override fun getOidcCodeVerifier(namespace: String): String? {
        return this.getString(namespace, Verifier)
    }

    override fun getAnonymousKeyId(namespace: String): String? {
        return this.getString(namespace, AnonymousKeyId)
    }

    override fun setAnonymousKeyId(namespace: String, keyId: String) {
        this.setString(namespace, AnonymousKeyId, keyId)
    }

    override fun deleteAnonymousKeyId(namespace: String) {
        this.deleteString(namespace, AnonymousKeyId)
    }

    override fun getBiometricKeyId(namespace: String): String? {
        return this.getString(namespace, BiometricKeyId)
    }

    override fun setBiometricKeyId(namespace: String, keyId: String) {
        this.setString(namespace, BiometricKeyId, keyId)
    }

    override fun deleteBiometricKeyId(namespace: String) {
        this.deleteString(namespace, BiometricKeyId)
    }

    override fun getApp2AppDeviceKeyId(namespace: String): String? {
        return this.getString(namespace, App2AppDeviceKeyId)
    }
    override fun setApp2AppDeviceKeyId(namespace: String, keyId: String) {
        this.setString(namespace, App2AppDeviceKeyId, keyId)
    }
    override fun deleteApp2AppDeviceKeyId(namespace: String) {
        this.deleteString(namespace, App2AppDeviceKeyId)
    }

    private fun getString(namespace: String, key: String): String? {
        try {
            return getPref(namespace).getString(key, null)
        } catch (e: Exception) {
            val handled = this.handleBackupProblem(e, namespace)
            if (handled) {
                return this.getString(namespace, key)
            }
            throw e
        }
    }

    private fun setString(namespace: String, key: String, value: String) {
        try {
            getPref(namespace).edit().putString(key, value).commit()
        } catch (e: Exception) {
            val handled = this.handleBackupProblem(e, namespace)
            if (handled) {
                this.setString(namespace, key, value)
                return
            }
            throw e
        }
    }

    private fun deleteString(namespace: String, key: String) {
        try {
            getPref(namespace).edit().remove(key).commit()
        } catch (e: Exception) {
            val handled = this.handleBackupProblem(e, namespace)
            if (handled) {
                this.deleteString(namespace, key)
                return
            }
            throw e
        }
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
            Log.w(LOGTAG, "try to recover from backup problem in PersistentContainerStorage", e)
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