package pt.ulisboa.tecnico.meic.ie.a11.settlement

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.hibernate.Session
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

interface EventHandler {
    // operator events

    fun handle(event: CheckInEvent): Boolean
    fun handle(event: CheckOutEvent): Boolean
    fun handle(event: FlatFareEvent): Boolean
    fun handle(event: OperatorDefinedEvent): Boolean

    // MaaS events

    fun handle(event: DiscountEvent): Boolean
    fun handle(event: NewOperatorEvent): Boolean
    fun handle(event: PaymentOrderEvent): Boolean
    fun handle(event: NewUserEvent): Boolean
}

sealed class AbstractEventHandler<K, V>(
    protected val consumer: KafkaConsumer<K, V>,
    protected val producer: KafkaProducer<K, V>
) : EventHandler

class OperatorEventHandler(
    consumer: KafkaConsumer<String, String>,
    producer: KafkaProducer<String, String>
) : AbstractEventHandler<String, String>(consumer, producer) {

    private val logger = LoggerFactory.getLogger(OperatorEventHandler::class.java)

    override fun handle(event: CheckInEvent): Boolean {
        logger.debug("- token: ${event.token}")
        val services = getOperatorServices(
            event.operator,
            "${EventType.CHECK_IN} ${EventType.CHECK_OUT}",
            event.serviceName
        )

        return services.isNotEmpty() && sessionFactory.openSession().use { session ->
            session.beginTransaction().runCatching {
                val hql = """from CheckInEvent
                               where token = :token
                               and operator = :operator
                               and serviceName = :name
                               and checkedOut = false"""

                val ok = session.createQuery(hql, CheckInEvent::class.java)
                    .setParameter("token", event.token)
                    .setParameter("operator", event.operator)
                    .setParameter("name", event.serviceName)
                    .list().isEmpty()
                commit()
                ok
            }.onFailure {
                logger.error("Error handling check-out event", it)
                session.transaction.rollback()
            }.getOrDefault(false)
        }
    }

    override fun handle(event: CheckOutEvent): Boolean {
        logger.debug("- token: ${event.token}")
        val services = getOperatorServices(
            event.operator,
            "${EventType.CHECK_IN} ${EventType.CHECK_OUT}",
            event.serviceName
        )

        return services.isNotEmpty() && sessionFactory.openSession().use { session ->
            session.beginTransaction().runCatching {
                val hql = """from CheckInEvent
                               where token = :token
                               and operator = :operator
                               and serviceName = :name
                               and checkedOut = false"""

                session.createQuery(hql, CheckInEvent::class.java)
                    .setParameter("token", event.token)
                    .setParameter("operator", event.operator)
                    .setParameter("name", event.serviceName)
                    .list().maxBy { it.timestamp }?.apply {
                        logger.info("Found matching check-in for ${this.token}")
                        checkedOut = true
                        sendPaymentOrder(session, event, services.first().tariff)
                        session.save(this)
                    }
                commit()
            }.onFailure {
                logger.error("Error handling check-out event", it)
                session.transaction.rollback()
            }.isSuccess
        }
    }

    override fun handle(event: FlatFareEvent): Boolean {
        logger.debug("- token: ${event.token}")
        val services = getOperatorServices(
            event.operator,
            EventType.FLAT_FARE.toString(),
            event.serviceName
        )

        return services.isNotEmpty() && sessionFactory.openSession().use { session ->
            session.beginTransaction().runCatching {
                sendPaymentOrder(session, event, services.first().tariff)
                commit()
            }.onFailure {
                logger.error("Error handling flat-fare event", it)
                session.transaction.rollback()
            }.isSuccess
        }
    }

    override fun handle(event: OperatorDefinedEvent): Boolean {
        logger.debug("- token: ${event.token}")
        val services = getOperatorServices(
            event.operator,
            EventType.OPERATOR_DEFINED.toString(),
            event.serviceName
        )

        return services.isNotEmpty() && sessionFactory.openSession().use { session ->
            session.beginTransaction().runCatching {
                sendPaymentOrder(session, event, event.tariff)
                commit()
            }.onFailure {
                logger.error("Error handling operator-defined event", it)
                session.transaction.rollback()
            }.isSuccess
        }
    }

    private fun sendPaymentOrder(
        session: Session,
        event: OperatorEvent,
        tariff: Int
    ) = sendPaymentOrder(session, event, tariff, true)

    private fun sendPaymentOrder(
        session: Session,
        event: OperatorEvent,
        tariff: Int,
        applyDiscount: Boolean
    ) {
        logger.info("Sending payment order to ${event.token}")
        logger.debug("""- operator: ${event.operator}
                       |- serviceName: ${event.serviceName}
                       |- tariff: ${tariff / 100f}€
                       |- applyDiscount: $applyDiscount""".trimMargin())

        val discount = if (applyDiscount) computeDiscount(session, event.token, tariff) else tariff
        val details = PaymentDetailsDto(discount)
        val dto = PaymentOrderEventDto(event.token, LocalDateTime.now(), details)
        logger.debug("- paymentOrderDto: $dto")

        val json = jsonMapper.writeValueAsString(dto)
        producer.send(ProducerRecord<String, String>(maasProviderTopic, json)).get()
        session.save(PaymentOrderEvent(event.operator, dto))
    }


    private fun computeDiscount(session: Session, token: String, tariff: Int): Int {
        logger.info("Computing discount for user $token")
        logger.debug("- tariff: ${tariff / 100f}€")

        val hql = "from DiscountEvent where token = :token"
        val value = session.createQuery(hql, DiscountEvent::class.java)
            .setParameter("token", token)
            .list().maxBy { it.timestamp }?.value ?: Value(1.0, 0)

        logger.debug("- value: a = ${value.a}; b = ${value.b}")
        val mx = ceil(tariff * value.a).toInt()
        val realB = max(0, min(mx - value.b, value.b))
        return (mx - realB).also { logger.debug("- discount: ${it / 100f}€") }
    }

    private fun getOperatorServices(operator: String, serviceType: String, serviceName: String) =
        "$catalogEndpoint/operators/$operator/services".httpGet()
            .responseObject<List<OperatorServiceDto>>(jsonMapper)
            .let { (_, _, result) -> result }
            .map { it.filter { op -> op.serviceType == serviceType && op.name == serviceName } }
            .map { logger.info("Operator services: $it"); it }
            .mapError { logger.error("Error getting operator services", it); it }
            .getOrElse(emptyList())

    override fun handle(event: DiscountEvent) = false
    override fun handle(event: NewOperatorEvent) = false
    override fun handle(event: NewUserEvent) = false
    override fun handle(event: PaymentOrderEvent) = false

}

class MaasEventHandler<K, V>(
    consumer: KafkaConsumer<K, V>,
    producer: KafkaProducer<K, V>
) : AbstractEventHandler<K, V>(consumer, producer) {

    private val logger = LoggerFactory.getLogger(OperatorEventHandler::class.java)

    override fun handle(event: CheckInEvent) = false
    override fun handle(event: CheckOutEvent) = false
    override fun handle(event: FlatFareEvent) = false
    override fun handle(event: OperatorDefinedEvent) = false

    override fun handle(event: DiscountEvent) = true

    override fun handle(event: NewOperatorEvent) = consumer.runCatching {
        subscribe(subscription().apply { add(event.operatorId) })
        logger.info("Subscriptions: ${consumer.subscription()}")
    }.isSuccess

    override fun handle(event: NewUserEvent) = true

    override fun handle(event: PaymentOrderEvent) = false
}