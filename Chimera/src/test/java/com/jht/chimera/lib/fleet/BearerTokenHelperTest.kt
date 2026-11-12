package com.jht.chimera.lib.fleet

import android.content.Context
import android.util.Log
import com.jht.oauthlib.auth.ClientCredentialsFlow
import com.jht.oauthlib.auth.ConsoleOAuthConfig
import com.jht.oauthlib.auth.TokenData
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BearerTokenHelper.getAccessToken] and [BearerTokenHelper.getBearerToken].
 *
 * The helper is an `object` that reaches out to collaborators it constructs internally
 * ([FleetCredentialStore.getInstance], [ConsoleOAuthConfig], [ClientCredentialsFlow]) and
 * logs via [android.util.Log], so the collaborators are replaced with MockK
 * object/constructor/static mocks.
 *
 * NOTE — assumptions about the `com.jht.oauthlib` API (the artifact wasn't available to
 * introspect). If the library signatures differ, adjust these few lines:
 *   - ConsoleOAuthConfig.getDeployment() returns Int
 *   - ClientCredentialsFlow.authenticate(deployment, scope) is suspending, returns Result<TokenData>
 *   - TokenData.fromJson(String) is a companion function
 */
class BearerTokenHelperTest {

    private lateinit var store: FleetCredentialStore
    private lateinit var context: Context

    private lateinit var storedToken: TokenData
    private lateinit var newToken: TokenData

    private val identity = FleetDeviceIdentity(
        deployment = FleetEnvironment.DEV.deployment, // 0 -> valid enum for fromDeployment()
        thingName = "thing-1",
        certificatePem = "cert",
        privateKeyPem = "key",
        oauthClientId = "client-id",
        oauthClientSecret = "client-secret",
        oauthScope = "fleet:read fleet:write",
        sgmPublicKey = "sgm-pub"
    )

    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        // Android Log is not available on the JVM unit test classpath.
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0

        // FleetCredentialStore.getInstance is a companion-object factory.
        mockkObject(FleetCredentialStore.Companion)
        store = mockk()
        every { FleetCredentialStore.getInstance(any()) } returns store

        // TokenData.fromJson(...) is a companion-object function.
        mockkObject(TokenData.Companion)

        // Both oauth collaborators are constructed inside the helper.
        mockkConstructor(ConsoleOAuthConfig::class)
        mockkConstructor(ClientCredentialsFlow::class)
        every { anyConstructed<ConsoleOAuthConfig>().getDeployment() } returns
            FleetEnvironment.DEV.deployment
        coJustRun { anyConstructed<ClientCredentialsFlow>().storeToken(any()) }

        storedToken = mockk()
        every { storedToken.expiresAt } returns 1_900_000_000_000L
        every { storedToken.toJson() } returns "stored-json"
        every { storedToken.accessToken } returns "stored-access-token"

        newToken = mockk()
        every { newToken.expiresAt } returns 1_950_000_000_000L
        every { newToken.toJson() } returns "new-json"
        every { newToken.accessToken } returns "new-access-token"

        // saveBearerJson is `= dataStore.edit { ... }`, so it returns Preferences.
        coEvery { store.saveBearerJson(any()) } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `returns failure when no identity is stored`() = runTest {
        coEvery { store.load() } returns null

        val result = BearerTokenHelper.getAccessToken(context)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("No identity available", result.exceptionOrNull()?.message)
        // Should short-circuit before touching the network / cache.
        coVerify(exactly = 0) { store.getBearerJson() }
        coVerify(exactly = 0) { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) }
    }

    @Test
    fun `reuses valid stored token then saves the refreshed token when it differs`() = runTest {
        coEvery { store.load() } returns identity
        coEvery { store.getBearerJson() } returns "stored-json"
        every { TokenData.fromJson("stored-json") } returns storedToken
        coEvery { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) } returns
            Result.success(newToken)

        val result = BearerTokenHelper.getAccessToken(context, forceRefresh = false)

        assertTrue(result.isSuccess)
        assertEquals("new-access-token", result.getOrNull())
        // Stored token was seeded into the flow before authenticating.
        coVerify(exactly = 1) { anyConstructed<ClientCredentialsFlow>().storeToken(storedToken) }
        // New token differed from stored -> persisted.
        coVerify(exactly = 1) { store.saveBearerJson("new-json") }
    }

    @Test
    fun `does not re-save when refreshed token equals the stored token`() = runTest {
        coEvery { store.load() } returns identity
        coEvery { store.getBearerJson() } returns "stored-json"
        every { TokenData.fromJson("stored-json") } returns storedToken
        // authenticate returns the very same instance as the stored token.
        coEvery { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) } returns
            Result.success(storedToken)

        val result = BearerTokenHelper.getAccessToken(context, forceRefresh = false)

        assertTrue(result.isSuccess)
        assertEquals("stored-access-token", result.getOrNull())
        coVerify(exactly = 1) { anyConstructed<ClientCredentialsFlow>().storeToken(storedToken) }
        coVerify(exactly = 0) { store.saveBearerJson(any()) }
    }

    @Test
    fun `forceRefresh bypasses the stored token and fetches a fresh one`() = runTest {
        coEvery { store.load() } returns identity
        coEvery { store.getBearerJson() } returns "stored-json"
        every { TokenData.fromJson("stored-json") } returns storedToken
        coEvery { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) } returns
            Result.success(newToken)

        val result = BearerTokenHelper.getAccessToken(context, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals("new-access-token", result.getOrNull())
        // Stored token must NOT be seeded when forcing a refresh.
        coVerify(exactly = 0) { anyConstructed<ClientCredentialsFlow>().storeToken(any()) }
        coVerify(exactly = 1) { store.saveBearerJson("new-json") }
    }

    @Test
    fun `fetches fresh token when no token is cached`() = runTest {
        coEvery { store.load() } returns identity
        coEvery { store.getBearerJson() } returns null
        coEvery { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) } returns
            Result.success(newToken)

        val result = BearerTokenHelper.getAccessToken(context, forceRefresh = false)

        assertTrue(result.isSuccess)
        assertEquals("new-access-token", result.getOrNull())
        coVerify(exactly = 0) { anyConstructed<ClientCredentialsFlow>().storeToken(any()) }
        coVerify(exactly = 1) { store.saveBearerJson("new-json") }
    }

    @Test
    fun `treats a malformed cached token as no cached token`() = runTest {
        coEvery { store.load() } returns identity
        coEvery { store.getBearerJson() } returns "not-json"
        every { TokenData.fromJson("not-json") } throws IllegalArgumentException("bad json")
        coEvery { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) } returns
            Result.success(newToken)

        val result = BearerTokenHelper.getAccessToken(context, forceRefresh = false)

        assertTrue(result.isSuccess)
        assertEquals("new-access-token", result.getOrNull())
        // Malformed cache -> storedToken is null -> nothing seeded.
        coVerify(exactly = 0) { anyConstructed<ClientCredentialsFlow>().storeToken(any()) }
        coVerify(exactly = 1) { store.saveBearerJson("new-json") }
    }

    @Test
    fun `propagates authentication failure and does not persist a token`() = runTest {
        coEvery { store.load() } returns identity
        coEvery { store.getBearerJson() } returns null
        val authError = RuntimeException("auth failed")
        coEvery { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) } returns
            Result.failure(authError)

        val result = BearerTokenHelper.getAccessToken(context, forceRefresh = false)

        assertTrue(result.isFailure)
        assertEquals(authError, result.exceptionOrNull())
        assertNull(result.getOrNull())
        coVerify(exactly = 0) { store.saveBearerJson(any()) }
    }

    @Test
    fun `requests the identity scopes when authenticating`() = runTest {
        coEvery { store.load() } returns identity
        coEvery { store.getBearerJson() } returns null
        coEvery { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) } returns
            Result.success(newToken)

        val result = BearerTokenHelper.getAccessToken(context)

        assertFalse(result.isFailure)
        coVerify(exactly = 1) {
            anyConstructed<ClientCredentialsFlow>().authenticate(any(), identity.oauthScope)
        }
    }

    // ---- getBearerToken ----

    @Test
    fun `getBearerToken prefixes the access token with the Bearer scheme`() = runTest {
        coEvery { store.load() } returns identity
        coEvery { store.getBearerJson() } returns null
        coEvery { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) } returns
            Result.success(newToken)

        val result = BearerTokenHelper.getBearerToken(context)

        assertTrue(result.isSuccess)
        assertEquals("Bearer new-access-token", result.getOrNull())
    }

    @Test
    fun `getBearerToken forwards forceRefresh and bypasses the stored token`() = runTest {
        coEvery { store.load() } returns identity
        coEvery { store.getBearerJson() } returns "stored-json"
        every { TokenData.fromJson("stored-json") } returns storedToken
        coEvery { anyConstructed<ClientCredentialsFlow>().authenticate(any(), any()) } returns
            Result.success(newToken)

        val result = BearerTokenHelper.getBearerToken(context, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals("Bearer new-access-token", result.getOrNull())
        // forceRefresh must propagate: stored token not seeded into the flow.
        coVerify(exactly = 0) { anyConstructed<ClientCredentialsFlow>().storeToken(any()) }
    }

    @Test
    fun `getBearerToken propagates failure from getAccessToken`() = runTest {
        coEvery { store.load() } returns null

        val result = BearerTokenHelper.getBearerToken(context)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("No identity available", result.exceptionOrNull()?.message)
        assertNull(result.getOrNull())
    }
}
