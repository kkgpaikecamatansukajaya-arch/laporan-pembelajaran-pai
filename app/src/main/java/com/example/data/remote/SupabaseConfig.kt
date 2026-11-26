package com.example.data.remote

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets

object SupabaseConfig {
    const val DEFAULT_URL = "https://fnztmondnvgyrkuctlty.supabase.co"
    const val STORAGE_BUCKET = "pai-documents"

    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().ifBlank { DEFAULT_URL }

    val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    fun isConfigured(): Boolean {
        val key = publishableKey
        val url = supabaseUrl
        if (!url.startsWith("https://")) return false
        if (key.isBlank()) return false
        if (key.startsWith("YOUR_", ignoreCase = true) || key.startsWith("MY_", ignoreCase = true)) return false
        return key.length >= 20
    }
}

object SupabaseClientProvider {
    @Volatile
    private var cachedClient: SupabaseClient? = null

    fun getClient(): SupabaseClient? {
        if (!SupabaseConfig.isConfigured()) return null
        return cachedClient ?: synchronized(this) {
            cachedClient ?: createClient(
                url = SupabaseConfig.supabaseUrl,
                publishableKey = SupabaseConfig.publishableKey,
                engine = CIO.create()
            ).also { cachedClient = it }
        }
    }

    fun createClient(
        url: String,
        publishableKey: String,
        engine: HttpClientEngine? = null
    ): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = publishableKey
        ) {
            if (engine != null) {
                httpEngine = engine
            }
            install(Auth) {
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Postgrest)
            install(Realtime) {
                reconnectDelay = kotlin.time.Duration.parse("5s")
            }
            install(Storage)
        }
    }

    fun resetForTests() {
        cachedClient = null
    }
}
