package com.jht.chimera.lib.common.file

import android.net.Uri
import com.jht.chimera.R
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.ui.DrawableUtil
import java.io.File

/** welcome screen video subfolder in our app files (old console config and USB) */
const val WELCOME_VIDEOS_SUBFOLDER = "/settings/theme/welcome_video"
/** welcome screen video subfolder in our app files (downloads via facility services) */
const val WEB_WELCOME_VIDEOS_SUBFOLDER = "/web_assets/welcome_video"

class CustomAssetFiles {
    companion object {
        /**
         * Returns a String describing the Uri of a custom logo from
         * /data/jht/settings/skin/theme/logo.
         *
         * @return  a String describing the Uri, or an empty String if no asset file exists
         */
        fun getCustomLogo(): String {
            var logoUri = ""
            val appDir = ContextUtil.getApplicationContext()?.filesDir
            if (appDir != null && appDir.exists()) {
                val logoDir = File(appDir, "settings/theme/logo")
                if (logoDir.exists()) {
                    val files = logoDir.listFiles()
                    if (files != null && files.isNotEmpty()) {
                        val logoFile = files[0]
                        logoUri = Uri.fromFile(logoFile).toString()
                    }
                }
            }
            return logoUri
        }

        /**
         * Returns a List of Strings describing the Uri of custom backgrounds from
         * /data/jht/settings/skin/theme/welcome.
         *
         * If no files are present in the folder above, a default list of assets will be returned
         *
         * Supports "jpeg", "jpg", "png", "gif", and "webp"
         */
        fun getCustomBackgrounds(): List<String> {
            //List of supported file types
            val imageExtensions = arrayOf("jpeg", "jpg", "png", "gif", "webp")

            val customBackgrounds = mutableListOf<String>()
            val mBackgrounds = mutableListOf<File>()

            val path = File(ContextUtil.getApplicationContext()?.filesDir, "/settings/theme/welcome")
            if (path.exists()) {
                val files = path.listFiles()
                if (files != null && files.isNotEmpty()) {
                    for (file in files) {
                        //Find file extension
                        if (file.extension.isNotEmpty()) {
                            for (imageExtension in imageExtensions) {
                                //If extension matches list above, add it to the background list
                                if (imageExtension == file.extension) {
                                    mBackgrounds.add((file))
                                    break
                                }
                            }
                        }
                    }
                    //Sort alphabetically
                    mBackgrounds.sortWith { f1: File, f2: File ->
                        f1.name.compareTo(f2.name)
                    }
                    //Add background files to Uri List of Strings
                    for (file: File in mBackgrounds) {
                        customBackgrounds.add(Uri.fromFile(file).toString())
                    }
                }
            }

            //Add defaults if no custom files have been found
            if (customBackgrounds.isEmpty()) {
                val context = ContextUtil.getApplicationContext()
                if (context != null) {
                    customBackgrounds.add(
                        DrawableUtil.getUri(
                            context,
                            R.drawable.screen_saver_commercial_v1
                        ).toString()
                    )
                    customBackgrounds.add(
                        DrawableUtil.getUri(
                            context,
                            R.drawable.screen_saver_commercial_v2
                        ).toString()
                    )
                    customBackgrounds.add(
                        DrawableUtil.getUri(
                            context,
                            R.drawable.screen_saver_commercial_v3
                        ).toString()
                    )
                    customBackgrounds.add(
                        DrawableUtil.getUri(
                            context,
                            R.drawable.screen_saver_commercial_v4
                        ).toString()
                    )
                    customBackgrounds.add(
                        DrawableUtil.getUri(
                            context,
                            R.drawable.screen_saver_commercial_v5
                        ).toString()
                    )
                    customBackgrounds.add(
                        DrawableUtil.getUri(
                            context,
                            R.drawable.screen_saver_commercial_v6
                        ).toString()
                    )
                }
            }

            return customBackgrounds
        }

        /**
         * Returns a List of Strings describing the Uri of custom videos from "/data/jht/settings/skin/theme/welcome_video".
         * If no files are present in the folder above, an empty list of assets will be returned.
         * Supports "mp4", "mov".
         */
        fun getCustomVideos(): List<String> {
            val regularList = getVideoFilesFrom(WELCOME_VIDEOS_SUBFOLDER)
            val webList = getVideoFilesFrom(WEB_WELCOME_VIDEOS_SUBFOLDER)
            return regularList + webList
        }
        private fun getVideoFilesFrom(subPath: String): List<String> {
            val path = File(ContextUtil.getApplicationContext()?.filesDir, subPath)
            if (path.exists()) {
                // List of supported file types
                val fileExtensions = arrayOf("mp4", "mov")
                val backgroundFiles = mutableListOf<File>()
                path.listFiles()?.forEach { file ->
                    //If extension matches list above, add it to the background list
                    if (file.extension.isNotEmpty()) fileExtensions.find { it == file.extension }?.let { backgroundFiles.add((file)) } // should we ignore case?
                }
                //Sort alphabetically
                backgroundFiles.sortWith { f1: File, f2: File ->
                    f1.name.compareTo(f2.name)
                }
                //Add background files to Uri List of Strings
                return backgroundFiles.map { Uri.fromFile(it).toString() }
            }
            return listOf()
        }

        /**
         * Returns a List of Strings describing the Uri of default backgrounds
         */
        fun getDefaultBackground(): List<String> {
            val defaultBackground = mutableListOf<String>()
            val context = ContextUtil.getApplicationContext()
            if (context != null) {
                defaultBackground.add(
                    DrawableUtil.getUri(
                        context,
                        R.drawable.new_welcome_background
                    ).toString()
                )
            }
            return defaultBackground
        }
    }
}