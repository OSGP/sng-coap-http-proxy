// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

import java.time.Duration

/**
 * @property udpReceiveBufferSize
 * @property udpSendBufferSize
 * @property healthStatusInterval
 */
@ConfigurationProperties(prefix = "config.udp")
class UdpProperties(
    val udpReceiveBufferSize: Int,
    val udpSendBufferSize: Int,
    val healthStatusInterval: Duration,
)
