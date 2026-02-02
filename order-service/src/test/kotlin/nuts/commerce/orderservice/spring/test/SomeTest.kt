package nuts.commerce.orderservice.spring.test

import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["order.outbox.publish.enabled=false"])
class SomeTest