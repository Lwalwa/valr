package com.valr


import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val type: OrderType,
    val price: Double,
    val quantity: Double
)

@Serializable
enum class OrderType {
    BUY, SELL
}