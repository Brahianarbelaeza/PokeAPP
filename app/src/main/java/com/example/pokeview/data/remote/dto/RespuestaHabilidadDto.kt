package com.example.pokeview.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RespuestaHabilidadDto(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("is_main_series")
    val esDeSeriePrincipal: Boolean = false,
    val pokemon: List<PokemonPorHabilidadDto> = emptyList()
)

data class PokemonPorHabilidadDto(
    @SerializedName("is_hidden")
    val esOculta: Boolean = false,
    val slot: Int = 0,
    val pokemon: RecursoNombradoDto = RecursoNombradoDto()
)

