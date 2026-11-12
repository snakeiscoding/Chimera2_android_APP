package com.jht.widgets.helpers

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.module.AppGlideModule
import java.io.File

//Class to override the glide settings and turn off manifest parsing. Needed for compose previews
@GlideModule
class GlideModuleJava : AppGlideModule() {
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    // disable disk cache globally https://github.com/bumptech/glide/issues/3610#issuecomment-1857548621
    private class NoCachingDiskCache: DiskCache {
        override fun get(key: Key?): File? {
            return null
        }
        override fun put(key: Key?, writer: DiskCache.Writer?) {}
        override fun delete(key: Key?) {}
        override fun clear() {}
    }
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        builder.setDiskCache { NoCachingDiskCache() }
    }
}
