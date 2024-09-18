// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy

import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.MessageHandler
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.exception.InvalidMessageException
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.validation.MessageValidator
import org.gxf.standalonenotifyinggateway.coaphttpproxy.domain.Message
import org.gxf.standalonenotifyinggateway.coaphttpproxy.http.HttpClient
import org.gxf.standalonenotifyinggateway.coaphttpproxy.logging.RemoteLogger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check

@ExtendWith(MockitoExtension::class)
class MessageHandlerTest {
    private val testJsonNode = ObjectMapper().readTree("{\"ID\": 12345}")
    private val testCbor = CBORMapper().writeValueAsBytes(testJsonNode)

    @Mock
    private lateinit var httpClient: HttpClient

    @Mock
    private lateinit var messageValidator: MessageValidator

    @Mock
    private lateinit var remoteLogger: RemoteLogger

    @InjectMocks
    private lateinit var messageHandler: MessageHandler

    @Test
    fun shouldCallRemoteLoggerWhenMessageIsInvalid() {
        `when`(messageValidator.isValid(any<Message>())).thenReturn(false)

        val thrownException = catchThrowable {
            messageHandler.handlePost("12345", testCbor)
            verify(remoteLogger).error(any())
        }

        assertThat(thrownException).isInstanceOf(InvalidMessageException::class.java)
    }

    @Test
    fun callHttpClientWhenMessageIsValid() {
        val message = Message("12345", testJsonNode)

        `when`(messageValidator.isValid(any<Message>())).thenReturn(true)

        messageHandler.handlePost("12345", testCbor)

        verify(httpClient)
            .postMessage(check { assertThat(it).usingRecursiveComparison().isEqualTo(message) })
    }
}
