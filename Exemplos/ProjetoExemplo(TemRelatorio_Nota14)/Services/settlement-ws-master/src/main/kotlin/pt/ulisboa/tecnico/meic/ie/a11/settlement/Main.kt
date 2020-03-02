package pt.ulisboa.tecnico.meic.ie.a11.settlement

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.map
import io.javalin.Javalin
import io.javalin.json.JavalinJackson
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties
import java.util.logging.Level
import java.util.logging.Logger


private const val APP_PROPERTIES = "app.properties"
private const val KAFKA_PROPERTIES = "kafka.properties"
private const val KAFKA_CONSUMER_PROPERTIES = "kafka-consumer.properties"
private const val KAFKA_PRODUCER_PROPERTIES = "kafka-producer.properties"

private val appProps = Properties()
private val kafkaProps = Properties()
private val consumerProps = Properties()
private val producerProps = Properties()

val catalogEndpoint: String by lazy {
    appProps.getProperty("maas.ws.catalog.endpoint", "http://localhost:8081")
}
val maasProviderTopic: String by lazy {
    appProps.getProperty("maas.kafka.topic", "maas")
}

val sessionFactory: SessionFactory by lazy {
    Logger.getLogger("org.hibernate").level = Level.SEVERE
    Configuration().configure().buildSessionFactory().apply {
        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }
}

val jsonMapper: ObjectMapper = jacksonObjectMapper()
    .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
    .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .registerModule(JavaTimeModule())


fun main() {
    with(Thread.currentThread().contextClassLoader) {
        appProps.load(getResourceAsStream(APP_PROPERTIES))
        consumerProps.load(getResourceAsStream(KAFKA_CONSUMER_PROPERTIES))
        producerProps.load(getResourceAsStream(KAFKA_PRODUCER_PROPERTIES))

        kafkaProps.apply {
            load(getResourceAsStream(KAFKA_PROPERTIES))
            forEach { k, v ->
                consumerProps.putIfAbsent(k, v)
                producerProps.putIfAbsent(k, v)
            }
        }

        consumerProps.putIfAbsent("bootstrap.servers", "localhost:9092")
        producerProps.putIfAbsent("bootstrap.servers", "localhost:9092")
    }

    httpServer(appProps.getProperty("maas.ws.settlement.api.port", "8000").toInt())
    Thread(::runConsumer).run()
}

private fun httpServer(port: Int) {
    // Setup JavalinJackson with Kotlin + JSR310 DateTime types
    JavalinJackson.configure(jsonMapper)

    val app = Javalin.create().apply {
        Runtime.getRuntime().addShutdownHook(Thread { stop() })
    }.start(port)

    app.get("/teapot") { ctx ->
        ctx.status(418).json(object {
            val status = ctx.status()
            val message = "These aren't the droids you're looking for"
        })
    }

    app.post("/settle") { ctx ->
        "$catalogEndpoint/operators".httpGet()
            .responseObject<List<OperatorDto>>(jsonMapper).let { (_, _, result) ->
                ctx.status(200).json(object {
                    val message = "settled"
                })
            }
    }
}

private fun runConsumer() {
    val topics = mutableSetOf(maasProviderTopic)

    "$catalogEndpoint/operators".httpGet()
        .responseObject<List<OperatorDto>>(jsonMapper).let { (_, _, result) ->
            result.map { it.map(OperatorDto::id) }.map(topics::addAll)
        }

    KafkaConsumer<String, String>(consumerProps).apply { subscribe(topics) }.use { consumer ->
        val groupId = consumerProps.getProperty("group.id")
        val logger = LoggerFactory.getLogger("KafkaConsumer($groupId)")
        val producer = KafkaProducer<String, String>(producerProps)

        while (!Thread.currentThread().isInterrupted) {
            try {
                val records = consumer.poll(Duration.ofMillis(200))
                topics.flatMap(records::records).map { it.topic() to it.value() }
                    .forEach { (topic, json) ->
                        logger.info("Got new event from $topic")
                        logger.debug(json)
                        val type = EventType.fromString(
                            jsonMapper.readTree(json)
                                ?.takeIf { it.hasNonNull("type") }?.get("type")!!
                                 .takeIf { it.isTextual }?.textValue() ?: return
                        )

                        val handler = createHandler(type ?: return, consumer, producer)

                        createEvent(type, topic, json).takeIf {
                            logger.info("Attempting to handle event of type '$type'")
                            it.handleEvent(handler)
                        }?.apply {
                            logger.info("Event handled successfully")
                            sessionFactory.openSession().use { session ->
                                session.beginTransaction().runCatching {
                                    save(session)
                                    commit()
                                    logger.info("Successfully saved event")
                                }.onFailure {
                                    logger.error("Failed to save event", it)
                                    session.transaction.rollback()
                                }
                            }

                            return
                        }

                        logger.warn("Event wasn't handled")
                    }
            } catch (ignored: InterruptedException) {
            } catch (t: Throwable) {
                logger.error("")
            }
        }
    }
}

private fun createEvent(type: EventType, topic: String, json: String): AbstractEvent =
    with(jsonMapper) {
        when (type) {
            EventType.CHECK_IN -> CheckInEvent(topic, readValue(json))
            EventType.CHECK_OUT -> CheckOutEvent(topic, readValue(json))
            EventType.FLAT_FARE -> FlatFareEvent(topic, readValue(json))
            EventType.OPERATOR_DEFINED -> OperatorDefinedEvent(topic, readValue(json))

            EventType.DISCOUNT -> DiscountEvent(readValue(json))
            EventType.NEW_OPERATOR -> NewOperatorEvent(readValue<NewOperatorEventDto>(json))
            EventType.NEW_USER -> NewUserEvent(readValue<NewUserEventDto>(json))
            EventType.PAYMENT_ORDER -> PaymentOrderEvent(topic, readValue(json))
        }
    }

private fun createHandler(
    type: EventType,
    consumer: KafkaConsumer<String, String>,
    producer: KafkaProducer<String, String>
): EventHandler = when (type) {
    EventType.CHECK_IN,
    EventType.CHECK_OUT,
    EventType.FLAT_FARE,
    EventType.OPERATOR_DEFINED -> OperatorEventHandler(consumer, producer)

    EventType.DISCOUNT,
    EventType.NEW_OPERATOR,
    EventType.NEW_USER,
    EventType.PAYMENT_ORDER -> MaasEventHandler(consumer, producer)
}
