package com.example.expensetracker.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ReceiptStorageHelper {

    fun createTempCameraUri(context: Context): Uri? {
        return try {
            val cameraDir = File(context.cacheDir, "camera").apply {
                if (!exists()) mkdirs()
            }
            val tempFile = File.createTempFile("receipt_cam_${System.currentTimeMillis()}_", ".jpg", cameraDir)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveReceiptImage(context: Context, imageUri: Uri): String? {
        return try {
            val receiptsDir = File(context.filesDir, "receipts").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(receiptsDir, "receipt_${UUID.randomUUID()}.jpg")

            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val tempBytes = inputStream.readBytes()

                // Read EXIF orientation directly from the original photo stream
                val orientation = try {
                    val exif = ExifInterface(ByteArrayInputStream(tempBytes))
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } catch (e: Exception) {
                    ExifInterface.ORIENTATION_NORMAL
                }

                // Decode bitmap bounds
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, options)

                // Scale down if larger than 2048x2048 to prevent memory issues
                var inSampleSize = 1
                val maxDim = 2048
                while (options.outHeight / inSampleSize > maxDim || options.outWidth / inSampleSize > maxDim) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                val rawBitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions)
                    ?: return null

                // Physically rotate bitmap so saved file is always in natural upright orientation
                val uprightBitmap = rotateBitmap(rawBitmap, orientation)

                FileOutputStream(destinationFile).use { outStream ->
                    uprightBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                }

                if (uprightBitmap != rawBitmap) {
                    rawBitmap.recycle()
                }
            }

            // Clean up temporary camera cache files
            try {
                val cameraDir = File(context.cacheDir, "camera")
                if (cameraDir.exists()) {
                    cameraDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("receipt_cam_")) {
                            file.delete()
                        }
                    }
                }
            } catch (_: Exception) {}

            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a receipt bitmap from disk, respecting any EXIF orientation metadata
     * and downsampling safely if necessary.
     */
    fun loadReceiptBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            var inSampleSize = 1
            val maxDim = 2048
            while (options.outHeight / inSampleSize > maxDim || options.outWidth / inSampleSize > maxDim) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val rawBitmap = BitmapFactory.decodeFile(path, decodeOptions) ?: return null

            val orientation = try {
                val exif = ExifInterface(path)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            rotateBitmap(rawBitmap, orientation)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Rotates a Bitmap according to ExifInterface orientation constants.
     */
    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            bitmap
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
