package com.example.pokeview.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Construye los componentes HTTP compartidos para las llamadas a PokeAPI. */
object ConfiguracionRed {

    const val URL_BASE = "https://pokeapi.co/api/v2/"

    fun crearClienteOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .build()

    fun crearRetrofit(cliente: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}

