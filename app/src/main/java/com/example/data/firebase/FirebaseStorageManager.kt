package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.TraceNetApplication
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class FirebaseStorageManager {
    private val TAG = "FirebaseStorageManager"

    private val storage: FirebaseStorage?
        get() = try {
            val apps = com.google.firebase.FirebaseApp.getApps(TraceNetApplication.instance)
            val app = if (apps.isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(TraceNetApplication.instance)
            } else {
                apps[0]
            }
            if (app != null) {
                FirebaseStorage.getInstance(app)
            } else {
                Log.e(TAG, "FirebaseApp is null in FirebaseStorageManager")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage is not initialized or configured: ${e.message}")
            null
        }

    suspend fun uploadImage(uri: Uri, folder: String): String? {
        return uploadImage(TraceNetApplication.instance, uri, folder)
    }

    suspend fun uploadImage(context: Context, uri: Uri, folder: String): String? {
        return uploadFile(context, uri, "$folder/${UUID.randomUUID()}.jpg")
    }

    suspend fun uploadVideo(uri: Uri, folder: String): String? {
        return uploadVideo(TraceNetApplication.instance, uri, folder)
    }

    suspend fun uploadVideo(context: Context, uri: Uri, folder: String): String? {
        return uploadFile(context, uri, "$folder/${UUID.randomUUID()}.mp4")
    }

    suspend fun uploadAudio(uri: Uri, folder: String): String? {
        return uploadAudio(TraceNetApplication.instance, uri, folder)
    }

    suspend fun uploadAudio(context: Context, uri: Uri, folder: String): String? {
        return uploadFile(context, uri, "$folder/${UUID.randomUUID()}.mp3")
    }

    suspend fun uploadImageBytes(bytes: ByteArray, folder: String): String? {
        val s = storage ?: return null
        val path = "$folder/${UUID.randomUUID()}.jpg"
        return try {
            val ref = s.reference.child(path)
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCacheControl("public, max-age=31536000")
                .build()
            ref.putBytes(bytes, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded bytes to $path: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading bytes: ${e.message}", e)
            null
        }
    }

    private suspend fun uploadFile(context: Context, uri: Uri, path: String): String? {
        val s = storage ?: run {
            Log.e(TAG, "❌ Firebase Storage is NOT initialized. Cannot upload $uri")
            return null
        }
        
        val maxFileSize = if (path.contains("videos") || path.contains("audio")) {
            50L * 1024 * 1024
        } else {
            15L * 1024 * 1024
        }
        val fileSize = try {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (!file.isFile || !file.canRead()) return null
                file.length()
            } else {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to inspect media source $uri: ${e.message}")
            return null
        }

        if (fileSize > maxFileSize) {
            Log.e(TAG, "File too large: $fileSize bytes. Max allowed is $maxFileSize bytes.")
            return null
        }

        val mimeType = (try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }) ?: when (path.substringAfterLast('.', "").lowercase()) {
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
        
        // Adjust path extension based on detected mimeType if it was generic
        val actualPath = if (path.endsWith(".jpg") || path.endsWith(".mp4") || path.endsWith(".mp3")) {
            val extension = mimeType.substringAfterLast('/', "")
            if (extension.isNotEmpty() && !path.endsWith(".$extension")) {
                path.substringBeforeLast('.') + ".$extension"
            } else path
        } else path

        return try {
            Log.d(TAG, "🚀 Starting cloud upload: $uri -> $actualPath")
            
            val ref = s.reference.child(actualPath)
            
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCacheControl("public, max-age=31536000")
                .build()

            if (BuildConfig.DEBUG) {
                Log.e("FirebaseValidation", "===== VALIDATION STEP 1 =====")
                Log.e("FirebaseValidation", "Firebase Storage path: $actualPath")
            }
            
            val uriStr = uri.toString()
            if (uriStr.startsWith("data:")) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Uploading base64 data URI directly via putBytes")
                val commaIndex = uriStr.indexOf(",")
                if (commaIndex != -1) {
                    val base64Data = uriStr.substring(commaIndex + 1)
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val uploadTask = ref.putBytes(bytes, metadata)
                    uploadTask.addOnProgressListener { taskSnapshot ->
                        if (taskSnapshot.totalByteCount > 0) {
                            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                            if (BuildConfig.DEBUG) Log.d(TAG, "⏳ Upload progress for $actualPath: ${"%.2f".format(progress)}%")
                        }
                    }
                    uploadTask.await()
                    val downloadUrl = ref.downloadUrl.await().toString()
                    if (BuildConfig.DEBUG) {
                        Log.e("FirebaseValidation", "Generated download URL: $downloadUrl")
                        Log.e("FirebaseValidation", "Upload success status: TRUE")
                    }
                    Log.d(TAG, "✅ Cloud upload successful! Cloud URL: $downloadUrl")
                    return downloadUrl
                } else {
                    throw IllegalArgumentException("Invalid data URI format")
                }
            }
            
            val normalizedUri = if (uri.scheme == null && uri.path != null) {
                Uri.fromFile(File(uri.path!!))
            } else if (uri.scheme == "file") {
                val filePath = uri.path ?: uri.toString().replace("file://", "")
                val file = File(filePath)
                Uri.fromFile(file)
            } else {
                uri
            }

            if (normalizedUri.scheme == "content") {
                context.contentResolver.openInputStream(normalizedUri)?.use { } 
                    ?: throw IllegalArgumentException("Media provider returned no readable stream")
            }

            if (BuildConfig.DEBUG) Log.d(TAG, "Normalized URI for upload: $normalizedUri")
            val uploadTask = ref.putFile(normalizedUri, metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    if (BuildConfig.DEBUG) Log.d(TAG, "⏳ Upload progress for $actualPath: ${"%.2f".format(progress)}%")
                }
            }
            
            // Wait for the upload task to finish
            uploadTask.await()

            val downloadUrl = ref.downloadUrl.await().toString()
            if (BuildConfig.DEBUG) {
                Log.e("FirebaseValidation", "Generated download URL: $downloadUrl")
                Log.e("FirebaseValidation", "Upload success status: TRUE")
            }
            Log.d(TAG, "✅ Cloud upload successful! Cloud URL: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("FirebaseValidation", "===== VALIDATION STEP 1 =====")
                Log.e("FirebaseValidation", "Firebase Storage path: $actualPath")
                Log.e("FirebaseValidation", "Upload success status: FALSE")
                Log.e("FirebaseValidation", "Upload exception: ${e.message}", e)
            }
            Log.e(TAG, "❌ Critical cloud upload failure for $uri -> $path: ${e.message}", e)
            null
        }
    }

    suspend fun uploadIdCard(uri: Uri, userId: String): String? {
        return uploadIdCard(TraceNetApplication.instance, uri, userId)
    }

    suspend fun uploadIdCard(context: Context, uri: Uri, userId: String): String? {
        return uploadFile(context, uri, "id_cards/$userId.jpg")
    }
}

