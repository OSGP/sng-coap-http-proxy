// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy

import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.config.CoapConfig
import org.eclipse.californium.core.network.CoapEndpoint
import org.eclipse.californium.elements.config.Configuration
import org.eclipse.californium.elements.config.TcpConfig
import org.eclipse.californium.elements.config.UdpConfig
import org.eclipse.californium.scandium.DTLSConnector
import org.eclipse.californium.scandium.MdcConnectionListener
import org.eclipse.californium.scandium.config.DtlsConfig
import org.eclipse.californium.scandium.config.DtlsConnectorConfig
import org.eclipse.californium.scandium.dtls.ProtocolVersion
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedSinglePskStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import java.net.InetSocketAddress

@Component
class IntegrationTestCoapClient {
    @Value("\${config.coap.coaps-port}") private lateinit var coapsPort: Number
    @Value("\${config.coap.path}") private lateinit var path: String
    @Value("\${config.psk.default-id}") private lateinit var defaultId: String
    @Value("\${config.psk.default-key}") private lateinit var defaultKey: String
    @Value("\${config.coap.cipher-suites}") private lateinit var cipherSuites: List<CipherSuite>

    init {
        DtlsConfig.register()
        CoapConfig.register()
        UdpConfig.register()
        TcpConfig.register()
    }

    fun getClient(): CoapClient {
        val uri = this.getUri()
        val coapClient = CoapClient(uri)
        val dtlsConnector = this.createDtlsConnector()
        val endpoint =
            CoapEndpoint.Builder()
                .setConfiguration(createConfiguration())
                .setConnector(dtlsConnector)
                .build()
        coapClient.setEndpoint(endpoint)
        return coapClient
    }

    private fun createConfiguration(): Configuration = Configuration.getStandard()
        .set(CoapConfig.COAP_SECURE_PORT, coapsPort.toInt())
        .set(DtlsConfig.DTLS_ROLE, DtlsConfig.DtlsRole.CLIENT_ONLY)
        .set(DtlsConfig.DTLS_CIPHER_SUITES, cipherSuites)

    private fun getUri(): String =
        String.format("%s://%s:%d/%s", "coaps", "localhost", coapsPort.toInt(), path)

    private fun createDtlsConnector(): DTLSConnector {
        val address = InetSocketAddress(0)
        val pskStore = createPskStore()
        val dtlsBuilder =
            DtlsConnectorConfig.builder(createConfiguration())
                .setAddress(address)
                .setAdvancedPskStore(pskStore)
                .setConnectionListener(MdcConnectionListener())
                .setProtocolVersionForHelloVerifyRequests(ProtocolVersion.VERSION_DTLS_1_2)
                .build()
        return DTLSConnector(dtlsBuilder)
    }

    private fun createPskStore(): AdvancedSinglePskStore = AdvancedSinglePskStore(defaultId, defaultKey.toByteArray())
}
