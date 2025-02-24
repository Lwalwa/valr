package com.valr

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.ext.web.client.WebClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.vertx.core.json.JsonObject

@ExtendWith(VertxExtension::class)
class OrderServiceTest {
    private lateinit var vertx: Vertx
    private lateinit var client: WebClient

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        this.vertx = vertx
        client = WebClient.create(vertx)
        vertx.deployVerticle(OrderService(), testContext.succeeding { _ -> testContext.completeNow() })
    }

    @Test
    fun testGetOrderBook(vertx: Vertx, testContext: VertxTestContext) {
        client.get(8080, "localhost", "/api/orderbook")
            .putHeader("X-API-Key", "my-secret-api-key")
            .send { ar ->
                if (ar.succeeded()) {
                    val response = ar.result()
                    testContext.verify {
                        assert(response.statusCode() == 200)
                        testContext.completeNow()
                    }
                } else {
                    testContext.failNow(ar.cause())
                }
            }
    }

    @Test
    fun testSubmitOrder(vertx: Vertx, testContext: VertxTestContext) {
        val order = JsonObject()
            .put("id", "1")
            .put("type", "BUY")
            .put("price", 5000.0)
            .put("quantity", 1.0)

        client.post(8080, "localhost", "/api/orders/limit")
            .putHeader("X-API-Key", "my-secret-api-key")
            .putHeader("Content-Type", "application/json")
            .sendJson(order) { ar ->
                if (ar.succeeded()) {
                    val response = ar.result()
                    testContext.verify {
                        assert(response.statusCode() == 201)
                        testContext.completeNow()
                    }
                } else {
                    testContext.failNow(ar.cause())
                }
            }
    }
}