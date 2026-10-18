package com.aritxonly.deadliner.ai

import com.deadliner.domain.ai.model.*
import com.deadliner.domain.ai.port.MemoryStorePort
import com.deadliner.domain.memory.*
import com.deadliner.lifi.agent.ReactAgent
import com.deadliner.lifi.llm.KtorLlmClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import kotlin.concurrent.thread

/** Exercises the Android actual HTTP engine without real credentials. */
class LifiTransportCompatibilityTest {
    @Test fun androidHttpEngineCompletesPlainChatWithoutRouting() = runBlocking {
        withServer("""{"choices":[{"message":{"role":"assistant","content":"你好"}}]}""") { config ->
            val transport = KtorLlmClient(config)
            try {
                assertEquals("你好", ReactAgent(config, transport, EmptyMemory()).processMessage("你好").chatResponse)
            } finally { transport.close() }
        }
    }

    @Test fun androidHttpEngineAcceptsNullContentWithNativeToolCall() = runBlocking {
        withServer("""{"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[{"id":"server-call","type":"function","function":{"name":"read_tasks","arguments":"{}"}}]}}]}""") { config ->
            val transport = KtorLlmClient(config)
            try {
                assertTrue(ReactAgent(config.copy(nativeToolCalling = true), transport, EmptyMemory()).processMessage("查询任务").toolCalls.single() is ToolCall.ReadTasks)
            } finally { transport.close() }
        }
    }

    @Test fun actualAndroidEngineRetries503And429BeforeSuccessfulReply() = runBlocking {
        withServer("""{"choices":[{"message":{"role":"assistant","content":"重试成功"}}]}""", listOf(503, 429, 200)) { config ->
            val transport = KtorLlmClient(config)
            try {
                assertEquals("重试成功", ReactAgent(config, transport, EmptyMemory()).processMessage("你好").chatResponse)
            } finally { transport.close() }
        }
    }

    @Test fun actualAndroidEngineDoesNotRetryInvalidCredentials() = runBlocking {
        withServer("{\"error\":\"unauthorized\"}", listOf(401)) { config ->
            val transport = KtorLlmClient(config)
            try {
                try { ReactAgent(config, transport, EmptyMemory()).processMessage("你好"); fail("Expected 401") }
                catch (error: io.ktor.client.plugins.ClientRequestException) { assertEquals(401, error.response.status.value) }
            } finally { transport.close() }
        }
    }

    private suspend fun withServer(response: String, statuses: List<Int> = listOf(200), block: suspend (AIConfig) -> Unit) {
        ServerSocket(0).use { server ->
            server.soTimeout = 10_000
            var failure: Throwable? = null
            val worker = thread(name = "lifi-http-fixture", isDaemon = true) {
                try {
                    for (status in statuses) server.accept().use { socket ->
                        socket.soTimeout = 10_000
                        val input = socket.getInputStream().buffered()
                        val headerBytes = java.io.ByteArrayOutputStream()
                        while (true) {
                            val byte = input.read()
                            check(byte >= 0)
                            headerBytes.write(byte)
                            if (headerBytes.toString("US-ASCII").endsWith("\r\n\r\n")) break
                            check(headerBytes.size() <= 16384)
                        }
                        val headers = headerBytes.toString("US-ASCII").split("\r\n")
                        assertTrue(headers.first().startsWith("POST /v1/chat/completions "))
                        val contentLength = headers.first { it.startsWith("Content-Length:", ignoreCase = true) }.substringAfter(':').trim().toInt()
                        val body = input.readNBytes(contentLength)
                        assertEquals(contentLength, body.size)
                        val wire = com.google.gson.JsonParser.parseString(body.toString(Charsets.UTF_8)).asJsonObject
                        assertEquals("fixture-model", wire.get("model").asString)
                        wire.get("tools")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { definition ->
                            assertEquals("function", definition.asJsonObject.get("type").asString)
                        }
                        val bytes = response.toByteArray(Charsets.UTF_8)
                        val header = "HTTP/1.1 $status Fixture\r\nRetry-After: 0\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                        socket.getOutputStream().write(header.toByteArray(Charsets.US_ASCII))
                        socket.getOutputStream().write(bytes)
                        socket.getOutputStream().flush()
                    }
                } catch (error: Throwable) { failure = error }
            }
            try {
                block(AIConfig("fixture-key", "http://127.0.0.1:${server.localPort}", "fixture-model", "android", "Asia/Shanghai"))
            } finally { worker.join(12_000); assertFalse("fixture leaked", worker.isAlive) }
            failure?.let { throw AssertionError("fixture failed", it) }
        }
    }
}

private class EmptyMemory : MemoryStorePort {
    override suspend fun saveMemory(content: String, category: String) = Unit
    override suspend fun getAllFragments(): List<MemoryFragment> = emptyList()
    override suspend fun saveUserProfile(profile: String) = Unit
    override suspend fun getUserProfile() = ""
    override suspend fun getLongTermContext() = ""
    override suspend fun replaceSnapshot(snapshot: MemorySnapshot) = Unit
    override suspend fun currentSnapshot() = MemorySnapshot(0, emptyList())
    override suspend fun appendConversationTurn(role: String, content: String) = Unit
    override suspend fun getRecentConversationContext(limit: Int) = ""
}
