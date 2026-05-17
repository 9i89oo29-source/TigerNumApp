package com.tigernum.app.data.remote.dto

data class BuyResponseDto(
    val orderId: String,
    val number: String,
    val price: Double,
    val currency: String
)
