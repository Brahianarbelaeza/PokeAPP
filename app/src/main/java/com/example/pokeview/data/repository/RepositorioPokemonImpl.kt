package com.example.pokeview.data.repository

import com.example.pokeview.data.remote.api.PokemonApi
import com.example.pokeview.data.remote.dto.RecursoNombradoDto
import com.example.pokeview.data.remote.dto.RespuestaPokemonDto
import com.example.pokeview.domain.model.EstadisticaPokemon
import com.example.pokeview.domain.model.HabilidadPokemon
import com.example.pokeview.domain.model.InformacionPaginacion
import com.example.pokeview.domain.model.PaginaPokemon
import com.example.pokeview.domain.model.Pokemon
import com.example.pokeview.domain.model.TipoPokemon
import com.example.pokeview.domain.repository.ErrorRepositorioPokemon
import com.example.pokeview.domain.repository.RepositorioPokemon
import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositorioPokemonImpl @Inject constructor(
    private val pokemonApi: PokemonApi
) : RepositorioPokemon {

    override suspend fun obtenerListado(
        limite: Int,
        desplazamiento: Int
    ): PaginaPokemon = consultar {
        validarPaginacion(limite, desplazamiento)
        val respuesta = pokemonApi.obtenerListadoPokemon(limite, desplazamiento)
        if (respuesta.count < 0) {
            throw ErrorRepositorioPokemon.RespuestaInesperada(
                "PokeAPI devolvió un total de Pokémon inválido"
            )
        }

        PaginaPokemon(
            pokemones = respuesta.results.map { referencia -> mapearReferencia(referencia) },
            paginacion = InformacionPaginacion(
                limite = limite,
                desplazamiento = desplazamiento,
                total = respuesta.count,
                siguienteDesplazamiento = extraerDesplazamiento(respuesta.next),
                anteriorDesplazamiento = extraerDesplazamiento(respuesta.previous)
            )
        )
    }

    override suspend fun obtenerDetalle(nombre: String): Pokemon = consultar {
        val respuesta = pokemonApi.obtenerPokemonPorNombre(validarTexto(nombre, "nombre"))
        mapearDetalle(respuesta)
    }

    override suspend fun obtenerTiposDisponibles(): List<String> = consultar {
        pokemonApi.obtenerListadoTipos().results
            .map(RecursoNombradoDto::name)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    override suspend fun obtenerHabilidadesDisponibles(): List<String> = consultar {
        pokemonApi.obtenerListadoHabilidades().results
            .map(RecursoNombradoDto::name)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    override suspend fun obtenerPorTipo(tipo: String): List<Pokemon> = consultar {
        val respuesta = pokemonApi.obtenerPokemonPorTipo(validarTexto(tipo, "tipo"))
        validarIdentidad(respuesta.id, respuesta.name, "tipo")

        respuesta.pokemon.map { entrada ->
            val referencia = entrada.pokemon
            validarReferencia(referencia)
            mapearReferencia(
                referencia,
                tipos = listOf(
                    TipoPokemon(
                        nombre = respuesta.name,
                        posicion = entrada.slot
                    )
                )
            )
        }
    }

    override suspend fun obtenerPorHabilidad(habilidad: String): List<Pokemon> = consultar {
        val respuesta = pokemonApi.obtenerPokemonPorHabilidad(
            validarTexto(habilidad, "habilidad")
        )
        validarIdentidad(respuesta.id, respuesta.name, "habilidad")

        respuesta.pokemon.map { entrada ->
            val referencia = entrada.pokemon
            validarReferencia(referencia)
            mapearReferencia(
                referencia,
                habilidades = listOf(
                    HabilidadPokemon(
                        nombre = respuesta.name,
                        esOculta = entrada.esOculta,
                        posicion = entrada.slot
                    )
                )
            )
        }
    }

    private fun mapearReferencia(
        referencia: RecursoNombradoDto,
        tipos: List<TipoPokemon> = emptyList(),
        habilidades: List<HabilidadPokemon> = emptyList()
    ): Pokemon {
        validarReferencia(referencia)
        return Pokemon(
            nombre = referencia.name,
            imagenUrl = crearUrlIlustracion(referencia.url),
            tipos = tipos,
            habilidades = habilidades
        )
    }

    private fun crearUrlIlustracion(urlRecurso: String): String? {
        val identificador = urlRecurso
            .trimEnd('/')
            .substringAfterLast('/')
            .toIntOrNull()
            ?: return null

        return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$identificador.png"
    }

    private fun mapearDetalle(respuesta: RespuestaPokemonDto): Pokemon {
        validarIdentidad(respuesta.id, respuesta.name, "Pokémon")
        respuesta.types.forEach { validarReferencia(it.type) }
        respuesta.abilities.forEach { validarReferencia(it.ability) }
        respuesta.stats.forEach { validarReferencia(it.stat) }

        return Pokemon(
            id = respuesta.id,
            nombre = respuesta.name,
            altura = respuesta.height,
            peso = respuesta.weight,
            imagenUrl = respuesta.sprites.other
                ?.ilustracionOficial
                ?.imagenFrontal
                ?: respuesta.sprites.imagenFrontal,
            tipos = respuesta.types.map { entrada ->
                TipoPokemon(
                    nombre = entrada.type.name,
                    posicion = entrada.slot
                )
            },
            habilidades = respuesta.abilities.map { entrada ->
                HabilidadPokemon(
                    nombre = entrada.ability.name,
                    esOculta = entrada.esOculta,
                    posicion = entrada.slot
                )
            },
            estadisticas = respuesta.stats.map { entrada ->
                EstadisticaPokemon(
                    nombre = entrada.stat.name,
                    valorBase = entrada.valorBase,
                    esfuerzo = entrada.effort
                )
            }
        )
    }

    private fun validarReferencia(referencia: RecursoNombradoDto) {
        if (referencia.name.isBlank() || referencia.url.isBlank()) {
            throw ErrorRepositorioPokemon.RespuestaInesperada(
                "PokeAPI devolvió una referencia de recurso incompleta"
            )
        }
    }

    private fun validarIdentidad(id: Int, nombre: String, recurso: String) {
        if (id <= 0 || nombre.isBlank()) {
            throw ErrorRepositorioPokemon.RespuestaInesperada(
                "PokeAPI devolvió la identidad del recurso $recurso incompleta"
            )
        }
    }

    private fun validarTexto(valor: String, campo: String): String {
        if (valor.isBlank()) {
            throw IllegalArgumentException("El $campo no puede estar vacío")
        }
        return valor.trim()
    }

    private fun validarPaginacion(limite: Int, desplazamiento: Int) {
        if (limite <= 0 || desplazamiento < 0) {
            throw IllegalArgumentException(
                "El límite debe ser positivo y el desplazamiento no puede ser negativo"
            )
        }
    }

    private fun extraerDesplazamiento(url: String?): Int? {
        if (url == null) return null

        val consulta = try {
            URI(url).rawQuery
        } catch (error: URISyntaxException) {
            throw ErrorRepositorioPokemon.RespuestaInesperada(
                mensaje = "PokeAPI devolvió un enlace de paginación inválido",
                causa = error
            )
        }
        val valor = consulta
            ?.split("&")
            ?.firstOrNull { it.startsWith("offset=") }
            ?.substringAfter("=")
            ?.toIntOrNull()

        return valor ?: throw ErrorRepositorioPokemon.RespuestaInesperada(
            "PokeAPI devolvió un enlace de paginación inválido"
        )
    }

    private suspend fun <T> consultar(operacion: suspend () -> T): T =
        try {
            operacion()
        } catch (error: HttpException) {
            throw ErrorRepositorioPokemon.ErrorHttp(
                codigo = error.code(),
                respuestaServidor = error.response()?.errorBody()?.string(),
                causa = error
            )
        } catch (error: IOException) {
            throw ErrorRepositorioPokemon.ErrorConexion(error)
        } catch (error: JsonParseException) {
            throw ErrorRepositorioPokemon.RespuestaInesperada(
                mensaje = "PokeAPI devolvió un JSON que no se pudo interpretar",
                causa = error
            )
        }
}

