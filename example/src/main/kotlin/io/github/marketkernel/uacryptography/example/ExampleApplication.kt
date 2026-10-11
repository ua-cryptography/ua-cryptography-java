package io.github.marketkernel.uacryptography.example

import android.app.Application
import io.github.marketkernel.uacryptography.example.trust.TrustStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/** Holds the one trust store of the app and starts reading it at launch. */
class ExampleApplication : Application() {
    val trustStore: TrustStore by lazy { TrustStore(this) }

    override fun onCreate() {
        super.onCreate()
        MainScope().launch { trustStore.load() }
    }
}
