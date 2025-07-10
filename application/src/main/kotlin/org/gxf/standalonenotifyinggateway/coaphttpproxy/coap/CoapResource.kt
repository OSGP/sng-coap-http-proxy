// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.coap

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.codec.binary.Hex
import org.eclipse.californium.core.coap.CoAP.ResponseCode
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.elements.util.DatagramWriter
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.configuration.properties.CoapProperties
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.exception.CoapExchangeException
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.exception.InvalidMessageException
import org.gxf.standalonenotifyinggateway.coaphttpproxy.logging.RemoteLogger
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.eclipse.californium.core.CoapResource as CaliforniumCoapResource

@Component
class CoapResource(
    private val coapProps: CoapProperties,
    private val messageHandler: MessageHandler,
    private val remoteLogger: RemoteLogger,
) : CaliforniumCoapResource(coapProps.path) {
    private val logger = KotlinLogging.logger {}

    init {
        logger.info { "Initializing Coap resource for path: ${coapProps.path}" }
    }

    override fun handlePOST(coapExchange: CoapExchange) {
        val deviceId = getIdFromRequestContext(coapExchange)
        logger.debug { "Handling CoAP POST: $coapExchange for device $deviceId" }
        try {
            logger.debug { "Received CBOR: ${Hex.encodeHexString(coapExchange.requestPayload)}" }
            if (deviceId == null) {
                throw CoapExchangeException("Device id from coap exchange is null")
            }
            val response = messageHandler.handlePost(deviceId, coapExchange.requestPayload)
            // Intentional exception throwing when the response is null or when there is no body
            writeResponse(coapExchange, response.body!!, deviceId)
        } catch (e: Exception) {
            logger.warn { "Error occurred while handling post to device service for device $deviceId" }
            when (e) {
                is HttpClientErrorException -> handleError(coapExchange, ResponseCode.BAD_REQUEST, e, deviceId)
                is HttpServerErrorException ->
                    handleError(coapExchange, ResponseCode.INTERNAL_SERVER_ERROR, e, deviceId)
                is InvalidMessageException -> handleInvalidMessage(coapExchange, e, deviceId)
                else -> handleUnexpectedError(coapExchange, e, deviceId)
            }
        }
    }

    private fun getIdFromRequestContext(coapExchange: CoapExchange): String? {
        try {
            return coapExchange.advanced().currentRequest.sourceContext.peerIdentity.name
        } catch (e: Exception) {
            logger.error(e) { "Error occurred while retrieving deviceId from coap exchange" }
            return null
        }
    }

    private fun writeResponse(coapExchange: CoapExchange, body: String, deviceId: String) {
        logger.info { "Sending successful response for device $deviceId" }
        coapExchange.setMaxAge(1)

        coapExchange.setETag(DatagramWriter(4).apply { write(body.hashCode(), 32) }.toByteArray())

        coapExchange.respond(ResponseCode.CONTENT, body)
    }

    private fun handleError(
        coapExchange: CoapExchange,
        responseCode: ResponseCode,
        exception: Exception,
        deviceId: String?,
    ) {
        remoteLogger.error(exception) { "Sending ${responseCode.name} as response to device $deviceId" }
        coapExchange.respond(responseCode)
    }

    private fun handleUnexpectedError(coapExchange: CoapExchange, exception: Exception, deviceId: String?) {
        remoteLogger.error(exception) { "Unexpected error occurred in handling the CoAP message for device $deviceId" }
        coapExchange.respond(ResponseCode.BAD_GATEWAY)
    }

    private fun handleInvalidMessage(coapExchange: CoapExchange, exception: Exception, deviceId: String?) {
        val responseCode = ResponseCode.BAD_GATEWAY
        remoteLogger.error(exception) {
            "Sending ${responseCode.name} as response to the device $deviceId because of an invalid message"
        }
        coapExchange.respond(responseCode)
    }
}
