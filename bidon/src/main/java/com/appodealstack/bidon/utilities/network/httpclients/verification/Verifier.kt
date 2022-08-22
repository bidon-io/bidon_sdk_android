package com.appodeal.ads.network.httpclients.verification

import android.util.Base64
import com.appodealstack.bidon.utilities.network.NetworkSettings
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

internal interface Verifier {
    fun createRequestId(): String
    fun isResponseValid(responseId: String?): Boolean

    companion object {
        /**
         * Create unique Verifier for each request as it stores Request ID
         */
        fun newInstance(): Verifier = VerifierImpl()
    }
}

private class VerifierImpl : Verifier {

    private var requestId: String? = null

    override fun createRequestId(): String {
        return UUID.randomUUID().toString().also {
            requestId = it
        }
    }

    override fun isResponseValid(responseId: String?): Boolean {
        val requestId = requestId ?: return true
        if (requestId.isNotEmpty() && !responseId.isNullOrEmpty()) {
            val decryptedSignatureBytes = Base64.decode(responseId, Base64.DEFAULT)
            return isValidSignature(
                messageBytes = requestId.toByteArray(),
                signatureBytes = decryptedSignatureBytes
            )
        }
        return true
    }

    private fun isValidSignature(
        messageBytes: ByteArray,
        signatureBytes: ByteArray
    ): Boolean {
        return try {
            val keyBytes: ByteArray = Base64.decode(NetworkSettings.Debug.PUBLIC_KEY.toByteArray(), Base64.DEFAULT)
            val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey = keyFactory.generatePublic(publicKeySpec)
            val signatureECDSA = Signature.getInstance("SHA256withECDSA")
            signatureECDSA.initVerify(publicKey)
            signatureECDSA.update(messageBytes)
            signatureECDSA.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }
}
