package com.example.demo.service

import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Commit
import org.springframework.test.context.TestConstructor
import java.util.UUID
import kotlin.test.Test

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Commit
class Test(
    private val orderService: OrderService

) {

    private val id = UUID.randomUUID()

    @BeforeEach
    fun testSaveRandomOrder() {

        val order = orderService.saveRandomOrder(userId = 1, id = id)
    }

    @Test
    fun testChangeStatus() {
        Thread() {
            orderService.changeStatus(id, com.example.demo.model.OrderStatus.COMPLETED)
        }.start()

        Thread() {
            orderService.changeStatus(id, com.example.demo.model.OrderStatus.COMPLETED)
        }.start()

        Thread.sleep(2000)
    }
}