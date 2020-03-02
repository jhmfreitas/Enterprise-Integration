package pt.ulisboa.tecnico.meic.ie.a11.settlement

import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.Session
import org.hibernate.annotations.NaturalId
import java.io.Serializable
import java.time.LocalDateTime
import java.util.Objects
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass

// domain entities
@MappedSuperclass
abstract class AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var _id: Long = -1
    val id
        get() = _id

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            javaClass != other?.javaClass -> false
            else -> Objects.equals(_id, (other as AbstractEntity)._id)
        }
    }

    override fun hashCode(): Int = Objects.hash(_id)

    abstract fun save(session: Session): Serializable
}

@MappedSuperclass
abstract class AbstractEvent : AbstractEntity() {
    abstract fun handleEvent(handler: EventHandler): Boolean
}

enum class EventType(private val type: String) {
    // operator events
    CHECK_IN("check-in"),
    CHECK_OUT("check-out"),
    FLAT_FARE("flat-fare"),
    OPERATOR_DEFINED("operator-defined"),

    // MaaS events
    DISCOUNT("discount"),
    NEW_USER("new-user"),
    NEW_OPERATOR("new-operator"),
    PAYMENT_ORDER("payment-order");

    companion object {
        private val map = values().map { it.toString() to it }.toMap()
        fun fromString(name: String) = map[name]
    }

    override fun toString() = type
}

@MappedSuperclass
abstract class OperatorEvent(
    val token: String,
    val operator: String,
    val serviceName: String,
    val timestamp: LocalDateTime
) : AbstractEvent()

@Entity
class CheckInEvent(
    operator: String,
    token: String,
    serviceName: String,
    timestamp: LocalDateTime
) : OperatorEvent(token, operator, serviceName, timestamp) {
    var checkedOut: Boolean = false
        set(value) {
            if (!field) field = value
        }

    constructor(operator: String, dto: CheckInEventDto) : this(
        operator,
        dto.token,
        dto.serviceName,
        dto.timestamp
    )

    override fun handleEvent(handler: EventHandler) = handler.handle(this)
    override fun save(session: Session): Serializable = session.save(this)
}

@Entity
class CheckOutEvent(
    operator: String,
    token: String,
    serviceName: String,
    timestamp: LocalDateTime
) : OperatorEvent(token, operator, serviceName, timestamp) {
    constructor(operator: String, dto: CheckOutEventDto) : this(
        operator,
        dto.token,
        dto.serviceName,
        dto.timestamp
    )

    override fun handleEvent(handler: EventHandler) = handler.handle(this)
    override fun save(session: Session): Serializable = session.save(this)
}

@Entity
class FlatFareEvent(
    operator: String,
    token: String,
    serviceName: String,
    timestamp: LocalDateTime
) : OperatorEvent(token, operator, serviceName, timestamp) {
    constructor(operator: String, dto: FlatFareEventDto) : this(
        operator,
        dto.token,
        dto.serviceName,
        dto.timestamp
    )

    override fun handleEvent(handler: EventHandler) = handler.handle(this)
    override fun save(session: Session): Serializable = session.save(this)
}

@Entity
class OperatorDefinedEvent(
    operator: String,
    token: String,
    serviceName: String,
    timestamp: LocalDateTime,
    val tariff: Int
) : OperatorEvent(token, operator, serviceName, timestamp) {
    constructor(operator: String, dto: OperatorDefinedEventDto) : this(
        operator,
        dto.token,
        dto.serviceName,
        dto.timestamp,
        dto.tariff
    )

    override fun handleEvent(handler: EventHandler) = handler.handle(this)
    override fun save(session: Session): Serializable = session.save(this)
}


@MappedSuperclass
abstract class MaasEvent : AbstractEvent()

@Entity
class DiscountEvent(
    val token: String,
    val timestamp: LocalDateTime,
    @Embedded
    val value: Value
) : MaasEvent() {
    constructor(dto: DiscountEventDto) : this(dto.token, dto.timestamp, Value(dto.value))

    override fun handleEvent(handler: EventHandler) = handler.handle(this)
    override fun save(session: Session): Serializable = session.save(this)
}

@Embeddable
class Value(val a: Double, val b: Int) {
    constructor(dto: ValueDto) : this(dto.a, dto.b)
}

@Entity
class NewOperatorEvent(@NaturalId val operatorId: String) : MaasEvent() {
    constructor(dto: NewOperatorEventDto) : this(dto.id)

    override fun handleEvent(handler: EventHandler) = handler.handle(this)
    override fun save(session: Session): Serializable = session.save(this)
}

@Entity
class NewUserEvent(val token: String) : MaasEvent() {
    constructor(dto: NewUserEventDto) : this(dto.user.token)

    override fun handleEvent(handler: EventHandler) = handler.handle(this)
    override fun save(session: Session): Serializable = session.save(this)
}

@Entity
class PaymentOrderEvent(
    val token: String,
    val operator: String,
    @Embedded
    val paymentDetails: PaymentDetails,
    val timestamp: LocalDateTime = LocalDateTime.now()
) : MaasEvent() {
    constructor(operator: String, dto: PaymentOrderEventDto) : this(
        dto.token,
        operator,
        PaymentDetails(dto.paymentDetails),
        dto.timestamp
    )

    override fun handleEvent(handler: EventHandler) = handler.handle(this)
    override fun save(session: Session): Serializable = session.save(this)
}

@Embeddable
class PaymentDetails(val amount: Int) {
    constructor(dto: PaymentDetailsDto) : this(dto.amount)
}


// MaaS data transfer objects
abstract class AbstractEventDto(val type: EventType)

abstract class OperatorEventDto(
    type: EventType,
    open val serviceName: String,
    open val token: String,
    open val timestamp: LocalDateTime = LocalDateTime.now()
) : AbstractEventDto(type)

data class CheckInEventDto(
    override val token: String,
    override val serviceName: String,
    override val timestamp: LocalDateTime
) : OperatorEventDto(EventType.CHECK_IN, serviceName, token, timestamp)

data class CheckOutEventDto(
    override val token: String,
    override val serviceName: String,
    override val timestamp: LocalDateTime
) : OperatorEventDto(EventType.CHECK_OUT, serviceName, token, timestamp)

data class FlatFareEventDto(
    override val token: String,
    override val serviceName: String,
    override val timestamp: LocalDateTime
) : OperatorEventDto(EventType.FLAT_FARE, serviceName, token, timestamp)

data class OperatorDefinedEventDto(
    override val token: String,
    override val serviceName: String,
    override val timestamp: LocalDateTime,
    val tariff: Int
) : OperatorEventDto(EventType.OPERATOR_DEFINED, serviceName, token, timestamp)


abstract class MaasEventDto(type: EventType) : AbstractEventDto(type)

data class DiscountEventDto(
    val token: String,
    val timestamp: LocalDateTime,
    val value: ValueDto
) : MaasEventDto(EventType.DISCOUNT)

data class ValueDto(val a: Double, val b: Int)

data class NewUserEventDto(val user: UserDto) : MaasEventDto(EventType.NEW_USER)

data class UserDto(
    @JsonProperty("id")
    val token: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val balance: Int,
    val isBlacklisted: Boolean,
    val trips: Int
)

data class NewOperatorEventDto(
    @JsonProperty("operator")
    val id: String
) : MaasEventDto(EventType.NEW_OPERATOR)

data class PaymentOrderEventDto(
    val token: String,
    val timestamp: LocalDateTime,
    val paymentDetails: PaymentDetailsDto
) : MaasEventDto(EventType.PAYMENT_ORDER)

data class PaymentDetailsDto(val amount: Int)


// Catalog service
data class OperatorDto(
    val id: String,
    val name: String,
    val address: AddressDto,
    val email: String,
    val vatNumber: String,
    val services: List<String>?
)

data class OperatorServiceDto(
    val name: String,
    val operatorId: String,
    val serviceType: String,
    val tariff: Int
)

data class AddressDto(
    val district: String,
    val county: String,
    val zipCode: String,
    val client: String
)
