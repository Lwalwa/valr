package com.valr

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OrderService : AbstractVerticle() {
    private val orderBook = OrderBook()
    private val validApiKey = "my-secret-api-key"

    override fun start(startPromise: Promise<Void>) {
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        router.route("/api/*").handler { ctx ->
            val apiKey = ctx.request().getHeader("X-API-Key")
            if (apiKey == validApiKey) {
                ctx.next()
            } else {
                ctx.response().setStatusCode(401).end("Unauthorized: Invalid API Key")
            }
        }

        router.get("/api/orderbook").handler { ctx ->
            val orderBookResponse = orderBook.getOrderBook()
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(Json.encodeToString(orderBookResponse))
        }

        router.post("/api/orders/limit").handler { ctx ->
            try {
                val order = Json.decodeFromString<Order>(ctx.bodyAsString)
                orderBook.addOrder(order)
                ctx.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToString(order))
            } catch (e: Exception) {
                ctx.response()
                    .setStatusCode(400)
                    .end("Invalid order format: ${e.message}")
            }
        }

        router.get("/api/recenttrades").handler { ctx ->
            val trades = orderBook.getRecentTrades()
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(Json.encodeToString(trades))
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080) { http ->
                if (http.succeeded()) {
                    println("HTTP server started on port 8080")
                    startPromise.complete()
                } else {
                    println("HTTP server failed to start")
                    startPromise.fail(http.cause())
                }
            }
    }
}