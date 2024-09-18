// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.validation

import org.gxf.standalonenotifyinggateway.coaphttpproxy.domain.Message
import org.springframework.stereotype.Service

@Service
class MessageValidator {
    fun isValid(message: Message): Boolean {
        val idValue = message.payload.findValue("ID")
        idValue?.let {
            val payloadId = idValue.asText()
            return message.deviceId == payloadId
        }
        return false
    }
}
