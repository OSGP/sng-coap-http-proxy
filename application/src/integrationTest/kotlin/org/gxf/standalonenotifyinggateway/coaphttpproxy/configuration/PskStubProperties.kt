// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @property defaultId
 * @property defaultKey
 */
@ConfigurationProperties(prefix = "config.psk")
class PskStubProperties(val defaultId: String, val defaultKey: String)
