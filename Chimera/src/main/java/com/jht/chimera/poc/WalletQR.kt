package com.jht.chimera.poc

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.http.IHttpClient
import com.jht.chimera.lib.common.http.SimpleHttpClient
import com.jht.chimera.lib.fleet.BearerTokenHelper
import org.json.JSONObject

class WalletQR {

    private val TAG = "WalletQR"
    private val httpClient: IHttpClient = SimpleHttpClient()

    fun generateQRCode(data: String): Bitmap? {
        return try {
            val qrCodeWriter = QRCodeWriter()
            val hints = mutableMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 1 // Optional margin adjustment

            // Generate QR Code
            val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 512, 512, hints)

            // Convert bitMatrix to Bitmap
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateWalletPass(context: Context, passData: String, username: String, uuid: String, dapiEnv: Int): String {
        val bearerToken = BearerTokenHelper.getBearerToken(context).getOrDefault("")

        val postJson = JSONObject()
            .put("platform", "universal")
            .put("templateId", "wallet_service_poc_login_v3")
            .put("passData", passData)
            .put("passHeader", username)
            .put("uniqueId", uuid)

        val url = when (dapiEnv) {
            DAPIKeys.Constants.DAPI_DEPLOYMENT_DEV -> "https://dev-wallet.jhtdev.services/v1/pass"
            DAPIKeys.Constants.DAPI_DEPLOYMENT_QA -> "https://qa-wallet.jhtdev.services/v1/pass"
            DAPIKeys.Constants.DAPI_DEPLOYMENT_STAGING -> "https://staging-wallet.jhtdev.services/v1/pass"
            else -> "https://wallet.jht.services/v1/pass"
        }

        val response = httpClient.put(
            url = url,
            headers = mapOf(
                "Content-Type" to listOf("application/json"),
                "Authorization" to listOf(bearerToken)
            ),
            body = postJson.toString(),
        )
        return when (response.statusCode) {
            200, 201 -> {
                JSONObject(response.body).optString("passUrl", "").trim()
            }
            else -> {
                Log.d(TAG, "Unexpected response: $response")
                ""
            }
        }
    }
}
