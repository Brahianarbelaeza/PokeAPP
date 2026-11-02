package com.example.pokeview.domain.model

data class InformacionPaginacion(
    val limite: Int,
    val desplazamiento: Int,
    val total: Int,
    val siguienteDesplazamiento: Int?,
    val anteriorDesplazamiento: Int?
)

