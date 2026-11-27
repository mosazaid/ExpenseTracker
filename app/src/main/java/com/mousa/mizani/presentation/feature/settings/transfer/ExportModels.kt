package com.mousa.mizani.presentation.feature.settings.transfer

import android.net.Uri

enum class ExportScope {
    ALL,
    CURRENT_MONTH
}

enum class ExportFormat {
    CSV,
    EXCEL,
    PDF
}

/** A created file waiting to be viewed, saved or shared. */
data class ExportShareRequest(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
    val title: String,
    /** Only full backups update the "last backup created" time when delivered. */
    val isFullBackup: Boolean = false
)
