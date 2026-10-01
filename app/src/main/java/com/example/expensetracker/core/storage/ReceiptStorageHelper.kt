package com.example.expensetracker.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ReceiptStorageHelper {

    fun saveReceiptImage(context: Context, imageUri: Uri): String? {
        return try {
            val receiptsDir = File(context.filesDir, "receipts").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(receiptsDir, "receipt_${UUID.randomUUID()}.jpg")

            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                // Decode bitmap with downsampling if needed to avoid massive heap usage
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                // Read bounds
                val tempBytes = inputStream.readBytes()
                BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, options)

                // Scale down if larger than 2048x2048
                var inSampleSize = 1
                val maxDim = 2048
                while (options.outHeight / inSampleSize > maxDim || options.outWidth / inSampleSize > maxDim) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                val bitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions)

                FileOutputStream(destinationFile).use { outStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteReceiptImage(path: String?) {
        if (path == null) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
