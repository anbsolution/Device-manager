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
    private const val MAX_ENVELOPE_LENGTH = 65536
    private const val MAX_FUTURE_SKEW_SECONDS = 300L

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

    fun isAppActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            "anb_license_store",
            Context.MODE_PRIVATE
        )
        val ownership = prefs.getString("ownership_certificate", null)
        val customer = prefs.getString("customer_license", null)

        return (ownership != null &&
            verify(context, ownership, "ownership").valid) ||
            (customer != null &&
            verify(context, customer, "customer").valid)
    }

    fun verify(
        context: Context,
        envelopeText: String,
        expectedType: String
    ): Result {
        return try {
            if (expectedType != "ownership" && expectedType != "customer") {
                return Result(false, "Unsupported expected licence type.")
            }

            if (envelopeText.isBlank() ||
                envelopeText.length > MAX_ENVELOPE_LENGTH
            ) {
                return Result(false, "Licence file is empty or too large.")
            }

            val envelope = JSONObject(envelopeText)
            val payloadEncoded = envelope.getString("payload")
            val signatureEncoded = envelope.getString("signature")

            if (payloadEncoded.isBlank() || signatureEncoded.isBlank()) {
                return Result(false, "Licence envelope is incomplete.")
            }

            val payloadBytes = Base64.decode(
                payloadEncoded,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val signatureBytes = Base64.decode(
                signatureEncoded,
                Base64.DEFAULT
            )

            if (payloadBytes.isEmpty() || payloadBytes.size > 49152) {
                return Result(false, "Invalid licence payload size.")
            }

            if (signatureBytes.size != 384) {
                return Result(false, "Invalid digital signature format.")
            }

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

            val payload = JSONObject(payloadBytes.toString(Charsets.UTF_8))

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

            val licenseId = payload.optString("licenseId")
            if (!Regex("^ANB-[0-9A-F]{16}$").matches(licenseId)) {
                return Result(false, "Invalid licence ID.")
            }

            val customerName = payload.optString("customerName").trim()
            if (customerName.isEmpty() || customerName.length > 200) {
                return Result(false, "Invalid customer name.")
            }

            val nowSeconds = System.currentTimeMillis() / 1000L
            val issuedAt = payload.optLong("issuedAt", -1L)

            if (issuedAt <= 0L) {
                return Result(false, "Invalid issue date.")
            }
            if (issuedAt > nowSeconds + MAX_FUTURE_SKEW_SECONDS) {
                return Result(false, "Licence issue date is in the future.")
            }

            val expiresAt = payload.optLong("expiresAt", -1L)
            if (expiresAt < 0L) {
                return Result(false, "Invalid expiry date.")
            }
            if (expiresAt > 0L && expiresAt <= issuedAt) {
                return Result(false, "Invalid licence validity period.")
            }
            if (expiresAt > 0L && nowSeconds > expiresAt) {
                return Result(false, "Licence has expired.")
            }

            val deviceId = payload.optString("deviceId")
            if (type == "ownership") {
                if (deviceId != "*") {
                    return Result(false, "Invalid ownership certificate scope.")
                }
            } else {
                if (deviceId.isBlank() || deviceId == "*") {
                    return Result(false, "Invalid customer device binding.")
                }

                val currentDeviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ) ?: return Result(false, "Device identifier unavailable.")

                if (deviceId != currentDeviceId) {
                    return Result(false, "Licence is for another device.")
                }
            }

            Result(
                true,
                if (expiresAt == 0L) "Valid • Lifetime"
                else "Valid • Time-based",
                licenseId,
                type
            )
        } catch (_: Exception) {
            Result(false, "Licence file is invalid or unreadable.")
        }
    }
}
