package com.jht.chimera.lib.common

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.i18n.LanguageProperty
import com.jht.datamatrixlib.DataMatrix
import java.io.File

private const val TAG = "ExternalAssetsUtil"
@SuppressLint("SdCardPath")
const val CONSOLE_STORAGE = "/sdcard/jht/"

private val IMAGE_EXTENSIONS = listOf("png", "jpg")

/**
 * Checks if image file exists. Will check various image extensions if exact match not found.
 */
private fun resolveImageFile(parent: File, name: String): File? {
    // Check if direct match exists
    val direct = File(parent, name)
    if (direct.exists()) return direct
    // Check for possible file extensions
    for (ext in IMAGE_EXTENSIONS) {
        val candidate = File(parent, "$name.$ext")
        if (candidate.exists()) return candidate
    }
    return null
}


// TODO - update the privacy policy html with one that changes dark/light mode (quite possibly a task for web)
private fun getPrivacyPolicyUri(): Uri {
    val languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT)
    val languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId)
    var textDir = File(CONSOLE_STORAGE, "privacy_policy/" + languageProperty.locale.split("_".toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray()[0])
    if (!textDir.exists()){ // if locale pre-fix not found use generic 'en' as last chance
        textDir = File(CONSOLE_STORAGE, "privacy_policy/" + "en/")
    }
    val policyFile = File(textDir, "privacy_policy.html")
    if (policyFile.exists()) {
        return policyFile.toUri()
    } else {
        Log.e(TAG, "External asset privacy policy not found")
        return Uri.EMPTY
    }
}

fun getSDCardBackgroundUri(file: String): Uri {
    return if (ProductConfig.hasPremiumUi()) {
        var sdFile = getSDCardFile(file, "backgrounds_chameleon")
        if (!sdFile.exists()) sdFile = getSDCardFile(file, "backgrounds") // fall back to standard images
        Uri.fromFile(sdFile)
    } else {
        getSDCardUri(file, "backgrounds")
    }
}

fun getSDCardThumbnailUri(file: String): Uri {
    return getSDCardUri(file, "thumbnails")
}

/** Finds frame images for error panel item for given category of frame */
fun findSdCardErrorPanelAsset(frameCategory: String, item: String): List<File> {
    val frameAssets = File(CONSOLE_STORAGE, "frame")
    val frameSubFolder = File(frameAssets, frameCategory)
    if (!frameSubFolder.exists()) return emptyList()
    // check for 2 image flip assets first, then try single image (could just iterate files and grab ones that startsWith?)
    val image1 = resolveImageFile(frameSubFolder, item + "_1")
    val image2 = resolveImageFile(frameSubFolder, item + "_2")
    val flipList = listOfNotNull(image1, image2)
    return flipList.ifEmpty {
        listOfNotNull(
            resolveImageFile(frameSubFolder, item)
        )
    }
}

private fun getSDCardFile(file: String, folder: String): File = File(CONSOLE_STORAGE, folder + File.separator + file)
public fun getSDCardUri(file: String, folder: String): Uri {
    return Uri.fromFile(
        getSDCardFile(file, folder)
    )
}