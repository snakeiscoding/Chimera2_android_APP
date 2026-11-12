package com.jht.chimera.lib.fleet

import android.content.Context
import android.util.Log
import com.jht.oauthlib.auth.ClientCredentialsFlow
import com.jht.oauthlib.auth.ConsoleOAuthConfig
import com.jht.oauthlib.auth.TokenData
import java.time.Instant
import java.time.ZoneId

object BearerTokenHelper {
    private const val TAG = "BearerTokenHelper"

    /**
     * Returns a valid Bearer access token for the current fleet identity.
     *
     * By default a stored/cached token is reused while it is still valid; only an expiring
     * or missing token triggers a network request. Pass [forceRefresh] = true to bypass the
     * stored token and obtain a brand-new one.
     *
     * @param context Android context used to load the fleet credential store.
     * @param forceRefresh When true, ignores any stored/cached token and forces a fresh request.
     * @return [Result] with the Bearer token string, or a failure if no identity is available
     *   or authentication fails.
     */
    suspend fun getAccessToken(context: Context, forceRefresh: Boolean = false): Result<String> {

        // Create the ClientCredentialsFlow flow object
        val store = FleetCredentialStore.getInstance(context)
        val identity = store.load()
            ?: return Result.failure(IllegalStateException("No identity available"))
        val fleetEnvironment = FleetEnvironment.fromDeployment(identity.deployment)
        val config = ConsoleOAuthConfig(
            context,
            fleetEnvironment.deployment,
            identity.oauthClientId,
            identity.oauthClientSecret
        )
        val requestScopes = identity.oauthScope
        val flow = ClientCredentialsFlow(config)
        val storedToken = store.getBearerJson()?.let { jsonStr ->
            runCatching { TokenData.fromJson(jsonStr) }.getOrNull()
        }

        // If no force refresh needed and there's a valid stored token, load it
        if (!forceRefresh && storedToken != null) {
            Log.i(TAG, "Using Bearer token from store. ExpiresAt: ${storedToken.expiresAt} ${
                Instant.ofEpochMilli(storedToken.expiresAt).atZone(ZoneId.systemDefault())}")
            flow.storeToken(storedToken)
        } else {
            Log.i(TAG, "Fetching new Bearer token from server")
        }

        return flow.authenticate(config.getDeployment(), requestScopes)
            .onSuccess { newToken ->
                // Save new token if it's different from the stored one
                if (newToken != storedToken) {
                    Log.d(TAG, "Saving Bearer token to store")
                    store.saveBearerJson(newToken.toJson())
                }
            }
            .map { it.accessToken }
    }

    /**
     * Returns a valid access token formatted as an HTTP Authorization header value,
     * e.g. "Bearer eyJraWQ...".
     *
     * Delegates to [getAccessToken] (including its caching/refresh behavior) and prefixes
     * the token with the "Bearer " scheme on success. Use this when the consumer expects
     * a complete header value; use [getAccessToken] when the consumer adds the scheme itself.
     *
     * @param context Android context used to load the fleet credential store.
     * @param forceRefresh When true, ignores any stored/cached token and forces a fresh request.
     * @return [Result] with the "Bearer "-prefixed token string, or a failure if no identity
     *   is available or authentication fails.
     */
    suspend fun getBearerToken(context: Context, forceRefresh: Boolean = false): Result<String> =
        getAccessToken(context, forceRefresh).map { "Bearer $it" }

}