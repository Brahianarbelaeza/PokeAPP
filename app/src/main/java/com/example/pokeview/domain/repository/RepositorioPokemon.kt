package com.example.pokeview.domain.repository

import com.example.pokeview.domain.model.PaginaPokemon
import com.example.pokeview.domain.model.Pokemon

interface RepositorioPokemon {

    suspend fun obtenerListado(limite: Int = 20, desplazamiento: Int = 0): PaginaPokemon

    suspend fun obtenerDetalle(nombre: String): Pokemon

    suspend fun obtenerTiposDisponibles(): List<String>

    suspend fun obtenerHabilidadesDisponibles(): List<String>

    suspend fun obtenerPorTipo(tipo: String): List<Pokemon>

    suspend fun obtenerPorHabilidad(habilidad: String): List<Pokemon>
}

