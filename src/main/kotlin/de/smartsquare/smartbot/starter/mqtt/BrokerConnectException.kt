package de.smartsquare.smartbot.starter.mqtt

import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck

class BrokerConnectException(message: String) : RuntimeException(message) {
    constructor(acknowledgement: Mqtt3ConnAck) : this("Unable to connect to broker. Return Code: ${acknowledgement.returnCode.code}")
}
