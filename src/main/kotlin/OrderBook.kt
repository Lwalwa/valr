package com.valr

import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class OrderBook {
    private val buyOrders = TreeMap<Double, MutableList<Order>>(Collections.reverseOrder())
    private val sellOrders = TreeMap<Double, MutableList<Order>>()
    private val trades = Collections.synchronizedList(mutableListOf<Trade>())
    private var sequenceNumber: Long = 0
    private var tradeSequenceId: Double = 1.0
    private val lock = ReentrantLock()

    private var isOrderBookModified = false

    fun addOrder(order: Order) {
        lock.withLock {
            val orderMap = if (order.type == OrderType.BUY) buyOrders else sellOrders
            orderMap.computeIfAbsent(order.price) { mutableListOf() }.add(order)
            isOrderBookModified = true
            matchOrders()
        }
    }

    fun getOrderBook(): OrderBookResponse {
        lock.withLock {
            val asks = sellOrders.flatMap { (price, orders) ->
                listOf(
                    OrderBookEntry(
                        side = "sell",
                        quantity = orders.sumOf { it.quantity }.toString(),
                        price = price.toString(),
                        currencyPair = "BTCZAR",
                        orderCount = orders.size
                    )
                )
            }

            val bids = buyOrders.flatMap { (price, orders) ->
                listOf(
                    OrderBookEntry(
                        side = "buy",
                        quantity = orders.sumOf { it.quantity }.toString(),
                        price = price.toString(),
                        currencyPair = "BTCZAR",
                        orderCount = orders.size
                    )
                )
            }

            val response = OrderBookResponse(
                asks = asks,
                bids = bids,
                lastChange = Date().toString(),
                sequenceNumber = if (isOrderBookModified) sequenceNumber++ else sequenceNumber
            )

            isOrderBookModified = false
            return response
        }
    }

    fun getRecentTrades(): List<Trade> {
        lock.withLock {
            return trades.toList()
        }
    }

    private fun matchOrders() {
        lock.withLock {
            while (buyOrders.isNotEmpty() && sellOrders.isNotEmpty()) {
                val highestBuyEntry = buyOrders.firstEntry()
                val lowestSellEntry = sellOrders.firstEntry()

                if (highestBuyEntry.key >= lowestSellEntry.key) {
                    val highestBuy = highestBuyEntry.value.first()
                    val lowestSell = lowestSellEntry.value.first()
                    val tradeQuantity = minOf(highestBuy.quantity, lowestSell.quantity)
                    val tradePrice = lowestSell.price

                    val trade = Trade(
                        price = tradePrice.toString(),
                        quantity = tradeQuantity.toString(),
                        currencyPair = "BTCZAR",
                        tradedAt = Date().toString(),
                        takerSide = if (highestBuy.type == OrderType.BUY) "buy" else "sell",
                        sequenceId = tradeSequenceId++,
                        id = UUID.randomUUID().toString(),
                        quoteVolume = (tradePrice * tradeQuantity).toString()
                    )
                    trades.add(trade)

                    updateOrRemoveOrder(highestBuyEntry, tradeQuantity)
                    updateOrRemoveOrder(lowestSellEntry, tradeQuantity)
                } else {
                    break
                }
            }
        }
    }

    private fun updateOrRemoveOrder(entry: MutableMap.MutableEntry<Double, MutableList<Order>>, tradeQuantity: Double) {
        val order = entry.value.first()
        if (order.quantity > tradeQuantity) {
            entry.value[0] = order.copy(quantity = order.quantity - tradeQuantity)
        } else {
            entry.value.removeAt(0)
            if (entry.value.isEmpty()) {
                if (order.type == OrderType.BUY) buyOrders.remove(entry.key) else sellOrders.remove(entry.key)
            }
        }
    }
}