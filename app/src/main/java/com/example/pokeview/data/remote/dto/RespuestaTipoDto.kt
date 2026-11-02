package com.example.pokeview.data.remote.dto

data class RespuestaTipoDto(
    val id: Int = 0,
    val name: String = "",
    val pokemon: List<PokemonPorTipoDto> = emptyList()
)

data class PokemonPorTipoDto(
    val slot: Int = 0,
    val pokemon: RecursoNombradoDto = RecursoNombradoDto()
)

