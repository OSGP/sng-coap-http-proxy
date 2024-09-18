// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.coap

import io.github.oshai.kotlinlogging.KotlinLogging
import org.eclipse.californium.core.CoapServer as CaliforniumCoapServer
import org.eclipse.californium.core.network.CoapEndpoint
import org.eclipse.californium.elements.config.Configuration
import org.springframework.stereotype.Component

/**
 * @param config
 * @param coapResource
 * @param coapEndpoint
 */
@Component
data class CoapServer(
    private val config: Configuration,
    private val coapResource: CoapResource,
    private val coapEndpoint: CoapEndpoint,
) {
    private val logger = KotlinLogging.logger {}
    private val californiumCoapServer = CaliforniumCoapServer(config)

    init {
        with(californiumCoapServer) {
            logger.info { "Starting CoAP server." }

            addEndpoint(coapEndpoint)
            add(coapResource)
            start()

            logger.info { "Started CoAP server." }
        }
    }
}
