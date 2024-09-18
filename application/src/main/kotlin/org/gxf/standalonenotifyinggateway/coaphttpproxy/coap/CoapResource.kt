// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.coap

import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.configuration.properties.CoapProperties
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.exception.CoapExchangeException
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.exception.InvalidMessageException
import org.gxf.standalonenotifyinggateway.coaphttpproxy.logging.RemoteLogger

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.codec.binary.Hex
import org.eclipse.californium.core.CoapResource as CaliforniumCoapResource
import org.eclipse.californium.core.coap.CoAP.ResponseCode
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.elements.util.DatagramWriter
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

/**
 * @param coapProps
 * @param messageHandler
 * @param remoteLogger
 */
@Component
class CoapResource(
    private val coapProps: CoapProperties,
    private val messageHandler: MessageHandler,
    private val remoteLogger: RemoteLogger
) : CaliforniumCoapResource(coapProps.path) {
    private val logger = KotlinLogging.logger {}

    init {
        logger.info { "Initializing Coap resource for path: ${coapProps.path}" }
    }

    @Suppress("FUNCTION_NAME_INCORRECT_CASE")
    override fun handlePOST(coapExchange: CoapExchange) {
        val deviceId = getIdFromRequestContext(coapExchange)
        logger.debug { "Handling CoAP POST: $coapExchange for device $deviceId" }
        try {
            logger.debug { "Received CBOR: ${Hex.encodeHexString(coapExchange.requestPayload)}" }
            deviceId ?: throw CoapExchangeException("Device id from coap exchange is null")
            val response = messageHandler.handlePost(deviceId, coapExchange.requestPayload)
            // Intentional exception throwing when the response is null or when there is no body
            writeResponse(coapExchange, response?.body!!, deviceId)
        } catch (e: Exception) {
            logger.warn {
                "Error occurred while handling post to device service for device $deviceId"
            }
            when (e) {
                is HttpClientErrorException -> handleError(coapExchange, ResponseCode.BAD_REQUEST)
                is HttpServerErrorException ->
                    handleError(coapExchange, ResponseCode.INTERNAL_SERVER_ERROR)
                is InvalidMessageException -> handleInvalidMessage(coapExchange)
                else -> handleUnexpectedError(coapExchange, e)
            }
        }
    }

    private fun getIdFromRequestContext(coapExchange: CoapExchange): String? {
        try {
            return coapExchange.advanced()
                .currentRequest
                .sourceContext
                .peerIdentity
                .name
        } catch (e: Exception) {
            logger.error(e) { "Error occurred while retrieving deviceId from coap exchange" }
            return null
        }
    }

    private fun writeResponse(
        coapExchange: CoapExchange,
        body: String,
        deviceId: String,
    ) {
        logger.info { "Sending successful response for device $deviceId" }
        coapExchange.setMaxAge(1)

        coapExchange.setETag(DatagramWriter(4).apply { write(body.hashCode(), 32) }.toByteArray())

        coapExchange.respond(ResponseCode.CONTENT, body)
    }

    private fun handleError(
        coapExchange: CoapExchange,
        responseCode: ResponseCode,
    ) {
        coapExchange.respond(responseCode)
    }

    private fun handleUnexpectedError(
        coapExchange: CoapExchange,
        exception: Exception,
    ) {
        remoteLogger.error(exception) { "Unexpected error occurred" }
        coapExchange.respond(ResponseCode.BAD_GATEWAY)
    }

    private fun handleInvalidMessage(coapExchange: CoapExchange) {
        coapExchange.respond(ResponseCode.BAD_GATEWAY)
    }
}
