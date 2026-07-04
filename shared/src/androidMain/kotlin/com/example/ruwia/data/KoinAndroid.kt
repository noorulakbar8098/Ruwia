package com.example.ruwia.data

import android.content.Context
import org.koin.android.ext.koin.androidContext

fun initKoinAndroid(context: Context) {
    initKoin {
        androidContext(context)
    }
}
