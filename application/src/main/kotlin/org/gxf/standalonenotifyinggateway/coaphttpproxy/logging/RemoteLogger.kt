// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class RemoteLogger(private val remoteLoggingWebClient: RemoteLoggingWebClient) {
    private val logger = KotlinLogging.logger {}

    fun error(msg: () -> String) {
        logger.error(msg)
        remoteLoggingWebClient.remoteLogMessage(msg())
    }

    fun error(exception: Exception, msg: () -> String) {
        logger.error(exception, msg)
        remoteLoggingWebClient.remoteLogMessage(
            "$msg - exception message: ${exception.message}. See DMZ log for stacktrace.",
        )
    }
}
