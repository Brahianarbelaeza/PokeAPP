package com.example.pokeview.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Observa la red predeterminada y considera en línea solo una conexión validada para Internet. */
@Singleton
class ObservadorConectividad @Inject constructor(
    @ApplicationContext contexto: Context
) {
    private val administradorConectividad =
        contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun consultarEstadoActual(): Boolean =
        administradorConectividad.activeNetwork
            ?.let(administradorConectividad::getNetworkCapabilities)
            ?.let(::tieneInternetValidado)
            ?: false

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarEstadoConexion(): Flow<Boolean> = callbackFlow {
        fun publicarEstadoActual() {
            trySend(consultarEstadoActual())
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(red: Network) {
                val redActiva = administradorConectividad.activeNetwork
                val capacidades = redActiva?.let(administradorConectividad::getNetworkCapabilities)
                trySend(redActiva == red && capacidades?.let(::tieneInternetValidado) == true)
            }

            override fun onLost(red: Network) {
                if (administradorConectividad.activeNetwork == red) {
                    trySend(false)
                } else {
                    publicarEstadoActual()
                }
            }

            override fun onCapabilitiesChanged(
                red: Network,
                capacidades: NetworkCapabilities
            ) {
                if (administradorConectividad.activeNetwork == red) {
                    trySend(tieneInternetValidado(capacidades))
                } else {
                    publicarEstadoActual()
                }
            }

            override fun onUnavailable() = publicarEstadoActual()
        }

        administradorConectividad.registerDefaultNetworkCallback(callback)
        publicarEstadoActual()

        awaitClose {
            administradorConectividad.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private fun tieneInternetValidado(capacidades: NetworkCapabilities): Boolean =
        capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
