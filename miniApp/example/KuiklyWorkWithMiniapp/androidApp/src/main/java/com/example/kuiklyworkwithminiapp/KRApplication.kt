package com.example.kuiklyworkwithminiapp

import android.app.Application

class KRApplication : Application() {

    init {
        application = this
    }

    companion object {
        lateinit var application: Application
    }
}