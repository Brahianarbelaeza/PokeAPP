package com.example.pokeview.data.remote.dto

data class RespuestaListaPokemonDto(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<RecursoNombradoDto> = emptyList()
)

data class RecursoNombradoDto(
    val name: String = "",
    val url: String = ""
)

