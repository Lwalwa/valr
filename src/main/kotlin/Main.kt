package com.valr

import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() {
    val vertx = Vertx.vertx()
    val router = Router.router(vertx)
    val orderBook = OrderBook()
    val validApiKey = "my-secret-api-key"

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
        val order = Json.decodeFromString<Order>(ctx.bodyAsString)
        orderBook.addOrder(order)
        ctx.response()
            .setStatusCode(201)
            .putHeader("content-type", "application/json")
            .end(Json.encodeToString(order))
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
            } else {
                println("HTTP server failed to start")
            }
        }
}