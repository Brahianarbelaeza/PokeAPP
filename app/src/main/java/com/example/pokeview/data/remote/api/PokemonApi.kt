package com.example.pokeview.data.remote.api

import com.example.pokeview.data.remote.dto.RespuestaHabilidadDto
import com.example.pokeview.data.remote.dto.RespuestaListaPokemonDto
import com.example.pokeview.data.remote.dto.RespuestaPokemonDto
import com.example.pokeview.data.remote.dto.RespuestaTipoDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Contrato Retrofit para consultar los recursos principales de PokeAPI. */
interface PokemonApi {

    @GET("pokemon")
    suspend fun obtenerListadoPokemon(
        @Query("limit") limite: Int = 20,
        @Query("offset") desplazamiento: Int = 0
    ): RespuestaListaPokemonDto

    @GET("type")
    suspend fun obtenerListadoTipos(
        @Query("limit") limite: Int = 100
    ): RespuestaListaPokemonDto

    @GET("ability")
    suspend fun obtenerListadoHabilidades(
        @Query("limit") limite: Int = 1000
    ): RespuestaListaPokemonDto

    @GET("pokemon/{nombre}")
    suspend fun obtenerPokemonPorNombre(
        @Path("nombre") nombre: String
    ): RespuestaPokemonDto

    @GET("type/{tipo}")
    suspend fun obtenerPokemonPorTipo(
        @Path("tipo") tipo: String
    ): RespuestaTipoDto

    @GET("ability/{habilidad}")
    suspend fun obtenerPokemonPorHabilidad(
        @Path("habilidad") habilidad: String
    ): RespuestaHabilidadDto
}

