package fr.tp.inf112.projects.robotsim.remote;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class FactorySimulationEventConsumer implements Runnable {

    private final KafkaConsumer<String, String> consumer;
    private final RemoteSimulatorController controller;
    private final String factoryId;

    public FactorySimulationEventConsumer(final RemoteSimulatorController controller, final String factoryId) {
        this.controller = controller;
        this.factoryId = factoryId;
        final Properties props = SimulationServiceUtils.getDefaultConsumerProperties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        this.consumer = new KafkaConsumer<>(props);
        final String topicName = SimulationTopicsUtils.getTopicName(factoryId);
        this.consumer.subscribe(Collections.singletonList(topicName));
    }

    @Override
    public void run() {
        try {
            while (controller.isPolling()) {
                final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                for (final ConsumerRecord<String, String> record : records) {
                    controller.deliverFromJson(record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }
}
