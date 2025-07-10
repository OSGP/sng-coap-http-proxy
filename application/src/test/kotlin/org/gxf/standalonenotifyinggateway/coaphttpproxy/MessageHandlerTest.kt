// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.MessageHandler
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.exception.InvalidMessageException
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.validation.MessageValidator
import org.gxf.standalonenotifyinggateway.coaphttpproxy.domain.Message
import org.gxf.standalonenotifyinggateway.coaphttpproxy.http.HttpClient
import org.gxf.standalonenotifyinggateway.coaphttpproxy.logging.RemoteLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity

@ExtendWith(MockKExtension::class)
class MessageHandlerTest {
    @MockK private lateinit var httpClient: HttpClient

    @MockK private lateinit var messageValidator: MessageValidator

    @MockK(relaxed = true)
    private lateinit var remoteLogger: RemoteLogger

    @InjectMockKs private lateinit var messageHandler: MessageHandler

    private val testJsonNode = ObjectMapper().readTree("{\"ID\": 12345}")
    private val testCbor = CBORMapper().writeValueAsBytes(testJsonNode)

    @Test
    fun shouldCallRemoteLoggerWhenMessageIsInvalid() {
        every { messageValidator.isValid(any()) } returns false

        val thrownException = catchThrowable {
            messageHandler.handlePost("12345", testCbor)

            verify { remoteLogger.error(any()) }
        }

        assertThat(thrownException).isInstanceOf(InvalidMessageException::class.java)
    }

    @Test
    fun callHttpClientWhenMessageIsValid() {
        every { messageValidator.isValid(any()) } returns true
        every { httpClient.postMessage(any()) } returns ResponseEntity.ok("OK")

        messageHandler.handlePost("12345", testCbor)

        verify { httpClient.postMessage(Message("12345", testJsonNode)) }
    }
}
