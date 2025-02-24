package com.valr

import kotlinx.serialization.Serializable

@Serializable
data class Trade(
    val price: String,
    val quantity: String,
    val currencyPair: String,
    val tradedAt: String,
    val takerSide: String,
    val sequenceId: Double,
    val id: String,
    val quoteVolume: String
)

@Serializable
data class OrderBookResponse(
    val asks: List<OrderBookEntry>,
    val bids: List<OrderBookEntry>,
    val lastChange: String,
    val sequenceNumber: Long
)

@Serializable
data class OrderBookEntry(
    val side: String,
    val quantity: String,
    val price: String,
    val currencyPair: String,
    val orderCount: Int
)