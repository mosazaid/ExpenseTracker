package com.example.expensetracker.core.export

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import java.io.File

object ExportFileHelper {

    fun buildViewIntent(uri: Uri, mimeType: String): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun saveToDownloads(
        context: Context,
        sourceUri: Uri,
        fileName: String,
        mimeType: String
    ): Result<String> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, sourceUri, fileName, mimeType)
        } else {
            saveWithLegacyDownloads(context, sourceUri, fileName)
        }
    }

    fun copyToUri(context: Context, sourceUri: Uri, destinationUri: Uri) {
        context.contentResolver.openOutputStream(destinationUri)?.use { output ->
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.copyTo(output)
            } ?: error("Could not read export file")
        } ?: error("Could not write to selected location")
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(
        context: Context,
        sourceUri: Uri,
        fileName: String,
        mimeType: String
    ): String {
        val resolver = context.contentResolver
        val pendingValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val destUri = resolver.insert(collection, pendingValues)
            ?: error("Could not create download file")
        try {
            resolver.openOutputStream(destUri)?.use { output ->
                resolver.openInputStream(sourceUri)?.use { input ->
                    input.copyTo(output)
                } ?: error("Could not read export file")
            } ?: error("Could not write download file")
            val publishedValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(destUri, publishedValues, null, null)
        } catch (error: Exception) {
            resolver.delete(destUri, null, null)
            throw error
        }
        return fileName
    }

    private fun saveWithLegacyDownloads(
        context: Context,
        sourceUri: Uri,
        fileName: String
    ): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
            error("Downloads folder unavailable")
        }
        val destFile = File(downloadsDir, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read export file")
        MediaScannerConnection.scanFile(
            context,
            arrayOf(destFile.absolutePath),
            null,
            null
        )
        return destFile.name
    }
}
