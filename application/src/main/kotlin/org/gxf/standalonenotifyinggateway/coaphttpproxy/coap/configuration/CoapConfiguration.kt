// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.configuration

import org.eclipse.californium.core.config.CoapConfig
import org.eclipse.californium.core.network.CoapEndpoint
import org.eclipse.californium.elements.config.CertificateAuthenticationMode
import org.eclipse.californium.elements.config.SystemConfig
import org.eclipse.californium.elements.config.TcpConfig
import org.eclipse.californium.elements.config.UdpConfig
import org.eclipse.californium.scandium.DTLSConnector
import org.eclipse.californium.scandium.MdcConnectionListener
import org.eclipse.californium.scandium.config.DtlsConfig
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole
import org.eclipse.californium.scandium.config.DtlsConnectorConfig
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedPskStore
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.configuration.properties.CoapProperties
import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.configuration.properties.UdpProperties
import org.springframework.context.annotation.Bean
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import org.eclipse.californium.elements.config.Configuration as CaliforniumConfiguration
import org.springframework.context.annotation.Configuration as SpringConfiguration

@SpringConfiguration
class CoapConfiguration(private val coapProps: CoapProperties, private val udpProps: UdpProperties) {

    init {
        DtlsConfig.register()
        CoapConfig.register()
        UdpConfig.register()
        TcpConfig.register()
    }

    @Bean
    fun serverConfiguration(): CaliforniumConfiguration = CaliforniumConfiguration.createStandardWithoutFile().apply {
        updateCoapConfigFromProperties()
        updateUdpConfigFromProperties()
        updateDtlsConfig()
    }

    private fun CaliforniumConfiguration.updateCoapConfigFromProperties() {
        set(CoapConfig.MAX_ACTIVE_PEERS, coapProps.maxActivePeers)
        set(CoapConfig.MAX_RESOURCE_BODY_SIZE, coapProps.maxResourceBodySize)
        set(CoapConfig.MAX_MESSAGE_SIZE, coapProps.maxMessageSize)
        set(CoapConfig.PREFERRED_BLOCK_SIZE, coapProps.preferredBlockSize)
        set(CoapConfig.DEDUPLICATOR, coapProps.deduplicator)
        set(CoapConfig.COAP_SECURE_PORT, coapProps.coapsPort)
        set(CoapConfig.MAX_PEER_INACTIVITY_PERIOD, coapProps.maxPeerInactivityPeriod.seconds, TimeUnit.SECONDS)
    }

    private fun CaliforniumConfiguration.updateUdpConfigFromProperties() {
        set(UdpConfig.UDP_RECEIVE_BUFFER_SIZE, udpProps.udpReceiveBufferSize)
        set(UdpConfig.UDP_SEND_BUFFER_SIZE, udpProps.udpSendBufferSize)
        set(SystemConfig.HEALTH_STATUS_INTERVAL, udpProps.healthStatusInterval.seconds, TimeUnit.SECONDS)
    }

    private fun CaliforniumConfiguration.updateDtlsConfig() {
        set(DtlsConfig.DTLS_ROLE, DtlsRole.SERVER_ONLY)
        set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, false)
        set(DtlsConfig.DTLS_PRESELECTED_CIPHER_SUITES, coapProps.cipherSuites)
        set(DtlsConfig.DTLS_CIPHER_SUITES, coapProps.cipherSuites)
        set(DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE, CertificateAuthenticationMode.NONE)
    }

    @Bean
    fun coapEndpoint(config: CaliforniumConfiguration, dtlsConnector: DTLSConnector): CoapEndpoint =
        CoapEndpoint.Builder().setConfiguration(config).setConnector(dtlsConnector).build()

    @Bean
    fun dtlsConnector(config: CaliforniumConfiguration, remotePskStore: AdvancedPskStore) = DTLSConnector(
        DtlsConnectorConfig.builder(config)
            .setAddress(InetSocketAddress(coapProps.coapsPort))
            .setAdvancedPskStore(remotePskStore)
            .setConnectionListener(MdcConnectionListener())
            .build(),
    )
}
