package com.valr

import io.restassured.RestAssured
import io.restassured.http.Header
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import io.vertx.core.Vertx
import io.vertx.ext.web.Router

class ApiTests {
    private val validApiKey = "my-secret-api-key"
    private lateinit var vertx: Vertx

    @BeforeEach
    fun setup() {
        vertx = Vertx.vertx()
        val router = Router.router(vertx)
        val orderBook = OrderBook()
        val validApiKey = "my-secret-api-key"

        router.route().handler(io.vertx.ext.web.handler.BodyHandler.create())
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
                if (!http.succeeded()) {
                    println("HTTP server failed to start")
                }
            }

        RestAssured.baseURI = "http://localhost"
        RestAssured.port = 8080
    }

    @AfterEach
    fun teardown() {
        vertx.close()
    }

    @Test
    fun testGetOrderBook() {
        RestAssured.given()
            .header(Header("X-API-Key", validApiKey))
            .get("/api/orderbook")
            .then()
            .statusCode(200)
    }

    @Test
    fun testAddLimitOrder() {
        val order = Order("1", OrderType.BUY, 1813210.0, 0.00321055)
        RestAssured.given()
            .header(Header("X-API-Key", validApiKey))
            .contentType("application/json")
            .body(Json.encodeToString(order))
            .post("/api/orders/limit")
            .then()
            .statusCode(201)
    }

    @Test
    fun testGetRecentTrades() {
        val buyOrder = Order("1", OrderType.BUY, 1813210.0, 0.00321055)
        val sellOrder = Order("2", OrderType.SELL, 1813210.0, 0.00321055)

        RestAssured.given()
            .header(Header("X-API-Key", validApiKey))
            .contentType("application/json")
            .body(Json.encodeToString(buyOrder))
            .post("/api/orders/limit")

        RestAssured.given()
            .header(Header("X-API-Key", validApiKey))
            .contentType("application/json")
            .body(Json.encodeToString(sellOrder))
            .post("/api/orders/limit")

        RestAssured.given()
            .header(Header("X-API-Key", validApiKey))
            .get("/api/recenttrades")
            .then()
            .statusCode(200)
    }

    @Test
    fun testUnauthorizedAccess() {
        RestAssured.given()
            .header(Header("X-API-Key", "invalid-api-key"))
            .get("/api/orderbook")
            .then()
            .statusCode(401)
    }
}