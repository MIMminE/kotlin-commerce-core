package nuts.commerce.orderservice.support

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.UnexpectedRollbackException
import org.springframework.transaction.support.SimpleTransactionStatus
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


class TestTransactionManager : PlatformTransactionManager {

    private val lastStatusRef: AtomicReference<TransactionStatus?> = AtomicReference(null)
    private val _commitCalled = AtomicBoolean(false)
    private val _rollbackCalled = AtomicBoolean(false)

    override fun getTransaction(definition: TransactionDefinition?): TransactionStatus {
        val status = SimpleTransactionStatus()
        _commitCalled.set(false)
        _rollbackCalled.set(false)
        lastStatusRef.set(status)
        return status
    }

    override fun commit(status: TransactionStatus) {
        _commitCalled.set(true)
        if (status.isRollbackOnly) {
            throw UnexpectedRollbackException("Transaction marked as rollback-only")
        }
    }

    override fun rollback(status: TransactionStatus) {
        _rollbackCalled.set(true)
    }

    fun wasCommitted(): Boolean = _commitCalled.get()
    fun wasRolledBack(): Boolean = _rollbackCalled.get()
    fun lastStatus(): TransactionStatus? = lastStatusRef.get()
    fun reset() {
        _commitCalled.set(false)
        _rollbackCalled.set(false)
        lastStatusRef.set(null)
    }
}

