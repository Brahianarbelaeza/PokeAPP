package com.example.pokeview.data.repository.di

import com.example.pokeview.data.repository.RepositorioPokemonImpl
import com.example.pokeview.domain.repository.RepositorioPokemon
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ModuloRepositorio {

    @Binds
    @Singleton
    abstract fun enlazarRepositorioPokemon(
        implementacion: RepositorioPokemonImpl
    ): RepositorioPokemon
}

