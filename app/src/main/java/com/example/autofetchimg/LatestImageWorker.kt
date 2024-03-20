package com.example.autofetchimg
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*


class LatestImageWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val latestImageUri = getLatestImageUri(applicationContext.contentResolver)

            Log.d(TAG, "Started LatestImageWorker $latestImageUri")
            if (latestImageUri != null) {
                Log.d(TAG, "Latest image URI: $latestImageUri")

                val filePath = getPathFromUri(latestImageUri)
                val file = File(filePath)
                if (file.exists()) {
                    val byteArray = file.readBytes()
                    Log.e(TAG, file.toString())
                    uploadImageToServer(byteArray)
                } else {
                    Log.e(TAG, "File does not exist")
                }
            } else {
                Log.d(TAG, "No latest image found")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving latest image", e)
            Result.failure()
        }
    }

    private fun getLatestImageUri(contentResolver: ContentResolver): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val query = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
        query?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumnIndex)
                return Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
            }
        }
        return null
    }

    private fun getPathFromUri(uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = applicationContext.contentResolver.query(uri, projection, null, null, null)
        cursor?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            path = cursor.getString(columnIndex)
        }
        return path
    }

    private fun uploadImageToServer(byteArray: ByteArray) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val imagesRef = storageRef.child("images") // Storage path where you want to store the images

        // Generate a random unique name for the image
        val imageName = "image_${UUID.randomUUID()}"

        // Create a reference to the image in Firebase Storage
        val imageRef = imagesRef.child(imageName)

        // Upload the byte array to Firebase Storage
        imageRef.putBytes(byteArray)
            .addOnSuccessListener {
                // Image upload successful
                // Get the download URL of the uploaded image
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Handle the download URL (if needed)
                    val downloadUrl = uri.toString()
                    Log.d(TAG, "uploadImageToServer: $downloadUrl")

                    // Now you can use the downloadUrl to display or further process the image
                    // You may also want to save this URL in your database if required
                }.addOnFailureListener { exception ->

                    Log.d(TAG, "uploadImageToServer: addOnFailureListener")
                    // Handle any errors that occurred while retrieving the download URL
                }
            }
            .addOnFailureListener { exception ->
                // Image upload failed
                Log.d(TAG, "uploadImageToServer: addOnFailureListenertwo")
                // Handle any errors that occurred during the upload process
            }
    }

    companion object {
        private const val TAG = "LatestImageWorker"
    }
}
