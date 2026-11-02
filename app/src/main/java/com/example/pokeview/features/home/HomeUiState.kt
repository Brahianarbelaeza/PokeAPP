package com.example.pokeview.features.home

import com.example.pokeview.domain.model.Pokemon

data class HomeUiState(
    val estaEnLinea: Boolean = false,
    val pokemones: List<Pokemon> = emptyList(),
    val estaCargandoInicial: Boolean = true,
    val estaCargandoMas: Boolean = false,
    val mensajeError: String? = null,
    val hayMasResultados: Boolean = true,
    val textoBusqueda: String = "",
    val resultadosBusqueda: List<Pokemon>? = null,
    val estaBuscando: Boolean = false,
    val mensajeBusquedaError: String? = null,
    val tiposDisponibles: List<String> = emptyList(),
    val habilidadesDisponibles: List<String> = emptyList(),
    val tipoSeleccionado: String? = null,
    val habilidadSeleccionada: String? = null,
    val tipoAplicado: String? = null,
    val habilidadAplicada: String? = null,
    val resultadosFiltros: List<Pokemon>? = null,
    val estaCargandoFiltros: Boolean = false,
    val estaAplicandoFiltros: Boolean = false,
    val mensajeErrorFiltros: String? = null,
    val estaPanelFiltrosExpandido: Boolean = false
)

