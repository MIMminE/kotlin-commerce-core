package nuts.commerce.productservice.adapter.security.apikey

interface ApiKeyAuthenticator {
    fun authenticate(apiKey: String): ApiKeyAuthResult?
}

data class ApiKeyAuthResult(
    val principal: Any,
    val roles: List<String>
)