package nuts.commerce.inventoryservice.adapter.system

import jakarta.annotation.PostConstruct
import nuts.commerce.inventoryservice.usecase.PublishCurrentStockOnStartUp
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "system.startup-runner",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class StartUpRunner(private val publishCurrentStockOnStartUp: PublishCurrentStockOnStartUp) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(StartUpRunner::class.java)

    @PostConstruct
    fun init() {
        logger.info("StartUpRunner initialized. Will publish current stock on startup.")
    }

    override fun run(vararg args: String) {
        publishCurrentStockOnStartUp.publish()
    }
}