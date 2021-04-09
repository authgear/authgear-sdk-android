package com.oursky.authgear

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal data class DeviceInfoRoot(
    val android: DeviceInfoAndroid
)

internal fun DeviceInfoRoot.toJsonObject(): JsonObject {
    return JsonObject(mutableMapOf<String, JsonElement>(
        "android" to this.android.toJsonObject()
    ))
}

internal fun DeviceInfoRoot.toBase64URLEncodedString(): String {
    return base64UrlEncode(Json.encodeToString(this.toJsonObject()).toUTF8())
}

internal data class DeviceInfoAndroid(
    val build: DeviceInfoBuild,
    val packageInfo: DeviceInfoPackageInfo,
    val settings: DeviceInfoSettings
)

internal fun DeviceInfoAndroid.toJsonObject(): JsonObject {
    return JsonObject(mutableMapOf<String, JsonElement>(
        "Build" to this.build.toJsonObject(),
        "PackageInfo" to this.packageInfo.toJsonObject(),
        "Settings" to this.settings.toJsonObject()
    ))
}

internal data class DeviceInfoBuild(
    val board: String,
    val brand: String,
    val model: String,
    val device: String,
    val display: String,
    val hardware: String,
    val manufacturer: String,
    val product: String
)

internal fun DeviceInfoBuild.toJsonObject(): JsonObject {
    return JsonObject(mutableMapOf<String, JsonElement>(
        "BOARD" to JsonPrimitive(this.board),
        "BRAND" to JsonPrimitive(this.brand),
        "MODEL" to JsonPrimitive(this.model),
        "DEVICE" to JsonPrimitive(this.device),
        "DISPLAY" to JsonPrimitive(this.display),
        "HARDWARE" to JsonPrimitive(this.hardware),
        "MANUFACTURER" to JsonPrimitive(this.manufacturer),
        "PRODUCT" to JsonPrimitive(this.product)
    ))
}

internal data class DeviceInfoPackageInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: String,
    val longVersionCode: String
)

internal fun DeviceInfoPackageInfo.toJsonObject(): JsonObject {
    return JsonObject(mutableMapOf<String, JsonElement>(
        "packageName" to JsonPrimitive(this.packageName),
        "versionName" to JsonPrimitive(this.versionName),
        "versionCode" to JsonPrimitive(this.versionCode),
        "longVersionCode" to JsonPrimitive(this.longVersionCode)
    ))
}

internal data class DeviceInfoSettings(
    val secure: DeviceInfoSettingsSecure,
    val global: DeviceInfoSettingsGlobal
)

internal fun DeviceInfoSettings.toJsonObject(): JsonObject {
    return JsonObject(mutableMapOf<String, JsonElement>(
        "Secure" to this.secure.toJsonObject(),
        "Global" to this.global.toJsonObject()
    ))
}

internal data class DeviceInfoSettingsSecure(
    val bluetoothName: String
)

internal fun DeviceInfoSettingsSecure.toJsonObject(): JsonObject {
    return JsonObject(mutableMapOf<String, JsonElement>(
        "bluetooth_name" to JsonPrimitive(this.bluetoothName)
    ))
}

internal data class DeviceInfoSettingsGlobal(
    val deviceName: String
)

internal fun DeviceInfoSettingsGlobal.toJsonObject(): JsonObject {
    return JsonObject(mutableMapOf<String, JsonElement>(
        "DEVICE_NAME" to JsonPrimitive(this.deviceName)
    ))
}

internal fun getDeviceInfo(context: Context): DeviceInfoRoot {
    val packageName = context.applicationContext.packageName
    val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
    val versionName = packageInfo.versionName
    val versionCode = packageInfo.versionCode.toString()
    var longVersionCode = ""
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode = packageInfo.longVersionCode.toString()
    }

    val bluetoothName = Settings.Secure.getString(context.applicationContext.contentResolver, "bluetooth_name") ?: ""
    var deviceName = ""
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        deviceName = Settings.Global.getString(context.applicationContext.contentResolver, Settings.Global.DEVICE_NAME) ?: ""
    }

    return DeviceInfoRoot(
        android = DeviceInfoAndroid(
            build = DeviceInfoBuild(
                board = Build.BOARD,
                brand = Build.BRAND,
                model = Build.MODEL,
                device = Build.DEVICE,
                display = Build.DISPLAY,
                hardware = Build.HARDWARE,
                manufacturer = Build.MANUFACTURER,
                product = Build.PRODUCT
            ),
            packageInfo = DeviceInfoPackageInfo(
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                longVersionCode = longVersionCode
            ),
            settings = DeviceInfoSettings(
                secure = DeviceInfoSettingsSecure(
                    bluetoothName = bluetoothName
                ),
                global = DeviceInfoSettingsGlobal(
                    deviceName = deviceName
                )
            )
        )
    )
}