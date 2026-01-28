package nuts.project.commerce.application.exception

open class PaymentException (message: String, cause: Throwable? = null) : Exception(message)

class PaymentNotFoundException(paymentId: String) :
    PaymentException("Payment with ID $paymentId not found.")

class PaymentModuleFailureException(message: String, cause: Throwable) :
    PaymentException("Payment module failure: $message", cause)