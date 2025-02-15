package com.valr

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SerializationTest {

    @Test
    fun testOrderSerialization() {
        val order = Order(
            id = "1",
            type = OrderType.BUY,
            price = 1813210.0,
            quantity = 0.00321055
        )

        val json = Json.encodeToString(order)
        println(json)

        val deserializedOrder = Json.decodeFromString<Order>(json)
        assertEquals(order, deserializedOrder)
    }
}