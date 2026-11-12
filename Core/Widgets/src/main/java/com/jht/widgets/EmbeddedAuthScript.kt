package com.jht.widgets

import org.json.JSONObject

/**
 * Builds the JS that hands JUMP tokens to an embedded web app per the Console Embedding contract
 *
 * Sets `window.__EMBEDDED_AUTH__` and dispatches `embedded-auth-ready`. Run it from onPageFinished.
 */
fun buildEmbeddedAuthScript(
    accessToken: String,
    idToken: String,
): String =
    "(function() {\n" +
        "  window.__EMBEDDED_AUTH__ = {\n" +
        "    accessToken: ${JSONObject.quote(accessToken)},\n" +
        "    idToken: ${JSONObject.quote(idToken)}\n" +
        "  };\n" +
        "  window.dispatchEvent(new CustomEvent('embedded-auth-ready'));\n" +
        "})();"
