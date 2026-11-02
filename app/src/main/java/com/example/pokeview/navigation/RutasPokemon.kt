package com.example.pokeview.navigation

import android.net.Uri
import com.example.pokeview.features.detail.ArgumentosDetalle

object RutasPokemon {
    const val HOME = "home"
    const val DETALLE = "detail/{${ArgumentosDetalle.NOMBRE_POKEMON}}"

    fun detalle(nombrePokemon: String): String =
        "detail/${Uri.encode(nombrePokemon)}"
}

