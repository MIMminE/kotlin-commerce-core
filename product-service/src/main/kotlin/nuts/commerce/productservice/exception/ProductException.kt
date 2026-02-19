package nuts.commerce.productservice.exception


sealed class ProductException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    class InvalidCommand(message: String) : ProductException(message)
}