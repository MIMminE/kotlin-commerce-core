package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.message.InMemoryMessageConsumer
import nuts.commerce.orderservice.application.port.message.InMemoryMessageProducer
import nuts.commerce.orderservice.application.port.message.MessageProducer
import nuts.commerce.orderservice.application.port.repository.InMemoryOrderOutboxRepository
import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class PublishOrderOutboxUseCasTest {
    private val orderOutboxRepository: OrderOutboxRepository = InMemoryOrderOutboxRepository()
    private val messageConsumer: InMemoryMessageConsumer = InMemoryMessageConsumer()
    private val messageProducer: InMemoryMessageProducer = InMemoryMessageProducer(messageConsumer)
    private val transactionTemplate: TransactionTemplate = mock {
        on { execute<Any?>(org.mockito.kotlin.any()) } doAnswer { inv ->
            val cb = inv.getArgument<TransactionCallback<Any?>>(0)
            cb.doInTransaction(mock()) // status는 의미 없으니 mock
        }
    }

    private val useCase: PublishOrderOutboxUseCase = PublishOrderOutboxUseCase(
        orderOutboxRepository,
        messageProducer,
        transactionTemplate,
        batchSize = 10,
        maxRetries = 5
    )

    @Test
    fun `발행할 대상이 없으면 produce가 호출되지 않는다`() {
        useCase.publishPendingOutboxMessages()
        assertEquals(0, messageProducer.produced.size)
    }
}

