package com.jht.chimera.lib.fleet

import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.oauthlib.auth.ConsoleOAuthConfig
import com.jht.oauthlib.auth.OAuthConfig

/**
 * Creates an OAuthConfig backed by per-device DCR credentials from FleetCredentialStore.
 * Drop-in replacement for createOAuthConfig() which uses shared build-time credentials.
 */
suspend fun createFleetOAuthConfig(deployment: Int): OAuthConfig {
    val context = ContextUtil.getApplicationContext()
        ?: throw IllegalStateException("Application context not available")
    val store = FleetCredentialStore.getInstance(context)
    val identity = store.load()
    return ConsoleOAuthConfig(
        context = context,
        deployment = deployment,
        clientId = identity?.oauthClientId ?: "",
        clientSecret = identity?.oauthClientSecret ?: ""
    )
}
