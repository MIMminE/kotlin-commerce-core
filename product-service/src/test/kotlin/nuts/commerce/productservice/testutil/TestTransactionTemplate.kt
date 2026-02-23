package nuts.commerce.productservice.testutil

import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

class TestTransactionStatus : TransactionStatus {
    private var rollbackOnly = false
    private var completed = false

    override fun isNewTransaction(): Boolean = true
    override fun hasSavepoint(): Boolean = false
    override fun setRollbackOnly() { rollbackOnly = true }
    override fun isRollbackOnly(): Boolean = rollbackOnly
    override fun flush() {}
    override fun isCompleted(): Boolean = completed
    override fun createSavepoint(): Any = Any()
    override fun rollbackToSavepoint(savepoint: Any) {}
    override fun releaseSavepoint(savepoint: Any) {}
}

class TestTransactionTemplate : TransactionTemplate() {
    override fun <T> execute(action: TransactionCallback<T>): T {
        // 간단한 테스트용 TransactionStatus 전달
        @Suppress("UNCHECKED_CAST")
        return action.doInTransaction(TestTransactionStatus()) as T
    }
}
