package com.example.universavideodownloader.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[index]
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getMimeType(filePathOrUrl: String): String {
        val ext = MimeTypeMap.getFileExtensionFromUrl(filePathOrUrl).lowercase(Locale.ROOT)
        if (ext.isNotEmpty()) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (mime != null) return mime
        }
        return when {
            filePathOrUrl.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            filePathOrUrl.endsWith(".webm", ignoreCase = true) -> "video/webm"
            filePathOrUrl.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
            filePathOrUrl.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            filePathOrUrl.endsWith(".3gp", ignoreCase = true) -> "video/3gpp"
            else -> "video/*"
        }
    }

    fun getSafeContentUri(context: Context, filePath: String?, uriString: String?): Uri? {
        if (!uriString.isNullOrBlank()) {
            val parsed = Uri.parse(uriString)
            if (parsed.scheme == "content") {
                return parsed
            }
        }

        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists()) {
                return try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }
            }
        }

        return if (!uriString.isNullOrBlank()) Uri.parse(uriString) else null
    }

    fun openVideo(context: Context, filePath: String?, uriString: String?, title: String) {
        val uri = getSafeContentUri(context, filePath, uriString)
        if (uri == null) {
            Toast.makeText(context, "Video file not found", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = getMimeType(filePath ?: uriString ?: "")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Open $title").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            Toast.makeText(context, "No app available to play this video", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareVideo(context: Context, filePath: String?, uriString: String?, title: String) {
        val uri = getSafeContentUri(context, filePath, uriString)
        if (uri == null) {
            Toast.makeText(context, "Video file not found to share", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = getMimeType(filePath ?: uriString ?: "")
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Downloaded with Universal Video Downloader: $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            val chooser = Intent.createChooser(shareIntent, "Share Video").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to share video", Toast.LENGTH_SHORT).show()
        }
    }

    fun deletePhysicalFile(context: Context, filePath: String?, uriString: String?): Boolean {
        var deleted = false
        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists()) {
                deleted = file.delete()
            }
        }

        if (!uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriString)
                if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    val rows = context.contentResolver.delete(uri, null, null)
                    deleted = deleted || rows > 0
                }
            } catch (_: Exception) {
                // MediaStore delete may require user permission on Android 10+ if not owned
            }
        }
        return deleted
    }

    fun getDownloadDirectory(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(publicDownloads, "UniversalVideoDownloader")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        return targetDir
    }
}
