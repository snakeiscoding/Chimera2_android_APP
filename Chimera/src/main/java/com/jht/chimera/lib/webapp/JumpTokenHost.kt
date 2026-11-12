package com.jht.chimera.lib.webapp

import android.net.Uri

/** Decides which pages the web app may hand JUMP tokens and the universal shim to */
object JumpTokenHost {
    // agreed with web; matrixworkouts.jfit.co is moving to workouts.matrixfitness.com
    private val TOKEN_DOMAINS = listOf("jfit.co", "matrixfitness.com")
    // letters, digits, dots, hyphens and underscores; anything else means the browser reads the host differently
    private val HOST_CHARS = Regex("[a-z0-9._-]+")

    /**
     * True only for https pages on `jfit.co`, `matrixfitness.com`, or a subdomain of either
     *
     * Parses leniently, so query characters like `|`, `{` or a bare `%` and underscores in the host pass.
     * Matches the parsed host, so `jfit.co.evil.com`, `evilmatrixfitness.com`, `?ref=jfit`,
     * `https://jfit.co@evil.com` and `https://evil.com\@jfit.co` are all rejected.
     */
    @JvmStatic
    fun allows(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val uri = Uri.parse(url)
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        // browsers end the authority at a backslash, android.net.Uri doesn't
        if (uri.encodedAuthority?.contains('\\') != false) return false
        val host = uri.host?.lowercase() ?: return false
        if (!HOST_CHARS.matches(host)) return false
        return TOKEN_DOMAINS.any { host == it || host.endsWith(".$it") }
    }

    /**
     * True for pages that get the universal shim, which carries no secrets
     *
     * Kept on the original loose check so every page that had the shim still does.
     */
    @JvmStatic
    fun allowsShim(url: String?): Boolean = url != null && url.contains("http") && url.contains("jfit")
}
