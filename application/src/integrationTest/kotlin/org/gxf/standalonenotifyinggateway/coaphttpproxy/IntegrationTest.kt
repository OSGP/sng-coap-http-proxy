// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import com.github.tomakehurst.wiremock.http.Fault
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.eclipse.californium.core.coap.CoAP
import org.eclipse.californium.core.coap.MediaTypeRegistry
import org.eclipse.californium.core.coap.Request
import org.gxf.standalonenotifyinggateway.coaphttpproxy.configuration.PskStubProperties
import org.gxf.standalonenotifyinggateway.coaphttpproxy.http.HttpClient
import org.gxf.standalonenotifyinggateway.coaphttpproxy.http.configuration.properties.HttpProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.net.URI
import java.time.Duration

@Import(IntegrationTestCoapClient::class)
@EnableConfigurationProperties(PskStubProperties::class, HttpProperties::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {
    @Autowired private lateinit var coapClient: IntegrationTestCoapClient

    @Autowired private lateinit var pskStubProperties: PskStubProperties

    @Autowired private lateinit var httpProperties: HttpProperties

    private lateinit var wiremock: WireMockServer

    private val wiremockStubOk = post(urlPathTemplate("${HttpClient.MESSAGE_PATH}/{id}")).willReturn(ok("0"))
    private val wiremockStubError =
        post(urlPathTemplate("${HttpClient.MESSAGE_PATH}/{id}")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
    private val wiremockStubInternalServerError =
        post(urlPathTemplate("${HttpClient.MESSAGE_PATH}/{id}")).willReturn(serverError())
    private val wiremockStubErrorEndpoint =
        post(urlPathTemplate(HttpClient.ERROR_PATH)).willReturn(aResponse().withStatus(200))
    private lateinit var jsonNode: JsonNode

    @BeforeEach
    fun beforeEach() {
        val wiremockStubPsk = get(urlPathTemplate(HttpClient.PSK_PATH)).willReturn(ok(pskStubProperties.defaultKey))

        jsonNode =
            ObjectMapper()
                .readTree(
                    """ 
            {
                "ID": "${pskStubProperties.defaultId}",
                "URC": [
                    "PSK:SET",
                    {
                        "DL": "!PSK:umU6KJ4g7Ye5ZU6o:4a3cfdd487298e2f048ebfd703a1da4800c18f2167b62192cf7dc9fd6cc4bcd3;PSK:umU6KJ4g7Ye5ZU6o:4a3cfdd487298e2f048ebfd703a1da4800c18f2167b62192cf7dc9fd6cc4bcd3:SET"
                    }
                ]
            }
            """,
                )

        val url = URI(httpProperties.url).toURL()
        wiremock = WireMockServer(url.port)
        wiremock.stubFor(wiremockStubErrorEndpoint)
        wiremock.stubFor(wiremockStubPsk)
        wiremock.stubFor(wiremockStubOk)
        wiremock.start()
    }

    @AfterEach
    fun afterEach() {
        wiremock.stop()
    }

    @Test
    fun shouldForwardCoapMessageToHttp() {
        val coapClient = coapClient.getClient()

        val request =
            Request.newPost()
                .apply { options.setContentFormat(MediaTypeRegistry.APPLICATION_CBOR) }
                .setPayload(CBORMapper().writeValueAsBytes(jsonNode))

        coapClient.advanced(request)

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val wiremockRequests =
                wiremock.findAll(postRequestedFor(urlPathTemplate("${HttpClient.MESSAGE_PATH}/{id}")))
            assertThat(wiremockRequests).hasSize(1)
            assertThat(ObjectMapper().readTree(wiremockRequests.first().bodyAsString)).isEqualTo(jsonNode)
        }
    }

    // When a error occurs should not forward the coap message to the next service
    // Instead it should call the error endpoint with the error
    @Test
    fun shouldNotForwardCoapMessageToHttpWhenTheIdsDontMatch() {
        val coapClient = coapClient.getClient()

        val jsonNodeWithInvalidId = (jsonNode as ObjectNode).put("ID", jsonNode.findValue("ID").asText().plus("1"))

        val request =
            Request.newPost()
                .apply { options.setContentFormat(MediaTypeRegistry.APPLICATION_CBOR) }
                .setPayload(CBORMapper().writeValueAsBytes(jsonNodeWithInvalidId))

        val response = coapClient.advanced(request)

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val wiremockRequestsSng =
                wiremock.findAll(postRequestedFor(urlPathTemplate("${HttpClient.MESSAGE_PATH}/{id}")))
            val wiremockRequestsError = wiremock.findAll(postRequestedFor(urlPathTemplate(HttpClient.ERROR_PATH)))

            assertThat(wiremockRequestsSng).hasSize(0)
            assertThat(wiremockRequestsError).hasSize(2)
            assertThat(response.code).isEqualTo(CoAP.ResponseCode.BAD_GATEWAY)
        }
    }

    @Test
    fun shouldReturnBadGatewayWhenHttpClientReturnsUnexpectedError() {
        wiremock.stubFor(wiremockStubError)
        val coapClient = coapClient.getClient()

        val request =
            Request.newPost()
                .apply { options.setContentFormat(MediaTypeRegistry.APPLICATION_CBOR) }
                .setPayload(CBORMapper().writeValueAsBytes(jsonNode))

        val response = coapClient.advanced(request)

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val wiremockRequests =
                wiremock.findAll(postRequestedFor(urlPathTemplate("${HttpClient.MESSAGE_PATH}/{id}")))
            val wiremockRequestsErrorEndpoint =
                wiremock.findAll(postRequestedFor(urlPathEqualTo(HttpClient.ERROR_PATH)))

            assertThat(wiremockRequestsErrorEndpoint).hasSize(1)
            assertThat(wiremockRequests).hasSize(1)
            assertThat(response.code).isEqualTo(CoAP.ResponseCode.BAD_GATEWAY)
        }
    }

    @Test
    fun shouldForwardInternalServiceErrorToDeviceIfHttp500IsReceived() {
        wiremock.stubFor(wiremockStubInternalServerError)
        val coapClient = coapClient.getClient()

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val wiremockRequests =
                wiremock.findAll(postRequestedFor(urlPathTemplate("${HttpClient.MESSAGE_PATH}/{id}")))
            val request = createRequest(jsonNode)
            val response = coapClient.advanced(request)
            assertThat(wiremockRequests).hasSize(1)
            assertThat(response.code).isEqualTo(CoAP.ResponseCode.INTERNAL_SERVER_ERROR)
        }
    }

    private fun createRequest(jsonNode: JsonNode): Request {
        val payload = CBORMapper().writeValueAsBytes(jsonNode)

        return Request.newPost()
            .apply { options.setContentFormat(MediaTypeRegistry.APPLICATION_CBOR) }
            .setPayload(payload)
    }
}
