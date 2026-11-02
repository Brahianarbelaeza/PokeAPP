package com.example.pokeview.features.detail

import com.example.pokeview.domain.model.Pokemon

data class DetailUiState(
    val pokemon: Pokemon? = null,
    val estaCargando: Boolean = true,
    val mensajeError: String? = null
)

