/*
 * FileHelper.kt
 * Implements the FileHelper object
 * A FileHelper provides helper methods for reading and writing files from and to device storage
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.y20k.transistor.Keys
import org.y20k.transistor.core.Collection
import org.y20k.transistor.core.Station
import java.io.*
import java.net.URL
import java.text.NumberFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.ln
import kotlin.math.pow


/*
 * FileHelper object
 */
object FileHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(FileHelper::class.java)


    /* Return an InputStream for given Uri */
    fun getTextFileStream(context: Context, uri: Uri): InputStream? {
        var stream : InputStream? = null
        try {
            stream = context.contentResolver.openInputStream(uri)
        } catch (e : Exception) {
            e.printStackTrace()
        }
        return stream
    }


    /* Get file size for given Uri */
    fun getFileSize(context: Context, uri: Uri): Long {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            val sizeIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            val size: Long = cursor.getLong(sizeIndex)
            cursor.close()
            return size
        } else {
            return 0L
        }
    }


    /* Get file name for given Uri */
    fun getFileName(context: Context, uri: Uri): String {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            val nameIndex: Int = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name: String = cursor.getString(nameIndex)
            cursor.close()
            return name
        } else {
            return String()
        }
    }


    /* Get content type for given file */
    fun getContentType(context: Context, uri: Uri): String {
        // get file type from content resolver
        var contentType: String = context.contentResolver.getType(uri) ?: Keys.MIME_TYPE_UNSUPPORTED
        contentType = contentType.lowercase(Locale.getDefault())
        if (contentType != Keys.MIME_TYPE_UNSUPPORTED && !contentType.contains(Keys.MIME_TYPE_OCTET_STREAM)) {
            // return the found content type
            return contentType
        } else {
            // fallback: try to determine file type based on file extension
            return getContentTypeFromExtension(getFileName(context, uri))
        }
    }


    /* Determine content type based on file extension */
    fun getContentTypeFromExtension(fileName: String): String {
        LogHelper.i(TAG, "Deducing content type from file name: $fileName")
        if (fileName.endsWith("m3u", true)) return Keys.MIME_TYPE_M3U
        if (fileName.endsWith("pls", true)) return Keys.MIME_TYPE_PLS
        if (fileName.endsWith("png", true)) return Keys.MIME_TYPE_PNG
        if (fileName.endsWith("jpg", true)) return Keys.MIME_TYPE_JPG
        if (fileName.endsWith("jpeg", true)) return Keys.MIME_TYPE_JPG
        // default return
        return Keys.MIME_TYPE_UNSUPPORTED
    }


    /* Determines a destination folder */
    fun determineDestinationFolderPath(type: Int, stationUuid: String): String {
        val folderPath: String
        when (type) {
            Keys.FILE_TYPE_PLAYLIST -> folderPath = Keys.FOLDER_TEMP
            Keys.FILE_TYPE_AUDIO -> folderPath = Keys.FOLDER_AUDIO + "/" + stationUuid
            Keys.FILE_TYPE_IMAGE -> folderPath = Keys.FOLDER_IMAGES + "/" + stationUuid
            else -> folderPath = "/"
        }
        return folderPath
    }


    /* Clears given folder - keeps given number of files */
    fun clearFolder(folder: File?, keep: Int, deleteFolder: Boolean = false) {
        if (folder != null && folder.exists()) {
            val files = folder.listFiles()
            val fileCount: Int = files.size
            files.sortBy { it.lastModified() }
            for (fileNumber in files.indices) {
                if (fileNumber < fileCount - keep) {
                    files[fileNumber].delete()
                }
            }
            if (deleteFolder && keep == 0) {
                folder.delete()
            }
        }
    }



    /* Creates a copy of a given uri from downloadmanager - goal is to provide stable Uris */
    fun saveCopyOfFile(context: Context, stationUuid: String, tempFileUri: Uri, fileType: Int, fileName: String, async: Boolean = false): Uri {
        val targetFile: File = File(context.getExternalFilesDir(determineDestinationFolderPath(fileType, stationUuid)), fileName)
        if (targetFile.exists()) targetFile.delete()
        when (async) {
            true -> {
                // copy file async (= fire & forget - no return value needed)
                CoroutineScope(IO).launch { saveCopyOfFileSuspended(context, tempFileUri, targetFile.toUri()) }
            }
            false -> {
                // copy file
                copyFile(context, tempFileUri, targetFile.toUri(), deleteOriginal = true)
            }
        }
        return targetFile.toUri()
    }



    /* Creates and save a scaled version of the station image */
    fun saveStationImage(context: Context, stationUuid: String, sourceImageUri: String, size: Int, fileName: String): Uri {
        val coverBitmap: Bitmap = ImageHelper.getScaledStationImage(context, sourceImageUri, size)
        val file: File = File(context.getExternalFilesDir(determineDestinationFolderPath(Keys.FILE_TYPE_IMAGE, stationUuid)), fileName)
        writeImageFile(coverBitmap, file, Bitmap.CompressFormat.JPEG, quality = 75)
        return file.toUri()
    }


    /* Saves collection of radio stations as JSON text file */
    fun saveCollection(context: Context, collection: Collection, lastSave: Date) {
        LogHelper.v(TAG, "Saving collection - Thread: ${Thread.currentThread().name}")
        val collectionSize: Int = collection.stations.size
        // do not override an existing collection with an empty one - except when last station is deleted
        if (collectionSize > 0 || PreferencesHelper.loadCollectionSize() == 1) {
            // convert to JSON
            val gson: Gson = getCustomGson()
            var json: String = String()
            try {
                json = gson.toJson(collection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (json.isNotBlank()) {
                // write text file
                writeTextFile(context, json, Keys.FOLDER_COLLECTION, Keys.COLLECTION_FILE)
                // save modification date and collection size
                PreferencesHelper.saveCollectionModificationDate(lastSave)
                PreferencesHelper.saveCollectionSize(collectionSize)
            } else {
                LogHelper.w(TAG, "Not writing collection file. Reason: JSON string was completely empty.")
            }
        } else {
            LogHelper.w(TAG, "Not saving collection. Reason: Trying to override an collection with more than one station")
        }
    }


    /* Checks if a folder contains a collection.json file */
    fun checkForCollectionFile(folder: File): Boolean {
        if (folder.exists() && folder.isDirectory) {
            val collectionFiles = folder.listFiles { _, name -> name?.equals(Keys.COLLECTION_FILE) ?: false }
            return collectionFiles?.isNotEmpty() ?: false
        }
        return false
    }



    /* Reads m3u or pls playlists */
    fun readStationPlaylist(playlistInputStream: InputStream?): Station {
        val station: Station = Station()
        if (playlistInputStream != null) {
            val reader: BufferedReader = BufferedReader(InputStreamReader(playlistInputStream))
            // until last line reached: read station name and stream address(es)
            reader.forEachLine { line ->
                when {
                    // M3U: found station name
                    line.contains("#EXTINF:-1,") -> station.name = line.substring(11).trim()
                    line.contains("#EXTINF:0,") -> station.name = line.substring(10).trim()
                    // M3U: found stream URL
                    line.startsWith("http") -> station.streamUris.add(0, line.trim())
                    // PLS: found station name
                    line.matches(Regex("^Title[0-9]+=.*")) -> station.name = line.substring(line.indexOf("=") + 1).trim()
                    // PLS: found stream URL
                    line.matches(Regex("^File[0-9]+=http.*")) -> station.streamUris.add(line.substring(line.indexOf("=") + 1).trim())
                }

            }
            playlistInputStream.close()
        }
        return station
    }


    /* Reads collection of radio stations from storage using GSON */
    fun readCollection(context: Context): Collection {
        LogHelper.v(TAG, "Reading collection - Thread: ${Thread.currentThread().name}")
        // get JSON from text file
        val json: String = readTextFile(context, Keys.FOLDER_COLLECTION, Keys.COLLECTION_FILE)
        var collection: Collection = Collection()
        when (json.isNotBlank()) {
            // convert JSON and return as collection
            true -> try {
                collection = getCustomGson().fromJson(json, collection::class.java)
            } catch (e: Exception) {
                LogHelper.e(TAG, "Error Reading collection.\nContent: $json")
                e.printStackTrace()
            }
        }
        return collection
    }


    /* Appends a message to an existing log - and saves it */
    fun saveLog(context: Context, logMessage: String) {
        var log: String = readTextFile(context, Keys.FOLDER_COLLECTION, Keys.DEBUG_LOG_FILE)
        log = "${log} {$logMessage}"
        writeTextFile(context, log, Keys.FOLDER_COLLECTION, Keys.DEBUG_LOG_FILE)
    }


    /* Deletes the debug log file */
    fun deleteLog(context: Context) {
        val logFile: File = File(context.getExternalFilesDir(Keys.FOLDER_COLLECTION), Keys.DEBUG_LOG_FILE)
        if (logFile.exists()) {
            logFile.delete()
        }
    }

    /* Checks if enough ( = more than 512mb) free space is available */
    fun enoughFreeSpaceAvailable(context: Context): Boolean {
        val usableSpace: Long = context.getExternalFilesDir(Keys.FOLDER_COLLECTION)?.usableSpace ?: 0L
        LogHelper.e(TAG, "usableSpace: $usableSpace")
        return usableSpace > 512000000L
    }


    /* Get content Uri for M3U file */
    fun getM3ulUri(activity: Activity): Uri? {
        var m3ulUri: Uri? = null
        // try to get an existing M3U File
        var m3uFile = File(activity.getExternalFilesDir(Keys.FOLDER_COLLECTION), Keys.COLLECTION_M3U_FILE)
        if (!m3uFile.exists()) { m3uFile = File(activity.getExternalFilesDir(Keys.TRANSISTOR_LEGACY_FOLDER_COLLECTION), Keys.COLLECTION_M3U_FILE) }
        // get Uri for existing M3U File
        if (m3uFile.exists()) { m3ulUri = FileProvider.getUriForFile(activity, "${activity.applicationContext.packageName}.provider", m3uFile) }
        return m3ulUri
    }



    /* Suspend function: Wrapper for saveCollection */
    suspend fun saveCollectionSuspended(context: Context, collection: Collection, lastUpdate: Date) {
        return suspendCoroutine { cont ->
            cont.resume(saveCollection(context, collection, lastUpdate))
        }
    }


    /* Suspend function: Wrapper for readCollection */
    suspend fun readCollectionSuspended(context: Context): Collection =
        withContext(Dispatchers.IO) {
            readCollection(context)
        }


    /* Suspend function: Wrapper for copyFile */
    suspend fun saveCopyOfFileSuspended(context: Context, originalFileUri: Uri, targetFileUri: Uri): Boolean {
        return suspendCoroutine { cont ->
            cont.resume(copyFile(context, originalFileUri, targetFileUri, deleteOriginal = true))
        }
    }


    /* Suspend function: Exports collection of stations as M3U file - local backup copy */
    suspend fun backupCollectionAsM3uSuspended(context: Context, collection: Collection) {
        return suspendCoroutine { cont ->
            LogHelper.v(TAG, "Backing up collection as M3U - Thread: ${Thread.currentThread().name}")
            // create M3U string
            val m3uString: String = CollectionHelper.createM3uString(collection)
            // save M3U as text file
            cont.resume(writeTextFile(context, m3uString, Keys.FOLDER_COLLECTION, Keys.COLLECTION_M3U_FILE))
        }
    }


    /* Copies file to specified target */
    fun copyFile(context: Context, originalFileUri: Uri, targetFileUri: Uri, deleteOriginal: Boolean = false): Boolean {
        var success: Boolean = true
        try {
            val inputStream = context.contentResolver.openInputStream(originalFileUri)
            val outputStream = context.contentResolver.openOutputStream(targetFileUri)
            if (outputStream != null && inputStream != null) {
                inputStream.copyTo(outputStream)
            }
        } catch (exception: Exception) {
            LogHelper.e(TAG, "Unable to copy file.")
            success = false
            exception.printStackTrace()
        }
        if (deleteOriginal) {
            try {
                // use contentResolver to handle files of type content://
                context.contentResolver.delete(originalFileUri, null, null)
            } catch (e: Exception) {
                LogHelper.e(TAG, "Unable to delete the original file. Stack trace: $e")
            }
        }
        return success
    }


    /*  Creates a Gson object */
    private fun getCustomGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.setDateFormat("M/d/yy hh:mm a")
        gsonBuilder.excludeFieldsWithoutExposeAnnotation()
        return gsonBuilder.create()
    }


    /* Create nomedia file in given folder to prevent media scanning */
    fun createNomediaFile(folder: File?) {
        if (folder != null && folder.exists() && folder.isDirectory) {
            val nomediaFile: File = getNoMediaFile(folder)
            if (!nomediaFile.exists()) {
                val noMediaOutStream: FileOutputStream = FileOutputStream(getNoMediaFile(folder))
                noMediaOutStream.write(0)
            } else {
                LogHelper.v(TAG, ".nomedia file exists already in given folder.")
            }
        } else  {
            LogHelper.w(TAG, "Unable to create .nomedia file. Given folder is not valid.")
        }
    }


    /* Delete nomedia file in given folder */
    fun deleteNoMediaFile(folder: File?) {
        if (folder != null && folder.exists() && folder.isDirectory) {
            getNoMediaFile(folder).delete()
        } else  {
            LogHelper.w(TAG, "Unable to delete .nomedia file. Given folder is not valid.")
        }
    }


    /* Converts byte value into a human readable format */
    // Source: https://programming.guide/java/formatting-byte-size-to-human-readable-format.html
    fun getReadableByteCount(bytes: Long, si: Boolean = true): String {

        // check if Decimal prefix symbol (SI) or Binary prefix symbol (IEC) requested
        val unit: Long = if (si) 1000L else 1024L

        // just return bytes if file size is smaller than requested unit
        if (bytes < unit) return "$bytes B"

        // calculate exp
        val exp: Int = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()

        // determine prefix symbol
        val prefix: String = ((if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i")

        // calculate result and set number format
        val result: Double = bytes / unit.toDouble().pow(exp.toDouble())
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.maximumFractionDigits = 1

        return numberFormat.format(result) + " " + prefix + "B"
    }


    /* Reads InputStream from file uri and returns it as String */
    private fun readTextFile(context: Context, folder: String, fileName: String): String {
        // todo read https://commonsware.com/blog/2016/03/15/how-consume-content-uri.html
        // https://developer.android.com/training/secure-file-sharing/retrieve-info

        // check if file exists
        val file: File = File(context.getExternalFilesDir(folder), fileName)
        if (!file.exists() || !file.canRead()) {
            return String()
        }
        // readSuspended until last line reached
        val stream: InputStream = file.inputStream()
        val reader: BufferedReader = BufferedReader(InputStreamReader(stream))
        val builder: StringBuilder = StringBuilder()
        reader.forEachLine {
            builder.append(it)
            builder.append("\n") }
        stream.close()
        return builder.toString()
    }


    /* Writes given text to file on storage */
    private fun writeTextFile(context: Context, text: String, folder: String, fileName: String) {
        if (text.isNotBlank()) {
            File(context.getExternalFilesDir(folder), fileName).writeText(text)
        } else {
            LogHelper.w(TAG, "Writing text file $fileName failed. Empty text string text was provided.")
        }
    }


    /* Writes given text to file specified by destinationUri */
    private fun writeTextToUri(context: Context, text: String, destinationUri: Uri) {
        if (text.isNotBlank()) {
            val resolver: ContentResolver = context.contentResolver
            val outputStream: OutputStream? = resolver.openOutputStream(destinationUri)
            outputStream?.write(text.toByteArray(Charsets.UTF_8))
        } else {
            LogHelper.w(TAG, "Writing text file $destinationUri failed. Empty text string text was provided.")
        }
    }



    /* Writes given bitmap as image file to storage */
    private fun writeImageFile(bitmap: Bitmap, file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 75) {
        if (file.exists()) file.delete ()
        try {
            val out = FileOutputStream(file)
            bitmap.compress(format, quality, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /* Checks the size of the collection folder */
    fun getCollectionFolderSize(context: Context): Int {
        val folder: File? = context.getExternalFilesDir(Keys.FOLDER_COLLECTION)
        val files = folder?.listFiles()
        if (folder != null && folder.exists() && folder.isDirectory) {
            return files?.size ?: -1
        } else {
            return -1
        }
    }



    /* Returns a nomedia file object */
    private fun getNoMediaFile(folder: File): File {
        return File(folder, ".nomedia")
    }


    /* Tries to parse feed URL string as URL */
    private fun isParsableAsUrl(feedUrl: String): Boolean {
        try {
            URL(feedUrl)
        } catch (e: Exception) {
            return false
        }
        return true
    }

}
