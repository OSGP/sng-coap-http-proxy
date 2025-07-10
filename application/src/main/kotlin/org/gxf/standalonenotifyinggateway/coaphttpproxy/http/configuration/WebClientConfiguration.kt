// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.http.configuration

import org.gxf.standalonenotifyinggateway.coaphttpproxy.http.configuration.properties.HttpProperties
import org.springframework.boot.autoconfigure.web.client.RestClientSsl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class WebClientConfiguration(private val httpProps: HttpProperties) {

    @Bean
    fun webClient(webClientBuilder: RestClient.Builder, webClientSsl: RestClientSsl): RestClient = webClientBuilder
        .requestFactory(requestFactory())
        .baseUrl(httpProps.url)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .apply {
            if (httpProps.sslBundle != null) {
                it.apply(webClientSsl.fromBundle(httpProps.sslBundle))
            }
        }
        .build()

    private fun requestFactory() = JdkClientHttpRequestFactory().apply { setReadTimeout(httpProps.connectionTimeout) }
}
