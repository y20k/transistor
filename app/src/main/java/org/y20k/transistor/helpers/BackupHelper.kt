/*
 * BackupHelper.kt
 * Implements the BackupHelper object
 * A BackupHelper provides helper methods for backing up and restoring the radio station collection
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.widget.Toast
import org.y20k.transistor.R
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(BackupHelper::class.java)


    /* Compresses all files in the app's external files directory into destination zip file */
    fun backup(context: Context, destinationUri: Uri) {
        val sourceFolder: File? = context.getExternalFilesDir("")
        if (sourceFolder != null && sourceFolder.isDirectory) {
            Toast.makeText(context, "${context.getString(R.string.toastmessage_collection_backup)} ${FileHelper.getFileName(context, destinationUri)}", Toast.LENGTH_LONG).show()
            val resolver: ContentResolver = context.contentResolver
            val outputStream: OutputStream? = resolver.openOutputStream(destinationUri)
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOutputStream ->
                zipOutputStream.use {
                    zipFolder(it, sourceFolder, "")
                }
            }
        } else {
            LogHelper.e(TAG, "Unable to access External Storage.")
        }
    }


    /* Compresses folder into ZIP file - Credit: https://stackoverflow.com/a/52216574 */
    private fun zipFolder(zipOutputStream: ZipOutputStream, source: File, parentDirPath: String) {
        // source.listFiles() will return null, if source is not a directory
        if (source.isDirectory) {
            val data = ByteArray(2048)
            // get all File objects in folder
            for (file in source.listFiles()!!) {
                val path = parentDirPath + File.separator + file.name
                when (file.isDirectory) {
                    // CASE: Folder
                    true -> {
//                        val entry = ZipEntry(path + File.separator) // add separator to make entry a folder
//                        entry.time = file.lastModified()
//                        entry.size = file.length()
//                        zipOutputStream.putNextEntry(entry)
                        // call zipFolder recursively to add files within this folder
                        zipFolder(zipOutputStream, file, path)
                    }
                    // CASE: File
                    false -> {
                        FileInputStream(file).use { fileInputStream ->
                            BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                                val entry = ZipEntry(path)
                                entry.time = file.lastModified()
                                entry.size = file.length()
                                zipOutputStream.putNextEntry(entry)
                                while (true) {
                                    val readBytes = bufferedInputStream.read(data)
                                    if (readBytes == -1) {
                                        break
                                    }
                                    zipOutputStream.write(data, 0, readBytes)
                                }
                            }
                        }
                    }
                }
            }

        }
    }


    /* Extracts zip backup  file and restores files and folders - Credit: https://www.baeldung.com/java-compress-and-uncompress*/
    fun restore(context: Context, sourceUri: Uri) {
        Toast.makeText(context, context.getString(R.string.toastmessage_restoring_collection), Toast.LENGTH_LONG).show()

        val resolver: ContentResolver = context.contentResolver
        val sourceInputStream: InputStream? = resolver.openInputStream(sourceUri)
        val destinationFolder: File? = context.getExternalFilesDir("")
        val buffer = ByteArray(1024)
        val zipInputStream: ZipInputStream = ZipInputStream(sourceInputStream)
        var zipEntry: ZipEntry? = zipInputStream.nextEntry

        // iterate through ZipInputStream until last ZipEntry
        while (zipEntry != null) {
            try {
                val newFile: File = getFile(destinationFolder!!, zipEntry)
                when (zipEntry.isDirectory) {
                    // CASE: Folder
                    true -> {
                        // create folder if new file is just a file
                        if (!newFile.isDirectory && !newFile.mkdirs()) {
                            LogHelper.w(TAG,"Failed to create directory $newFile")
                        }
                    }
                    // CASE: Files
                    false -> {
                        // create parent directory, if necessary
                        val parent: File? = newFile.parentFile
                        if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
                            LogHelper.w(TAG, "Failed to create directory $parent")
                        }
                        // write file content
                        val fileOutputStream: FileOutputStream = FileOutputStream(newFile)
                        var len: Int
                        while (zipInputStream.read(buffer).also { len = it } > 0) {
                            fileOutputStream.write(buffer, 0, len)
                        }
                        fileOutputStream.close()
                    }
                }
            } catch (e: Exception) {
                LogHelper.e(TAG, "Unable to safely create get file. $e")
            }
            // get next entry - zipEntry will be null, when zipInputStream has no more entries left
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.closeEntry()
        zipInputStream.close()

        // notify CollectionViewModel that collection has changed
        CollectionHelper.sendCollectionBroadcast(context, modificationDate = Calendar.getInstance().time)
    }


    /* Normalize file path - protects against zip slip attack */
    @Throws(IOException::class)
    private fun getFile(destinationFolder: File, zipEntry: ZipEntry): File {
        val destinationFile = File(destinationFolder, zipEntry.name)
        val destinationFolderPath = destinationFolder.canonicalPath
        val destinationFilePath = destinationFile.canonicalPath
        // make sure that zipEntry path is in the destination folder
        if (!destinationFilePath.startsWith(destinationFolderPath + File.separator)) {
            throw IOException("ZIP entry is not within of the destination folder: " + zipEntry.name)
        }
        return destinationFile
    }

}



