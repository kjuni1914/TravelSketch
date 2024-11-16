package com.travelsketch

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(this, "10417161ccb1c6807969be9947764e7f")
    }
}