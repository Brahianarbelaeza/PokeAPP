package com.example.pokeview.domain.model

data class Pokemon(
    val nombre: String,
    val id: Int? = null,
    val altura: Int? = null,
    val peso: Int? = null,
    val imagenUrl: String? = null,
    val tipos: List<TipoPokemon> = emptyList(),
    val habilidades: List<HabilidadPokemon> = emptyList(),
    val estadisticas: List<EstadisticaPokemon> = emptyList()
)

