package com.mousa.mizani.presentation.feature.settings.backup

import android.content.Context
import androidx.core.content.FileProvider
import com.mousa.mizani.R
import com.mousa.mizani.presentation.feature.settings.transfer.ExportShareRequest
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject

/** Turns a created backup file into a request the UI can save or share. */
fun interface BackupFileSharer {
    fun toShareRequest(file: File): ExportShareRequest
}

class FileProviderBackupFileSharer @Inject constructor(
    @ApplicationContext private val context: Context
) : BackupFileSharer {
    override fun toShareRequest(file: File): ExportShareRequest = ExportShareRequest(
        uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
        mimeType = "application/json",
        fileName = file.name,
        title = context.getString(R.string.full_backup_title),
        isFullBackup = true
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupFileSharerModule {

    @Binds
    abstract fun bindBackupFileSharer(impl: FileProviderBackupFileSharer): BackupFileSharer
}
