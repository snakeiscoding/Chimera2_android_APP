package com.jht.interfaces

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File

/** Our system uses a custom Uri scheme "assets" for android asset files that might be overridden by files in other locations */
fun stripAssetScheme(uri: Uri?, context: Context): Uri {
    if (uri == null) return Uri.EMPTY
    try {
        // example> "assets://samples/virtual-active.jpg" (though the double slash probably should not be there since it denotes authority?)
        if (uri.scheme != "assets") return uri
        // can't use uri.path cause it could interpret subfolder as authority, ssp is everything between ':' and '#'
        val ssp = uri.schemeSpecificPart
        val path = if (ssp.startsWith("//")) ssp.substring(2) else ssp // remove unneeded leading "//" if it is there
        // check for absolute path (original go screen loader did not handle this, just appended to app data path?)
        /*if (path.startsWith("/")) {
            val file = File(path)
            if (file.exists() && file.isFile) return Uri.fromFile(file)
        } else {*/
            // try app data folder first
            val appFile = File(context.filesDir, path)
            if (appFile.exists() && appFile.isFile) return Uri.fromFile(appFile)
            // do we check SD card assets? (original go screen loader did not)
            /*val jhtAssetFolder = File(Environment.getExternalStorageDirectory(), "jht")
            val jhtAssetFile = File(jhtAssetFolder, path)
            if (jhtAssetFile.exists() && jhtAssetFile.isFile) return Uri.fromFile(jhtAssetFile)*/
        //}
        // fallback to android asset folder
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_FILE)
            .encodedAuthority("")
            .appendEncodedPath("android_asset")
            .appendEncodedPath(path)
            .build()
    } catch (_:Exception) {
        return Uri.EMPTY
    }
}
