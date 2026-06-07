package com.example.diplom

import android.app.Application
import com.example.diplom.data.OnboardingStore
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.Session
import com.example.diplom.data.TokenStore
import kotlinx.coroutines.runBlocking

class DiplomApplication : Application() {
    lateinit var tokenStore: TokenStore
        private set
    lateinit var onboardingStore: OnboardingStore
        private set

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(this)
        onboardingStore = OnboardingStore(this)
        RemoteApi.init()
        runBlocking {
            Session.token = tokenStore.getToken()
        }
    }
}
