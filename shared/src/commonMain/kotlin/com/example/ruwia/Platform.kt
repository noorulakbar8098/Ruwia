package com.example.ruwia

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform