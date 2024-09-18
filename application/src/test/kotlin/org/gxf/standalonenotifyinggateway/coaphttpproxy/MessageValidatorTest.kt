// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy

import org.gxf.standalonenotifyinggateway.coaphttpproxy.coap.validation.MessageValidator
import org.gxf.standalonenotifyinggateway.coaphttpproxy.domain.Message

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MessageValidatorTest {
    @Test
    fun testMessageValidatorCorrectId() {
        val jsonNode = ObjectMapper().readTree("{\"ID\": 12345}")
        val testMessage = Message("12345", jsonNode)

        val result = MessageValidator().isValid(testMessage)

        assertThat(result).isTrue()
    }

    @Test
    fun testMessageValidatorInvalidId() {
        val jsonNode = ObjectMapper().readTree("{\"ID\": 12345}")
        val testMessage = Message("6789", jsonNode)

        val result = MessageValidator().isValid(testMessage)

        assertThat(result).isFalse()
    }
}
