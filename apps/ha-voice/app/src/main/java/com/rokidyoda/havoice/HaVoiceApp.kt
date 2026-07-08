package com.rokidyoda.havoice

import android.app.Application

class HaVoiceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: HaVoiceApp
            private set
    }
}
