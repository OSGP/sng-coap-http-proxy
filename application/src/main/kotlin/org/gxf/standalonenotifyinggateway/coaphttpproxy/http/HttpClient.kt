// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.http

import org.gxf.standalonenotifyinggateway.coaphttpproxy.domain.Message

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

/**
 * @param webClient
 */
@Component
class HttpClient(private val webClient: RestClient) {
    private val logger = KotlinLogging.logger {}

    @Throws(HttpClientErrorException::class, HttpServerErrorException::class)
    fun postMessage(message: Message): ResponseEntity<String> {
        val (id, payload) = message

        val urc = getUrcFromMessage(payload)
        logger.debug { "Posting message with id $id, body: $payload and urc $urc" }

        try {
            val response = executeRequest(id, payload.toString())
            logger.debug { "Posted message with id $id, resulting response: $response" }
            return response
        } catch (e: Exception) {
            logger.warn { "Error received while posting message with id $id and $urc" }
            throw e
        }
    }

    private fun getUrcFromMessage(body: JsonNode) =
        body["URC"].filter { it.isTextual }.map { it.asText() }.firstOrNull()

    @Throws(HttpClientErrorException::class, HttpServerErrorException::class)
    private fun executeRequest(
        id: String,
        body: String,
    ): ResponseEntity<String> =
        webClient.post()
            .uri("$MESSAGE_PATH/$id")
            .body(body)
            .retrieve()
            .toEntity<String>()
    companion object {
        const val ERROR_PATH = "/error"
        const val MESSAGE_PATH = "/sng"
        const val PSK_PATH = "/psk"
    }
}
