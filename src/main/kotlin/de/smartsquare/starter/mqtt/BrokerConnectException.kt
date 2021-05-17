package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck

/**
 * Exception thrown when the connection to mqtt broker fails.
 */
class BrokerConnectException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    constructor(acknowledgement: Mqtt3ConnAck, cause: Throwable? = null) : this(
        "Unable to connect to broker. Return Code: ${acknowledgement.returnCode.code}",
        cause
    )
}
