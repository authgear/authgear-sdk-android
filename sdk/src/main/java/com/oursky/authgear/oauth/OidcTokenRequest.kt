package com.oursky.authgear.oauth

import com.oursky.authgear.ActorTokenType
import com.oursky.authgear.GrantType
import com.oursky.authgear.RequestedTokenType
import com.oursky.authgear.SettingsAction
import com.oursky.authgear.SubjectTokenType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OidcTokenRequest(
    @SerialName("grant_type")
    val grantType: GrantType,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("x_device_info")
    val xDeviceInfo: String? = null,
    @SerialName("redirect_uri")
    val redirectUri: String? = null,
    val code: String? = null,
    @SerialName("code_verifier")
    val codeVerifier: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("jwt")
    val jwt: String? = null,
    @SerialName("x_app2app_device_key_jwt")
    val xApp2AppDeviceKeyJwt: String? = null,
    @SerialName("code_challenge")
    val codeChallenge: String? = null,
    @SerialName("code_challenge_method")
    val codeChallengeMethod: String? = null,
    @SerialName("x_settings_action")
    val settingsAction: SettingsAction? = null,
    @SerialName("device_secret")
    val deviceSecret: String? = null,
    @SerialName("requested_token_type")
    val requestedTokenType: RequestedTokenType? = null,
    @SerialName("audience")
    val audience: String? = null,
    @SerialName("subject_token_type")
    val subjectTokenType: SubjectTokenType? = null,
    @SerialName("subject_token")
    val subjectToken: String? = null,
    @SerialName("actor_token_type")
    val actorTokenType: ActorTokenType? = null,
    @SerialName("actor_token")
    val actorToken: String? = null,
    @SerialName("scope")
    val scope: List<String>? = null
)
