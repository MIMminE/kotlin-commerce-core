package nuts.commerce.orderservice.adapter.controller

import nuts.commerce.orderservice.exception.OrderException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(OrderException.InvalidCommand::class)
    fun handleInvalidCommand(e: OrderException.InvalidCommand): ResponseEntity<String> {
        val msg = e.message ?: "invalid command"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg)
    }

}