package com.nebula.browser.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

fun createQrCode(content: String, size: Int = 640): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
        for (x in 0 until size) for (y in 0 until size) {
            setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
}
