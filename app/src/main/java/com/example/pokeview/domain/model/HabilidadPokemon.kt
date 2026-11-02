package com.example.pokeview.domain.model

data class HabilidadPokemon(
    val nombre: String,
    val esOculta: Boolean = false,
    val posicion: Int? = null
)

