package com.example.collegemanagementsystemfaculty.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NetworkMonitor {

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun start(context: Context) {
        val appCtx = context.applicationContext
        val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        _isConnected.value = hasInternet(cm)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = true
            }
            override fun onLost(network: Network) {
                _isConnected.value = hasInternet(cm)
            }
            override fun onUnavailable() {
                _isConnected.value = false
            }
        })

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        appCtx.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                _isConnected.value = hasInternet(cm)
            }
        }, filter)
    }

    private fun hasInternet(cm: ConnectivityManager): Boolean {
        val n = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(n) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
