package com.valr

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class OrderBookTest {

    private val orderBook = OrderBook()

    @Test
    fun testAddAndMatchOrders() {
        orderBook.addOrder(Order("1", OrderType.BUY, 1813210.0, 0.00321055))
        orderBook.addOrder(Order("2", OrderType.SELL, 1813210.0, 0.00321055))

        val trades = orderBook.getRecentTrades()

        assertEquals(1, trades.size)
        assertEquals("1813210.0", trades[0].price)
        assertEquals("0.00321055", trades[0].quantity)
        assertEquals("BTCZAR", trades[0].currencyPair)
        assertEquals("sell", trades[0].takerSide)
    }
}