package fr.tp.slr201.projects.robotsim.service.simulation.kafka;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.notifier.FactoryModelChangedNotifier;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class KafkaFactoryModelChangeNotifier implements FactoryModelChangedNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaFactoryModelChangeNotifier.class);

    private final Factory factoryModel;
    private final KafkaTemplate<String, Factory> kafkaTemplate;
    private final AdminClient adminClient;

    public KafkaFactoryModelChangeNotifier(final Factory factoryModel,
                                           final KafkaTemplate<String, Factory> kafkaTemplate,
                                           final AdminClient adminClient) {
        this.factoryModel = factoryModel;
        this.kafkaTemplate = kafkaTemplate;
        this.adminClient = adminClient;

        ensureTopicExists();
    }

    private String topicName() {
        return "simulation-topic-" + factoryModel.getId();
    }

    private void ensureTopicExists() {
        try {
            final NewTopic topic = new NewTopic(topicName(), 1, (short) 1);
            adminClient.createTopics(Collections.singleton(topic));
        } catch (final Exception e) {
            // Often harmless if topic already exists depending on broker settings
            LOG.debug("Topic creation attempt for '{}' resulted in: {}", topicName(), e.getMessage());
        }
    }

    @Override
    public void notifyObservers() {
        try {
            final Message<Factory> message = MessageBuilder.withPayload(factoryModel)
                    .setHeader(KafkaHeaders.TOPIC, topicName())
                    .build();
            final CompletableFuture<?> result = kafkaTemplate.send(message);
            result.whenComplete((r, ex) -> {
                if (ex != null) {
                    LOG.warn("Failed to publish factory update to topic {}: {}", topicName(), ex.getMessage());
                }
            });
        } catch (final Exception e) {
            LOG.warn("Error while sending factory update to Kafka: {}", e.getMessage());
        }
    }

    @Override
    public boolean addObserver(final fr.tp.inf112.projects.canvas.controller.Observer observer) {
        // Remote notifier does not keep local observers
        return true;
    }

    @Override
    public boolean removeObserver(final fr.tp.inf112.projects.canvas.controller.Observer observer) {
        // Remote notifier does not keep local observers
        return true;
    }
}
