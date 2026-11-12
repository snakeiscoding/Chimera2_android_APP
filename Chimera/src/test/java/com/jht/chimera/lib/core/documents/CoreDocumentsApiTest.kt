package com.jht.chimera.lib.core.documents

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class CoreDocumentsApiTest {
    @Test
    fun `authenticate should return success when API responds correctly`() = runTest {
        // Mock the machine-auth endpoint response
        val api = createMockClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertTrue(request.url.toString().contains("machine-auth"))
            respond(
                content = """{"token":"valid_token"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        // Validate TOTP token is retrieved successfully
        val response = api.authenticate(environment = 1, secret = "test-secret")
        api.close()
        assertEquals("valid_token", response.token)
    }

    @Test
    fun `authenticate should throw exception on HTTP error`() = runTest {
        // Mock unauthorized response
        val api = createMockClient { respondError(HttpStatusCode.Unauthorized) }

        // Validate exception is thrown due to response
        try {
            api.authenticate(environment = 1, secret = "test-secret")
            fail("Expected CoreDocumentsApiException to be thrown")
        } catch (_: CoreDocumentsApi.CoreDocumentsApiException) {}
        finally {
            api.close()
        }
    }

    @Test
    fun `getAllDocuments should return success when API responds correctly`() = runTest {
        // Mock the document endpoint response
        val api = createMockClient { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertTrue(request.url.toString().contains("/document/123"))
            respond(
                content = """{"tv_channels":{}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        // Validate documents are retrieved successfully
        val documents = api.getAllDocuments(token = "valid_token", environment = 1, clubId = 123)
        api.close()
        assertNotNull(documents)
    }

    @Test
    fun `getAllDocuments should throw exception on HTTP error`() = runTest {
        // Mock internal server error response
        val api = createMockClient { respondError(HttpStatusCode.InternalServerError) }

        // Validate exception is thrown due to response
        try {
            api.getAllDocuments(token = "valid_token", environment = 1, clubId = 123)
            fail("Expected CoreDocumentsApiException to be thrown")
        } catch (_: CoreDocumentsApi.CoreDocumentsApiException) {}
        finally {
            api.close()
        }
    }

    @Test
    fun `getDocumentMetadata should return success with correct metadata`() = runTest {
        // Mock the document metadata response
        val api = createMockClient { _ ->
            respond(
                content = "",
                headers = headersOf(
                    "Content-Type" to listOf("application/pdf"),
                    "Content-Length" to listOf("2048"),
                    "Last-Modified" to listOf("Wed, 21 Oct 2023 07:28:00 GMT"),
                    "ETag" to listOf("\"abc123\"")
                )
            )
        }

        // Validate metadata is retrieved successfully
        val metadata = api.getDocumentMetadata("https://example.com/document")
        api.close()
        assertEquals("application/pdf", metadata.contentType)
        assertEquals(2048L, metadata.contentLength)
        assertEquals("Wed, 21 Oct 2023 07:28:00 GMT", metadata.lastModified)
        assertEquals("abc123", metadata.etag)
    }

    @Test
    fun `getDocumentMetadata should handle missing headers`() = runTest {
        // Mock the document metadata response with missing headers
        val api = createMockClient { _ -> respond(content = "", headers = headersOf()) }

        // Validate metadata is retrieved successfully
        val metadata = api.getDocumentMetadata("https://example.com/document")
        api.close()
        assertNull(metadata.contentType)
        assertNull(metadata.contentLength)
        assertNull(metadata.lastModified)
        assertNull(metadata.etag)
    }

    @Test
    fun `downloadDocument should succeed when response is OK`() = runTest {
        // Mock successful download
        val tempFile = File.createTempFile("test", ".tmp")
        val dummyContent = "dummy content".toByteArray()
        val api = createMockClient { _ ->
            respond(
                content = dummyContent,
                headers = headersOf(HttpHeaders.ContentType, "application/octet-stream")
            )
        }

        // Validate that the number of bytes downloaded matches the downloaded content size.
        val bytesDownloaded = api.downloadDocument("https://example.com/document", tempFile)
        api.close()
        assertEquals(dummyContent.size.toLong(), bytesDownloaded)
        assertEquals(dummyContent.size.toLong(), tempFile.length())

        // Clean up the downloaded file.
        tempFile.delete()
    }

    @Test
    fun `downloadDocument should throw exception on error response`() = runTest {
        // Mock download error
        val tempFile = File.createTempFile("test", ".tmp")
        val api = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }

        // Validate exception is thrown due to response
        try {
            api.downloadDocument("https://example.com/document", tempFile)
            fail("Expected CoreDocumentsApiException to be thrown")
        } catch (_: CoreDocumentsApi.CoreDocumentsApiException) {}
        finally {
            api.close()
        }

        // Clean up the downloaded file
        tempFile.delete()
    }

    @Test
    fun `uploadDocument should succeed when API returns success`() = runTest {
        // Mock successful upload
        val tempFile = File.createTempFile("test", ".pdf")
        val dummyContent = "dummy content for upload".toByteArray()
        tempFile.writeBytes(dummyContent)
        val api = createMockClient { _ ->
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        // Validate that the number of bytes uploaded matches the upload content size.
        val bytesUploaded = api.uploadDocument(
            token = "valid_token",
            environment = 1,
            clubId = 123,
            documentType = "report",
            documentSubType = "pdf",
            document = tempFile,
        )
        api.close()
        assertEquals(dummyContent.size.toLong(), bytesUploaded)
        assertEquals(tempFile.length(), bytesUploaded)

        // Clean up the uploaded file
        tempFile.delete()
    }

    @Test
    fun `uploadDocument should throw exception when API returns error`() = runTest {
        // Mock upload error
        val tempFile = File.createTempFile("test", ".pdf")
        tempFile.writeBytes("dummy content for upload".toByteArray())
        val api = createMockClient { _ -> respondError(HttpStatusCode.InternalServerError) }

        // Validate exception is thrown due to response
        try {
            api.uploadDocument(
                token = "valid_token",
                environment = 1,
                clubId = 123,
                documentType = "report",
                documentSubType = "pdf",
                document = tempFile
            )
            fail("Expected CoreDocumentsApiException to be thrown")
        } catch (_: CoreDocumentsApi.CoreDocumentsApiException) {}
        finally {
            api.close()
        }

        // Clean up the uploaded file
        tempFile.delete()
    }

    // Helper method to create a mock client with a custom handler.
    private fun createMockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): CoreDocumentsApi {
        val mockEngine = MockEngine { request -> handler(request) }
        return CoreDocumentsApi(engine = mockEngine)
    }
}
