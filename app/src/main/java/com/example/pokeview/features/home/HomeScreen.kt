package com.example.pokeview.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.pokeview.core.theme.PokeViewTheme
import com.example.pokeview.domain.model.Pokemon
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPokemonSeleccionado: (Pokemon) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        estado = estado,
        onCargarMas = viewModel::cargarMasPokemones,
        onReintentar = viewModel::reintentarCarga,
        onBusquedaCambiada = viewModel::actualizarBusqueda,
        onAlternarFiltros = viewModel::alternarPanelFiltros,
        onSeleccionarTipo = viewModel::seleccionarTipo,
        onSeleccionarHabilidad = viewModel::seleccionarHabilidad,
        onAplicarFiltros = viewModel::aplicarFiltros,
        onLimpiarFiltros = viewModel::limpiarFiltros,
        onReintentarFiltros = viewModel::reintentarCargaFiltros,
        onPokemonSeleccionado = onPokemonSeleccionado,
        modifier = modifier
    )
}

@Composable
private fun HomeContent(
    estado: HomeUiState,
    onCargarMas: () -> Unit,
    onReintentar: () -> Unit,
    onBusquedaCambiada: (String) -> Unit,
    onAlternarFiltros: () -> Unit,
    onSeleccionarTipo: (String?) -> Unit,
    onSeleccionarHabilidad: (String?) -> Unit,
    onAplicarFiltros: () -> Unit,
    onLimpiarFiltros: () -> Unit,
    onReintentarFiltros: () -> Unit,
    onPokemonSeleccionado: (Pokemon) -> Unit,
    modifier: Modifier = Modifier
) {
    val estadoCuadricula = rememberLazyGridState()

    LaunchedEffect(
        estado.pokemones.size,
        estado.textoBusqueda,
        estado.tipoAplicado,
        estado.habilidadAplicada,
        estado.estaCargandoInicial,
        estado.estaCargandoMas,
        estado.hayMasResultados,
        estado.mensajeError
    ) {
        snapshotFlow {
            estadoCuadricula.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.distinctUntilChanged().collect { indiceVisible ->
            val cercaDelFinal = indiceVisible >= estado.pokemones.size - UMBRAL_PAGINACION
            if (
                estado.textoBusqueda.isBlank() &&
                estado.tipoAplicado == null &&
                estado.habilidadAplicada == null &&
                cercaDelFinal &&
                estado.hayMasResultados &&
                !estado.estaCargandoInicial &&
                !estado.estaCargandoMas &&
                estado.mensajeError == null &&
                estado.pokemones.isNotEmpty()
            ) {
                onCargarMas()
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            EncabezadoHome()
            IndicadorConexion(estaEnLinea = estado.estaEnLinea)
            BarraBusquedaHome(
                texto = estado.textoBusqueda,
                panelExpandido = estado.estaPanelFiltrosExpandido,
                cantidadFiltrosAplicados = listOfNotNull(
                    estado.tipoAplicado,
                    estado.habilidadAplicada
                ).size,
                onTextoCambiado = onBusquedaCambiada,
                onAlternarFiltros = onAlternarFiltros
            )

            if (estado.estaPanelFiltrosExpandido) {
                PanelFiltrosHome(
                    estado = estado,
                    onSeleccionarTipo = onSeleccionarTipo,
                    onSeleccionarHabilidad = onSeleccionarHabilidad,
                    onAplicar = onAplicarFiltros,
                    onLimpiar = onLimpiarFiltros,
                    onReintentar = onReintentarFiltros
                )
            }

            val listaBase = estado.resultadosFiltros ?: estado.pokemones
            val pokemonesVisibles = estado.resultadosBusqueda ?: listaBase

            when {
                estado.textoBusqueda.isBlank() &&
                    estado.tipoAplicado == null && estado.habilidadAplicada == null &&
                    estado.estaCargandoInicial && estado.pokemones.isEmpty() -> {
                    EstadoCargaInicial(modifier = Modifier.weight(1f))
                }

                estado.textoBusqueda.isBlank() &&
                    estado.tipoAplicado == null && estado.habilidadAplicada == null &&
                    estado.mensajeError != null && estado.pokemones.isEmpty() -> {
                    EstadoErrorHome(
                        mensaje = estado.mensajeError,
                        onReintentar = onReintentar,
                        modifier = Modifier.weight(1f)
                    )
                }

                estado.textoBusqueda.isNotBlank() &&
                    estado.estaBuscando && pokemonesVisibles.isEmpty() -> {
                    EstadoBuscando(modifier = Modifier.weight(1f))
                }

                estado.textoBusqueda.isNotBlank() &&
                    estado.mensajeBusquedaError != null && pokemonesVisibles.isEmpty() -> {
                    EstadoErrorBusqueda(
                        mensaje = estado.mensajeBusquedaError,
                        modifier = Modifier.weight(1f)
                    )
                }

                estado.textoBusqueda.isNotBlank() && pokemonesVisibles.isEmpty() -> {
                    EstadoSinCoincidencias(
                        consulta = estado.textoBusqueda,
                        modifier = Modifier.weight(1f)
                    )
                }

                estado.textoBusqueda.isBlank() && pokemonesVisibles.isEmpty() -> {
                    EstadoVacioHome(modifier = Modifier.weight(1f))
                }

                else -> {
                    if (estado.textoBusqueda.isBlank() &&
                        estado.tipoAplicado == null && estado.habilidadAplicada == null &&
                        estado.mensajeError != null
                    ) {
                        AvisoErrorPaginacion(
                            mensaje = estado.mensajeError,
                            onReintentar = onReintentar
                        )
                    }
                    if (estado.estaAplicandoFiltros) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = " Aplicando filtros…",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (estado.mensajeBusquedaError != null) {
                        Text(
                            text = estado.mensajeBusquedaError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = estadoCuadricula,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 18.dp,
                            end = 18.dp,
                            top = 8.dp,
                            bottom = 24.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = pokemonesVisibles,
                            key = Pokemon::nombre
                        ) { pokemon ->
                            TarjetaPokemon(
                                pokemon = pokemon,
                                onClick = { onPokemonSeleccionado(pokemon) }
                            )
                        }

                        if (
                            estado.textoBusqueda.isBlank() &&
                            estado.tipoAplicado == null &&
                            estado.habilidadAplicada == null &&
                            estado.estaCargandoMas
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                IndicadorCargaAdicional()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicadorConexion(estaEnLinea: Boolean) {
    val colorIndicador = if (estaEnLinea) Color(0xFF2E7D32) else Color(0xFFC62828)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = colorIndicador)
        }
        Text(
            text = if (estaEnLinea) "En línea" else "Sin conexión",
            modifier = Modifier.padding(start = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BarraBusquedaHome(
    texto: String,
    panelExpandido: Boolean,
    cantidadFiltrosAplicados: Int,
    onTextoCambiado: (String) -> Unit,
    onAlternarFiltros: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = texto,
            onValueChange = onTextoCambiado,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text("Buscar Pokémon") },
            leadingIcon = { IconoBusqueda() },
            trailingIcon = {
                if (texto.isNotEmpty()) {
                    IconButton(onClick = { onTextoCambiado("") }) {
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics {
                                contentDescription = "Limpiar búsqueda"
                            }
                        )
                    }
                }
            }
        )
        IconButton(
            onClick = onAlternarFiltros,
            modifier = Modifier.semantics {
                contentDescription = if (panelExpandido) "Cerrar filtros" else "Abrir filtros"
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                IconoFiltros()
                if (cantidadFiltrosAplicados > 0) {
                    Text(
                        text = cantidadFiltrosAplicados.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun IconoFiltros() {
    val colorIcono = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(24.dp)) {
        val grosor = 2.dp.toPx()
        listOf(0.28f, 0.5f, 0.72f).forEachIndexed { indice, fraccionY ->
            val inicioX = size.width * 0.12f
            val finX = size.width * 0.88f
            val y = size.height * fraccionY
            drawLine(colorIcono, Offset(inicioX, y), Offset(finX, y), grosor)
            val centroX = size.width * listOf(0.36f, 0.68f, 0.44f)[indice]
            drawCircle(colorIcono, radius = grosor * 1.7f, center = Offset(centroX, y))
        }
    }
}

@Composable
private fun PanelFiltrosHome(
    estado: HomeUiState,
    onSeleccionarTipo: (String?) -> Unit,
    onSeleccionarHabilidad: (String?) -> Unit,
    onAplicar: () -> Unit,
    onLimpiar: () -> Unit,
    onReintentar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 4.dp)
    ) {
        Text("Tipo", style = MaterialTheme.typography.titleSmall)
        when {
            estado.estaCargandoFiltros -> Text(
                "Cargando opciones de filtro…",
                style = MaterialTheme.typography.bodySmall
            )

            estado.tiposDisponibles.isNotEmpty() -> LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(estado.tiposDisponibles, key = { it }) { tipo ->
                    FilterChip(
                        selected = estado.tipoSeleccionado == tipo,
                        enabled = !estado.estaAplicandoFiltros,
                        onClick = {
                            onSeleccionarTipo(if (estado.tipoSeleccionado == tipo) null else tipo)
                        },
                        label = { Text(tipo.replaceFirstChar { it.titlecase() }) }
                    )
                }
            }
        }

        SelectorHabilidad(
            habilidades = estado.habilidadesDisponibles,
            seleccionada = estado.habilidadSeleccionada,
            habilitado = !estado.estaCargandoFiltros && !estado.estaAplicandoFiltros,
            onSeleccionar = onSeleccionarHabilidad
        )

        if (estado.mensajeErrorFiltros != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    estado.mensajeErrorFiltros,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Button(onClick = onReintentar) { Text("Reintentar") }
            }
        }

        Text(
            text = "Seleccionados: ${estado.tipoSeleccionado ?: "sin tipo"} · " +
                (estado.habilidadSeleccionada ?: "sin habilidad"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onLimpiar,
                modifier = Modifier.weight(1f)
            ) { Text("Limpiar filtros") }
            Button(
                onClick = onAplicar,
                enabled = !estado.estaAplicandoFiltros,
                modifier = Modifier.weight(1f)
            ) {
                if (estado.estaAplicandoFiltros) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Aplicar filtros")
                }
            }
        }
    }
}

@Composable
private fun SelectorHabilidad(
    habilidades: List<String>,
    seleccionada: String?,
    habilitado: Boolean,
    onSeleccionar: (String?) -> Unit
) {
    var menuExpandido by remember { mutableStateOf(false) }
    Text("Habilidad", style = MaterialTheme.typography.titleSmall)
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { menuExpandido = true },
            enabled = habilitado && habilidades.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(seleccionada?.replaceFirstChar { it.titlecase() } ?: "Elegir habilidad")
        }
        DropdownMenu(
            expanded = menuExpandido,
            onDismissRequest = { menuExpandido = false },
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            if (seleccionada != null) {
                DropdownMenuItem(
                    text = { Text("Quitar habilidad") },
                    onClick = {
                        onSeleccionar(null)
                        menuExpandido = false
                    }
                )
            }
            habilidades.forEach { habilidad ->
                DropdownMenuItem(
                    text = { Text(habilidad.replaceFirstChar { it.titlecase() }) },
                    onClick = {
                        onSeleccionar(habilidad)
                        menuExpandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun IconoBusqueda() {
    val colorIcono = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = Modifier
            .size(22.dp)
            .semantics { contentDescription = "Buscar" }
    ) {
        val grosor = 2.dp.toPx()
        drawCircle(
            color = colorIcono,
            radius = size.minDimension * 0.31f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = grosor)
        )
        drawLine(
            color = colorIcono,
            start = Offset(size.width * 0.64f, size.height * 0.64f),
            end = Offset(size.width * 0.91f, size.height * 0.91f),
            strokeWidth = grosor,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun EstadoBuscando(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Buscando Pokémon…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EstadoSinCoincidencias(consulta: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "No hay coincidencias para ‘$consulta’.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EstadoErrorBusqueda(mensaje: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No se pudo completar la búsqueda", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(mensaje, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EncabezadoHome() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 12.dp)
    ) {
        Text(
            text = "PokeView",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Descubre y explora el mundo Pokémon",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TarjetaPokemon(
    pokemon: Pokemon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var imagenConError by remember(pokemon.imagenUrl) { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            if (pokemon.imagenUrl.isNullOrBlank() || imagenConError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Imagen no disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AsyncImage(
                    model = pokemon.imagenUrl,
                    contentDescription = "Imagen de ${pokemon.nombre}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    onError = { imagenConError = true }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = pokemon.nombre.replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EstadoCargaInicial(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Cargando Pokémon…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EstadoErrorHome(
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No se pudo cargar la lista", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(mensaje, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onReintentar) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun EstadoVacioHome(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "No hay Pokémon para mostrar.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AvisoErrorPaginacion(
    mensaje: String,
    onReintentar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = mensaje,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        Button(onClick = onReintentar) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun IndicadorCargaAdicional() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(
            text = "  Cargando más Pokémon…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VistaPreviaHome() {
    PokeViewTheme {
        HomeContent(
            estado = HomeUiState(
                pokemones = listOf(
                    Pokemon(
                        nombre = "bulbasaur",
                        id = 1,
                        imagenUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png"
                    ),
                    Pokemon(
                        nombre = "ivysaur",
                        id = 2,
                        imagenUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/2.png"
                    )
                ),
                estaCargandoInicial = false,
                hayMasResultados = true
            ),
            onCargarMas = {},
            onReintentar = {},
            onBusquedaCambiada = {},
            onAlternarFiltros = {},
            onSeleccionarTipo = {},
            onSeleccionarHabilidad = {},
            onAplicarFiltros = {},
            onLimpiarFiltros = {},
            onReintentarFiltros = {},
            onPokemonSeleccionado = {}
        )
    }
}

private const val UMBRAL_PAGINACION = 4

