package xyz.bi7khi.inventree.mobile

import android.app.Application
import xyz.bi7khi.inventree.mobile.data.api.ApiClient
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeApi
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.local.SessionStore

class InvenTreeApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val sessionStore = SessionStore(application)

    fun api(serverUrl: String, token: String?): InvenTreeApi =
        ApiClient.create(serverUrl, token)

    fun repository(serverUrl: String, token: String): InvenTreeRepository =
        InvenTreeRepository(api(serverUrl, token), serverUrl, token)
}
