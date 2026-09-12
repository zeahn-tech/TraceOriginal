package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object MediaStorageHelper {
    fun saveMediaToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val extension = if (uri.scheme == "file") {
                uri.path?.substringAfterLast(".", "bin") ?: "bin"
            } else {
                val type = try { context.contentResolver.getType(uri) } catch (e: Exception) { null }
                if (type != null) {
                    type.substringAfterLast("/", "bin")
                } else {
                    uri.path?.substringAfterLast(".", "bin") ?: "bin"
                }
            }
            // Sanitize extension to ensure it's reasonable
            val finalExtension = if (extension.length > 5 || extension.contains("/")) "bin" else extension
            
            val inputStream: InputStream = if (uri.scheme == "file") {
                try {
                    File(uri.path ?: "").inputStream()
                } catch (e: Exception) {
                    context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Unable to open media URI: $uri")
                }
            } else {
                context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Unable to open media URI: $uri")
            }
            val file = File(context.filesDir, "media_${UUID.randomUUID()}.$finalExtension")
            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            if (!file.exists() || file.length() == 0L) {
                file.delete()
                throw IllegalArgumentException("Media URI contains no readable data: $uri")
            }
            Uri.fromFile(file).toString()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    fun getMediaUriAndCacheIfNeeded(context: Context, url: String): String {
        if (!url.startsWith("data:")) return url
        try {
            val commaIndex = url.indexOf(",")
            if (commaIndex == -1) return url
            val base64Data = url.substring(commaIndex + 1)
            
            // Use a stronger hash to avoid collisions by hashing the full content
            val hash = try {
                val digest = java.security.MessageDigest.getInstance("MD5")
                val hashBytes = digest.digest(base64Data.toByteArray())
                hashBytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                base64Data.hashCode().toString() + base64Data.length
            }
            
            val mimeTypeStartIndex = 5
            val mimeTypeEndIndex = url.indexOf(";")
            val mimeType = if (mimeTypeEndIndex != -1 && mimeTypeEndIndex > mimeTypeStartIndex) {
                url.substring(mimeTypeStartIndex, mimeTypeEndIndex)
            } else {
                "application/octet-stream"
            }
            
            val extension = when {
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                mimeType.contains("png") -> "png"
                mimeType.contains("gif") -> "gif"
                mimeType.contains("mp4") -> "mp4"
                mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
                mimeType.contains("wav") -> "wav"
                else -> mimeType.split("/").lastOrNull()?.take(4) ?: "bin"
            }
            
            val cacheDir = File(context.cacheDir, "tracenet_media")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val cacheFile = File(cacheDir, "med_$hash.$extension")
            
            if (!cacheFile.exists()) {
                try {
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    cacheFile.writeBytes(bytes)
                } catch (e: Throwable) {
                    return url
                }
            }
            
            // Use a standard file URI
            return Uri.fromFile(cacheFile).toString()
        } catch (e: Throwable) {
            e.printStackTrace()
            return url
        }
    }
}
