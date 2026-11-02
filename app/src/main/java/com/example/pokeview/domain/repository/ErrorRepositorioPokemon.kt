package com.example.pokeview.domain.repository

sealed class ErrorRepositorioPokemon(
    mensaje: String,
    causa: Throwable? = null
) : Exception(mensaje, causa) {

    class ErrorHttp(
        val codigo: Int,
        val respuestaServidor: String?,
        causa: Throwable
    ) : ErrorRepositorioPokemon(
        mensaje = "PokeAPI respondió con el código HTTP $codigo",
        causa = causa
    )

    class ErrorConexion(causa: Throwable) : ErrorRepositorioPokemon(
        mensaje = "No fue posible conectar con PokeAPI",
        causa = causa
    )

    class RespuestaInesperada(
        mensaje: String,
        causa: Throwable? = null
    ) : ErrorRepositorioPokemon(mensaje, causa)
}

