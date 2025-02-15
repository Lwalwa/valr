package com.valr

import java.util.Collections
import java.util.Date
import java.util.TreeMap
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class OrderBook {
    private val buyOrders = TreeMap<Double, MutableList<Order>>(Collections.reverseOrder())
    private val sellOrders = TreeMap<Double, MutableList<Order>>()
    private val trades = Collections.synchronizedList(mutableListOf<Trade>())
    private var sequenceNumber: Long = 0
    private var tradeSequenceId: Double = 1.0
    private val lock = ReentrantLock()

    fun addOrder(order: Order) {
        lock.withLock {
            val orderMap = if (order.type == OrderType.BUY) buyOrders else sellOrders
            orderMap.computeIfAbsent(order.price) { mutableListOf() }.add(order)
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

            return OrderBookResponse(
                asks = asks,
                bids = bids,
                lastChange = Date().toString(),
                sequenceNumber = sequenceNumber++
            )
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
                    val trade = Trade(
                        price = lowestSell.price.toString(),
                        quantity = tradeQuantity.toString(),
                        currencyPair = "BTCZAR",
                        tradedAt = Date().toString(),
                        takerSide = if (highestBuy.type == OrderType.BUY) "sell" else "buy",
                        sequenceId = tradeSequenceId++,
                        id = UUID.randomUUID().toString(),
                        quoteVolume = (lowestSell.price * tradeQuantity).toString()
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