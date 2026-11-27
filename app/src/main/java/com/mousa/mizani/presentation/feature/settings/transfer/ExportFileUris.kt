package com.mousa.mizani.presentation.feature.settings.transfer

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject

/** Gives other apps read access to a file this app created in its cache. */
fun interface ExportFileUris {
    fun uriFor(file: File): Uri
}

class FileProviderExportFileUris @Inject constructor(
    @ApplicationContext private val context: Context
) : ExportFileUris {
    override fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportFileUrisModule {

    @Binds
    abstract fun bindExportFileUris(impl: FileProviderExportFileUris): ExportFileUris
}
