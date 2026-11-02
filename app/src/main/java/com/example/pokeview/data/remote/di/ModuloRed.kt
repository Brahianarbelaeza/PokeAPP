package com.example.pokeview.data.remote.di

import com.example.pokeview.data.remote.ConfiguracionRed
import com.example.pokeview.data.remote.api.PokemonApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModuloRed {

    @Provides
    @Singleton
    fun proporcionarClienteOkHttp(): OkHttpClient =
        ConfiguracionRed.crearClienteOkHttp()

    @Provides
    @Singleton
    fun proporcionarRetrofit(cliente: OkHttpClient): Retrofit =
        ConfiguracionRed.crearRetrofit(cliente)

    @Provides
    @Singleton
    fun proporcionarPokemonApi(retrofit: Retrofit): PokemonApi =
        retrofit.create(PokemonApi::class.java)
}

