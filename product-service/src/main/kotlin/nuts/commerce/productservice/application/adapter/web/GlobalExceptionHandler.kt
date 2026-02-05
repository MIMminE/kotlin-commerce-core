package nuts.commerce.productservice.application.adapter.web

import nuts.commerce.productservice.model.exception.ProductException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import jakarta.persistence.OptimisticLockException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ProductException.InvalidCommand::class)
    fun handleInvalidCommand(e: ProductException.InvalidCommand): ResponseEntity<String> {
        val msg = e.message ?: "Invalid command"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg)
    }

    @ExceptionHandler(ProductException.InvalidTransition::class)
    fun handleInvalidTransition(e: ProductException.InvalidTransition): ResponseEntity<String> {
        val msg = e.message ?: "Invalid transition"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg)
    }

    @ExceptionHandler(OptimisticLockingFailureException::class, OptimisticLockException::class)
    fun handleOptimisticLock(e: Exception): ResponseEntity<String> {
        val msg = e.message ?: "Conflict due to optimistic locking"
        return ResponseEntity.status(HttpStatus.CONFLICT).body(msg)
    }
}