package com.anb.devicemanager.license

import android.content.Context
import android.provider.Settings
import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object LicenseManager {
    private const val ISSUER = "ANB Solution"
    private const val PRODUCT = "ANB Device Manager"

    private const val PUBLIC_KEY_PEM = """
-----BEGIN PUBLIC KEY-----
MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAkfg/GU9Rzz58yzGS2ccQ
sYChaLzq9S8ROKfYfNKnsJBDDPFLi6t2FQKk+6mxzQdIL9w0kWg8IfiA3+oC7Mi3
DuneuUnNSWRXRG2z4TzsLy1AtgrLiApiA9hz6ThRYWyCSh00Q34IapfpHoqO+880
FGdCCMaEMs/SnnzqpJMnmrMwBA75rXX91/SDPOXDN4qb4xCOxZhrysj19pE81jXo
+URgb3AHUyD5n89Ha+bmByZgYcLt9GoRphPCsucAMjvcV8pxXt2+3wV/dD6pszff
GsRrVVxlnXuNmYGlEUplZShQZ7aQCzfo/TAx18O8C4YSRkxPlUdAlYn1SOER63hE
a39oNPdSTd0XHZHIUWFm+qamkx7mOoWkx+dCJaK2BYkK9QuafbgUNXF1aBCOVMMj
RpZxaB48TIptFz+IHCm9hKn+6Z5uz+1rF649ppp7My/tY10ivDU0JEHDFv7DCJ5V
AiZwCcTy+AZz0RjzlL/DoIXqhYK5W1nTKH92AqY9v5uBAgMBAAE=
-----END PUBLIC KEY-----
"""

    data class Result(
        val valid: Boolean,
        val message: String,
        val licenseId: String = "",
        val licenseType: String = ""
    )

    fun verify(
        context: Context,
        envelopeText: String,
        expectedType: String
    ): Result {
        return try {
            val envelope = JSONObject(envelopeText)
            val payloadEncoded = envelope.getString("payload")
            val signatureEncoded = envelope.getString("signature")

            val payloadBytes = Base64.decode(
                payloadEncoded,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val signatureBytes = Base64.decode(
                signatureEncoded,
                Base64.DEFAULT
            )

            val pemBody = PUBLIC_KEY_PEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .filterNot { it.isWhitespace() }

            val keyBytes = Base64.decode(pemBody, Base64.DEFAULT)
            val publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(keyBytes))

            val verifier = Signature.getInstance("SHA256withRSA")
            verifier.initVerify(publicKey)
            verifier.update(payloadBytes)

            if (!verifier.verify(signatureBytes)) {
                return Result(false, "Invalid digital signature.")
            }

            val payload = JSONObject(
                payloadBytes.toString(Charsets.UTF_8)
            )

            if (payload.optString("issuer") != ISSUER) {
                return Result(false, "Unknown licence issuer.")
            }
            if (payload.optString("product") != PRODUCT) {
                return Result(false, "Licence belongs to another product.")
            }
            if (payload.optString("packageName") != context.packageName) {
                return Result(false, "Package ID does not match.")
            }

            val type = payload.optString("type")
            if (type != expectedType) {
                return Result(false, "Unexpected licence type.")
            }

            if (type == "ownership") {
                if (payload.optString("deviceId") != "*") {
                    return Result(false, "Invalid ownership certificate scope.")
                }
            } else if (type == "customer") {
                val currentDeviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ) ?: return Result(false, "Device identifier unavailable.")

                if (payload.optString("deviceId") != currentDeviceId) {
                    return Result(false, "Licence is for another device.")
                }
            } else {
                return Result(false, "Unsupported licence type.")
            }

            val expiresAt = payload.optLong("expiresAt", 0L)
            if (expiresAt > 0L && System.currentTimeMillis() > expiresAt) {
                return Result(false, "Licence has expired.")
            }

            Result(
                true,
                if (expiresAt == 0L) "Valid • Lifetime"
                else "Valid • Time-based",
                payload.optString("licenseId"),
                type
            )
        } catch (_: Exception) {
            Result(false, "Licence file is invalid or unreadable.")
        }
    }
}
