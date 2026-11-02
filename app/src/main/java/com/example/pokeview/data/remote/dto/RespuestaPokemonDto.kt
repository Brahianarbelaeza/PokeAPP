package com.example.pokeview.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RespuestaPokemonDto(
    val id: Int = 0,
    val name: String = "",
    val height: Int = 0,
    val weight: Int = 0,
    val sprites: SpritesPokemonDto = SpritesPokemonDto(),
    val types: List<TipoEnPokemonDto> = emptyList(),
    val abilities: List<HabilidadEnPokemonDto> = emptyList(),
    val stats: List<EstadisticaPokemonDto> = emptyList()
)

data class SpritesPokemonDto(
    @SerializedName("front_default")
    val imagenFrontal: String? = null,
    val other: OtrosSpritesDto? = null
)

data class OtrosSpritesDto(
    @SerializedName("official-artwork")
    val ilustracionOficial: IlustracionOficialDto? = null
)

data class IlustracionOficialDto(
    @SerializedName("front_default")
    val imagenFrontal: String? = null
)

data class TipoEnPokemonDto(
    val slot: Int = 0,
    val type: RecursoNombradoDto = RecursoNombradoDto()
)

data class HabilidadEnPokemonDto(
    @SerializedName("is_hidden")
    val esOculta: Boolean = false,
    val slot: Int = 0,
    val ability: RecursoNombradoDto = RecursoNombradoDto()
)

data class EstadisticaPokemonDto(
    @SerializedName("base_stat")
    val valorBase: Int = 0,
    val effort: Int = 0,
    val stat: RecursoNombradoDto = RecursoNombradoDto()
)

