package com.example.pokeview.features.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeview.domain.model.Pokemon
import com.example.pokeview.domain.repository.ErrorRepositorioPokemon
import com.example.pokeview.domain.repository.RepositorioPokemon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repositorioPokemon: RepositorioPokemon,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val nombrePokemon =
        savedStateHandle.get<String>(ArgumentosDetalle.NOMBRE_POKEMON).orEmpty()

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var solicitudActiva = false

    init {
        cargarDetalle()
    }

    fun cargarDetalle() {
        if (solicitudActiva) return

        if (nombrePokemon.isBlank()) {
            _uiState.update {
                it.copy(
                    estaCargando = false,
                    mensajeError = "No se recibió el nombre del Pokémon."
                )
            }
            return
        }

        solicitudActiva = true
        _uiState.update { it.copy(estaCargando = true, mensajeError = null) }

        viewModelScope.launch {
            try {
                val pokemon: Pokemon = repositorioPokemon.obtenerDetalle(nombrePokemon)
                _uiState.update {
                    it.copy(pokemon = pokemon, mensajeError = null)
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                _uiState.update { it.copy(mensajeError = obtenerMensajeError(error)) }
            } finally {
                solicitudActiva = false
                _uiState.update { it.copy(estaCargando = false) }
            }
        }
    }

    private fun obtenerMensajeError(error: Exception): String =
        when (error) {
            is ErrorRepositorioPokemon.ErrorHttp ->
                "PokeAPI respondió con el código HTTP ${error.codigo}."

            is ErrorRepositorioPokemon.ErrorConexion ->
                "No se pudo conectar. Revisa tu conexión e inténtalo de nuevo."

            is ErrorRepositorioPokemon.RespuestaInesperada ->
                "PokeAPI devolvió una respuesta inesperada."

            else -> error.message ?: "Ocurrió un error al cargar el Pokémon."
        }
}

