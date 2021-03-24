package com.oursky.authgear

import android.os.Build
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

internal data class DeviceInfoAndroid(
    val build: DeviceInfoBuild
)

internal fun DeviceInfoAndroid.toJsonObject(): JsonObject {
    return JsonObject(mutableMapOf<String, JsonElement>(
        "Build" to this.build.toJsonObject()
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

internal fun getDeviceInfo(): DeviceInfoRoot {
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
            )
        )
    )
}