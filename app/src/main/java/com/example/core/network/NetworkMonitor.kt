package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object NetworkMonitor {

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return true
            }
        }
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    fun observeNetwork(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isOnline(context))
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        trySend(isOnline(context))

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            trySend(isOnline(context))
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()

    suspend fun checkRealInternetAccess(context: Context? = null): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val targets = mutableListOf<String>()
        val serverCandidates = ServerConfig.getCandidateUrls()
        for (base in serverCandidates) {
            targets.add("${base.removeSuffix("/")}/.json?shallow=true")
        }
        targets.add("https://clients3.google.com/generate_204")
        targets.add("https://www.google.com/generate_204")
        targets.add("https://www.cloudflare.com/cdn-cgi/trace")

        for (target in targets) {
            try {
                val url = java.net.URL(target)
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 2500
                    readTimeout = 2500
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    useCaches = false
                }
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..399 || code == 401 || code == 403) return@withContext true
            } catch (_: Exception) {
                // try next target
            }
        }
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("8.8.8.8", 53), 1500)
                return@withContext true
            }
        } catch (_: Exception) {}
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("1.1.1.1", 53), 1500)
                return@withContext true
            }
        } catch (_: Exception) {}

        // Fallback: if Android connectivity manager reports active network is online
        if (context != null && isOnline(context)) {
            return@withContext true
        }
        false
    }
}

@Composable
fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current
    return produceState(initialValue = NetworkMonitor.isOnline(context)) {
        NetworkMonitor.observeNetwork(context).collect { value = it }
    }
}
