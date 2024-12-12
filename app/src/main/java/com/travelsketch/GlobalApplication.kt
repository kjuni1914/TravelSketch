package com.travelsketch

import android.app.Application
import android.content.Context
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    companion object {
        private lateinit var instance: GlobalApplication

        fun getInstance(): GlobalApplication {
            return instance
        }

        fun getContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        KakaoSdk.init(this, "10417161ccb1c6807969be9947764e7f")
    }
}