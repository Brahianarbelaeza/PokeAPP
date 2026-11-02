package com.example.pokeview.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeview.core.util.ObservadorConectividad
import com.example.pokeview.domain.model.Pokemon
import com.example.pokeview.domain.repository.ErrorRepositorioPokemon
import com.example.pokeview.domain.repository.RepositorioPokemon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repositorioPokemon: RepositorioPokemon,
    observadorConectividad: ObservadorConectividad
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(estaEnLinea = observadorConectividad.consultarEstadoActual())
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val candadoCarga = Mutex()
    private var siguienteDesplazamiento: Int? = 0
    private val consultasBusqueda = MutableStateFlow(CriteriosBusqueda())
    private var revisionBusqueda = 0
    private var trabajoAplicarFiltros: Job? = null

    init {
        viewModelScope.launch {
            observadorConectividad.observarEstadoConexion().collect { estaEnLinea ->
                _uiState.update { it.copy(estaEnLinea = estaEnLinea) }
            }
        }
        cargarPrimeraPagina()
        cargarOpcionesFiltros()
        viewModelScope.launch {
            consultasBusqueda
                .collectLatest { criterios ->
                    delay(DEMORA_BUSQUEDA_MS)
                    buscarPokemon(criterios.texto, criterios.revision)
                }
        }
    }

    fun actualizarBusqueda(texto: String) {
        val consulta = texto.trim()
        val base = obtenerPokemonesParaBusqueda(_uiState.value)
        val coincidencias = if (consulta.isBlank()) {
            emptyList()
        } else {
            buscarCoincidencias(base, consulta)
        }

        _uiState.update {
            it.copy(
                textoBusqueda = texto,
                resultadosBusqueda = if (consulta.isBlank()) null else coincidencias,
                estaBuscando = consulta.isNotBlank() && coincidencias.isEmpty(),
                mensajeBusquedaError = null
            )
        }
        actualizarCriteriosBusqueda(texto)
    }

    fun alternarPanelFiltros() {
        _uiState.update {
            it.copy(estaPanelFiltrosExpandido = !it.estaPanelFiltrosExpandido)
        }
    }

    fun seleccionarTipo(tipo: String?) {
        _uiState.update {
            if (it.estaAplicandoFiltros) it else it.copy(tipoSeleccionado = tipo)
        }
    }

    fun seleccionarHabilidad(habilidad: String?) {
        _uiState.update {
            if (it.estaAplicandoFiltros) it else it.copy(habilidadSeleccionada = habilidad)
        }
    }

    fun aplicarFiltros() {
        val estado = _uiState.value
        if (estado.estaAplicandoFiltros) return

        val tipo = estado.tipoSeleccionado
        val habilidad = estado.habilidadSeleccionada
        if (tipo == null && habilidad == null) {
            limpiarFiltros()
            return
        }

        _uiState.update { it.copy(estaAplicandoFiltros = true, mensajeErrorFiltros = null) }
        trabajoAplicarFiltros = viewModelScope.launch {
            try {
                val resultados = coroutineScope {
                    when {
                        tipo != null && habilidad != null -> {
                            val porTipo = async { repositorioPokemon.obtenerPorTipo(tipo) }
                            val porHabilidad = async {
                                repositorioPokemon.obtenerPorHabilidad(habilidad)
                            }
                            val nombresConHabilidad = porHabilidad.await()
                                .mapTo(mutableSetOf(), Pokemon::nombre)
                            porTipo.await().filter { it.nombre in nombresConHabilidad }
                        }

                        tipo != null -> repositorioPokemon.obtenerPorTipo(tipo)
                        else -> repositorioPokemon.obtenerPorHabilidad(checkNotNull(habilidad))
                    }
                }
                val consulta = _uiState.value.textoBusqueda.trim()
                val coincidencias = if (consulta.isBlank()) {
                    null
                } else {
                    buscarCoincidencias(resultados, consulta)
                }
                _uiState.update {
                    it.copy(
                        tipoAplicado = tipo,
                        habilidadAplicada = habilidad,
                        resultadosFiltros = resultados,
                        resultadosBusqueda = coincidencias,
                        estaBuscando = consulta.isNotBlank() && coincidencias.isNullOrEmpty(),
                        estaAplicandoFiltros = false,
                        mensajeErrorFiltros = null,
                        estaPanelFiltrosExpandido = false
                    )
                }
                actualizarCriteriosBusqueda(_uiState.value.textoBusqueda)
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        estaAplicandoFiltros = false,
                        mensajeErrorFiltros = obtenerMensajeError(error)
                    )
                }
            }
        }
    }

    fun limpiarFiltros() {
        trabajoAplicarFiltros?.cancel()
        trabajoAplicarFiltros = null
        val texto = _uiState.value.textoBusqueda
        val consulta = texto.trim()
        val coincidencias = if (consulta.isBlank()) {
            null
        } else {
            buscarCoincidencias(_uiState.value.pokemones, consulta)
        }
        _uiState.update {
            it.copy(
                tipoSeleccionado = null,
                habilidadSeleccionada = null,
                tipoAplicado = null,
                habilidadAplicada = null,
                resultadosFiltros = null,
                resultadosBusqueda = coincidencias,
                estaBuscando = consulta.isNotBlank() && coincidencias.isNullOrEmpty(),
                estaAplicandoFiltros = false,
                mensajeErrorFiltros = null
            )
        }
        actualizarCriteriosBusqueda(texto)
    }

    fun reintentarCargaFiltros() {
        if (_uiState.value.tipoSeleccionado != null ||
            _uiState.value.habilidadSeleccionada != null
        ) {
            aplicarFiltros()
        } else {
            cargarOpcionesFiltros()
        }
    }

    fun cargarMasPokemones() {
        if (!candadoCarga.tryLock()) return

        val estadoActual = _uiState.value
        val desplazamiento = siguienteDesplazamiento
        if (
            !estadoActual.hayMasResultados ||
            desplazamiento == null ||
            estadoActual.resultadosFiltros != null ||
            estadoActual.textoBusqueda.isNotBlank()
        ) {
            candadoCarga.unlock()
            return
        }

        _uiState.update {
            it.copy(estaCargandoMas = true, mensajeError = null)
        }

        viewModelScope.launch {
            try {
                val pagina = repositorioPokemon.obtenerListado(
                    limite = TAMANO_PAGINA,
                    desplazamiento = desplazamiento
                )
                siguienteDesplazamiento = pagina.paginacion.siguienteDesplazamiento
                _uiState.update { estado ->
                    val pokemonesActualizados = (estado.pokemones + pagina.pokemones)
                        .distinctBy(Pokemon::nombre)
                    val consulta = estado.textoBusqueda.trim()
                    val baseBusqueda = estado.resultadosFiltros ?: pokemonesActualizados
                    val coincidencias = if (consulta.isBlank()) {
                        emptyList()
                    } else {
                        buscarCoincidencias(baseBusqueda, consulta)
                    }
                    estado.copy(
                        pokemones = pokemonesActualizados,
                        hayMasResultados = siguienteDesplazamiento != null,
                        mensajeError = null,
                        resultadosBusqueda = when {
                            consulta.isBlank() -> null
                            coincidencias.isNotEmpty() -> coincidencias
                            else -> estado.resultadosBusqueda
                        },
                        estaBuscando = if (coincidencias.isNotEmpty()) {
                            false
                        } else {
                            estado.estaBuscando
                        }
                    )
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                _uiState.update { it.copy(mensajeError = obtenerMensajeError(error)) }
            } finally {
                _uiState.update { it.copy(estaCargandoMas = false) }
                candadoCarga.unlock()
            }
        }
    }

    fun reintentarCarga() {
        if (_uiState.value.pokemones.isEmpty()) {
            cargarPrimeraPagina()
        } else {
            cargarMasPokemones()
        }
    }

    private fun cargarPrimeraPagina() {
        if (!candadoCarga.tryLock()) return

        _uiState.update {
            it.copy(estaCargandoInicial = true, mensajeError = null)
        }

        viewModelScope.launch {
            try {
                val pagina = repositorioPokemon.obtenerListado(
                    limite = TAMANO_PAGINA,
                    desplazamiento = 0
                )
                siguienteDesplazamiento = pagina.paginacion.siguienteDesplazamiento
                _uiState.update { estado ->
                    val consulta = estado.textoBusqueda.trim()
                    val baseBusqueda = estado.resultadosFiltros ?: pagina.pokemones
                    val coincidencias = if (consulta.isBlank()) {
                        emptyList()
                    } else {
                        buscarCoincidencias(baseBusqueda, consulta)
                    }
                    estado.copy(
                        pokemones = pagina.pokemones,
                        hayMasResultados = siguienteDesplazamiento != null,
                        mensajeError = null,
                        resultadosBusqueda = if (consulta.isBlank()) null else coincidencias,
                        estaBuscando = estado.estaBuscando && coincidencias.isEmpty()
                    )
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                _uiState.update { it.copy(mensajeError = obtenerMensajeError(error)) }
            } finally {
                _uiState.update { it.copy(estaCargandoInicial = false) }
                candadoCarga.unlock()
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

            else -> error.message ?: "Ocurrió un error al cargar los Pokémon."
        }

    private fun cargarOpcionesFiltros() {
        if (_uiState.value.estaCargandoFiltros) return
        _uiState.update { it.copy(estaCargandoFiltros = true, mensajeErrorFiltros = null) }
        viewModelScope.launch {
            try {
                val (tipos, habilidades) = coroutineScope {
                    val cargaTipos = async { repositorioPokemon.obtenerTiposDisponibles() }
                    val cargaHabilidades = async {
                        repositorioPokemon.obtenerHabilidadesDisponibles()
                    }
                    cargaTipos.await() to cargaHabilidades.await()
                }
                _uiState.update {
                    it.copy(
                        tiposDisponibles = tipos,
                        habilidadesDisponibles = habilidades,
                        estaCargandoFiltros = false,
                        mensajeErrorFiltros = null
                    )
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        estaCargandoFiltros = false,
                        mensajeErrorFiltros = obtenerMensajeError(error)
                    )
                }
            }
        }
    }

    private fun actualizarCriteriosBusqueda(texto: String) {
        revisionBusqueda += 1
        consultasBusqueda.value = CriteriosBusqueda(texto, revisionBusqueda)
    }

    private fun obtenerPokemonesParaBusqueda(estado: HomeUiState): List<Pokemon> =
        estado.resultadosFiltros ?: estado.pokemones

    private fun esBusquedaActual(revision: Int): Boolean = revision == revisionBusqueda

    private fun cumpleFiltrosAplicados(pokemon: Pokemon, estado: HomeUiState): Boolean =
        (estado.tipoAplicado == null ||
            pokemon.tipos.any { it.nombre.equals(estado.tipoAplicado, ignoreCase = true) }) &&
            (estado.habilidadAplicada == null ||
                pokemon.habilidades.any {
                    it.nombre.equals(estado.habilidadAplicada, ignoreCase = true)
                })

    private suspend fun buscarPokemon(texto: String, revision: Int) {
        val consulta = texto.trim()
        if (consulta.isBlank()) {
            _uiState.update {
                if (esBusquedaActual(revision) && it.textoBusqueda.isBlank()) {
                    it.copy(
                        resultadosBusqueda = null,
                        estaBuscando = false,
                        mensajeBusquedaError = null
                    )
                } else {
                    it
                }
            }
            return
        }

        val estadoBusqueda = _uiState.value
        if (!esBusquedaActual(revision)) return
        val pokemonesBase = obtenerPokemonesParaBusqueda(estadoBusqueda)
        val coincidenciasLocales = buscarCoincidencias(pokemonesBase, consulta)
        if (coincidenciasLocales.isNotEmpty()) {
            actualizarResultadosSiConsultaActual(
                consulta = consulta,
                revision = revision,
                resultados = coincidenciasLocales,
                error = null
            )
            return
        }

        if (!esBusquedaActual(revision)) return
        _uiState.update { estado ->
            if (esBusquedaActual(revision)) {
                estado.copy(
                    resultadosBusqueda = emptyList(),
                    estaBuscando = true,
                    mensajeBusquedaError = null
                )
            } else {
                estado
            }
        }

        try {
            val pokemon = repositorioPokemon.obtenerDetalle(consulta)
            val estadoActual = _uiState.value
            if (!esBusquedaActual(revision)) return

            val coincidenciasActuales = buscarCoincidencias(
                obtenerPokemonesParaBusqueda(estadoActual),
                consulta
            )
            val resultados = when {
                coincidenciasActuales.any { it.nombre.equals(consulta, ignoreCase = true) } ->
                    coincidenciasActuales

                pokemon.nombre.equals(consulta, ignoreCase = true) &&
                    cumpleFiltrosAplicados(pokemon, estadoActual) -> listOf(pokemon)
                else -> coincidenciasActuales
            }
            actualizarResultadosSiConsultaActual(
                consulta = consulta,
                revision = revision,
                resultados = resultados,
                error = null
            )
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (error: Exception) {
            val estadoActual = _uiState.value
            if (!esBusquedaActual(revision)) return

            val coincidenciasActuales = buscarCoincidencias(
                obtenerPokemonesParaBusqueda(estadoActual),
                consulta
            )
            val esNoEncontrado =
                error is ErrorRepositorioPokemon.ErrorHttp && error.codigo == 404
            actualizarResultadosSiConsultaActual(
                consulta = consulta,
                revision = revision,
                resultados = coincidenciasActuales,
                error = if (esNoEncontrado) null else obtenerMensajeErrorBusqueda(error)
            )
        }
    }

    private fun buscarCoincidencias(
        pokemones: List<Pokemon>,
        consulta: String
    ): List<Pokemon> {
        val pokemonExacto = pokemones.firstOrNull {
            it.nombre.equals(consulta, ignoreCase = true)
        }
        return if (pokemonExacto != null) {
            listOf(pokemonExacto)
        } else {
            pokemones.filter { it.nombre.contains(consulta, ignoreCase = true) }
        }
    }

    private fun actualizarResultadosSiConsultaActual(
        consulta: String,
        revision: Int,
        resultados: List<Pokemon>,
        error: String?
    ) {
        _uiState.update { estado ->
            if (esBusquedaActual(revision) &&
                esConsultaActual(estado.textoBusqueda, consulta)
            ) {
                estado.copy(
                    resultadosBusqueda = resultados,
                    estaBuscando = false,
                    mensajeBusquedaError = error
                )
            } else {
                estado
            }
        }
    }

    private fun esConsultaActual(textoActual: String, consulta: String): Boolean =
        textoActual.trim().equals(consulta, ignoreCase = true)

    private fun obtenerMensajeErrorBusqueda(error: Exception): String =
        when (error) {
            is ErrorRepositorioPokemon.ErrorConexion ->
                "No se pudo consultar el nombre exacto."

            is ErrorRepositorioPokemon.RespuestaInesperada ->
                "La respuesta de búsqueda no fue válida."

            is ErrorRepositorioPokemon.ErrorHttp ->
                "No se pudo completar la búsqueda (HTTP ${error.codigo})."

            else -> "No se pudo completar la búsqueda."
        }

    private companion object {
        const val TAMANO_PAGINA = 20
        const val DEMORA_BUSQUEDA_MS = 450L
    }

    private data class CriteriosBusqueda(
        val texto: String = "",
        val revision: Int = 0
    )
}

