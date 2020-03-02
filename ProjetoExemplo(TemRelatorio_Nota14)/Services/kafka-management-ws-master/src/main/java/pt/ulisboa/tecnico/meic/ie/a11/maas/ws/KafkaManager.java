package pt.ulisboa.tecnico.meic.ie.a11.maas.ws;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebService
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
public class KafkaManager {

  private static final Properties props = new Properties();
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaManager.class);

  static {
    final var loader = Thread.currentThread().getContextClassLoader();
    final var addr = Main.getHostAddress().orElse("localhost");

    try (final InputStream is = loader.getResourceAsStream("kafka.properties")) {
      LOGGER.info("Loading kafka client properties");
      props.load(Objects.requireNonNull(is));
      if (!addr.equals("localhost")) {
        props.put("bootstrap.servers", addr + ":9092");
      } else {
        props.putIfAbsent("bootstrap.servers", addr + ":9092");
      }
    } catch (IOException | NullPointerException e) {
      LOGGER.warn("Couldn't load kafka properties, assuming defaults of a local config", e);
      props.entrySet().addAll(Map.<Object, Object>of(
          "bootstrap.servers", addr + ":9092",
          "zookeeper.connect", addr + ":2181",
          "zookeeper.connection.timeout.ms", "10000",
          "zookeeper.session.timeout.ms", "15000"
      ).entrySet());
    }
  }

  public boolean createTopic(String topic) {
    LOGGER.info("Attempting to create topic '{}'", topic);

    try (final var adminClient = AdminClient.create(props)) {
      final boolean topicExists = adminClient.listTopics()
          .names()
          .thenApply(topics -> topics.contains(topic))
          .get();
      LOGGER.info("topicExists: {}", topicExists);

      if (!topicExists) {
        final var newTopic = new NewTopic(topic, 1, (short) 1);
        final KafkaFuture<Void> future = adminClient
            .createTopics(Collections.singletonList(newTopic))
            .all();
        future.get();

        final boolean topicCreated = !future.isCompletedExceptionally();
        LOGGER.info("topicCreated: {}", topicCreated);

        return topicCreated;
      }
    } catch (Throwable t) {
      // I'm sorry, I'm lazy
      LOGGER.warn(t.getMessage(), t);
    }

    return false;
  }

  public boolean deleteTopic(String topic) {
    LOGGER.info("Attempting to delete topic '{}'", topic);

    try (final var adminClient = AdminClient.create(props)) {
      final var future = adminClient.deleteTopics(Collections.singleton(topic)).all();
      future.get();

      final var topicDeleted = !future.isCompletedExceptionally();
      LOGGER.info("topicDeleted: {}", topicDeleted);

      return topicDeleted;
    } catch (Throwable t) {
      // Again, sorry
      LOGGER.warn(t.getMessage(), t);
    }

    return false;
  }
}
