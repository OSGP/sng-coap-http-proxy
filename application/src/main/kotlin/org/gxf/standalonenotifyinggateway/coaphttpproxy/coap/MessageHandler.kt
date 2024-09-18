// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.coap

import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.exception.InvalidMessageException
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.validation.MessageValidator
import org.gxf.standalonenotifyinggateway.coaphttpproxy.domain.Message
import org.gxf.standalonenotifyinggateway.coaphttpproxy.http.HttpClient
import org.gxf.standalonenotifyinggateway.coaphttpproxy.logging.RemoteLogger

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

/**
 * @param httpClient
 * @param messageValidator
 * @param remoteLogger
 */
@Service
class MessageHandler(
    private val httpClient: HttpClient,
    private val messageValidator: MessageValidator,
    private val remoteLogger: RemoteLogger,
) {
    private val logger = KotlinLogging.logger {}
    private val cborMapper = CBORMapper()

    @Throws(HttpClientErrorException::class, HttpServerErrorException::class)
    fun handlePost(
        id: String,
        payload: ByteArray,
    ): ResponseEntity<String> {
        val parsedJson = cborMapper.readTree(payload)
        val message = Message(id, parsedJson)

        logger.trace { "Handling post, for message: $message." }

        if (messageValidator.isValid(message)) {
            val response = httpClient.postMessage(message)
            logger.debug { "Handled post" }
            return response
        }

        remoteLogger.error { "Received invalid message: $message" }
        throw InvalidMessageException("Received invalid message: $message")
    }
}
