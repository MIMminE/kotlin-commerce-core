package nuts.project.commerce.application.exception

open class IdempotencyException(message: String, cause: Throwable? = null) : Exception(message, cause)

class UniqueConstraintViolationException(message: String, cause: Throwable? = null) :
    IdempotencyException(message, cause)