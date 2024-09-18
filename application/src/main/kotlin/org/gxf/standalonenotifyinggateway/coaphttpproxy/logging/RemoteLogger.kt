// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.standalonenotifyinggateway.coaphttpproxy.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * @param remoteLoggingWebClient
 */
@Component
class RemoteLogger(private val remoteLoggingWebClient: RemoteLoggingWebClient) {
    private val logger = KotlinLogging.logger {}

    fun error(msg: () -> String) {
        logger.error(msg)
        remoteLoggingWebClient.remoteLogMessage(msg())
    }

    fun error(
        exception: Exception,
        msg: () -> String,
    ) {
        logger.error(exception, msg)
        remoteLoggingWebClient.remoteLogMessage(
            "Unknown error occurred with $msg, " +
                    "exception message: ${exception.message} and stacktrace: ${exception.stackTrace}",
        )
    }
}
