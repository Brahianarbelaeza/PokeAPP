package com.example.pokeview.features.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.pokeview.core.theme.PokeViewTheme
import com.example.pokeview.domain.model.EstadisticaPokemon
import com.example.pokeview.domain.model.Pokemon

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val mensajeError = estado.mensajeError
    val pokemon = estado.pokemon

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onVolver) {
                Text("‹  Volver")
            }
            Text(
                text = "Detalle Pokémon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        when {
            estado.estaCargando && pokemon == null -> {
                EstadoCargaDetalle(modifier = Modifier.weight(1f))
            }

            mensajeError != null && pokemon == null -> {
                EstadoErrorDetalle(
                    mensaje = mensajeError,
                    onReintentar = viewModel::cargarDetalle,
                    modifier = Modifier.weight(1f)
                )
            }

            pokemon != null -> {
                ContenidoDetalle(
                    pokemon = pokemon,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ContenidoDetalle(
    pokemon: Pokemon,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ImagenPokemon(pokemon)
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = pokemon.nombre.replaceFirstChar { it.titlecase() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = pokemon.id?.let { "#${it.toString().padStart(3, '0')}" } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pokemon.tipos.forEach { tipo ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = tipo.nombre.replaceFirstChar { it.titlecase() },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        FilaMedidas(
            altura = pokemon.altura?.let { "${it / 10f} m" } ?: "—",
            peso = pokemon.peso?.let { "${it / 10f} kg" } ?: "—"
        )

        Spacer(modifier = Modifier.height(24.dp))
        SeccionDetalle(titulo = "Habilidades") {
            if (pokemon.habilidades.isEmpty()) {
                Text("No hay habilidades disponibles.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pokemon.habilidades.forEach { habilidad ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                habilidad.nombre.replace('-', ' ')
                                    .replaceFirstChar { it.titlecase() }
                            )
                            if (habilidad.esOculta) {
                                Text(
                                    "Oculta",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SeccionDetalle(titulo = "Estadísticas base") {
            val etiquetas = listOf(
                "hp" to "HP",
                "attack" to "Ataque",
                "defense" to "Defensa",
                "special-attack" to "Ataque especial",
                "special-defense" to "Defensa especial",
                "speed" to "Velocidad"
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                etiquetas.forEach { (nombre, etiqueta) ->
                    pokemon.estadisticas
                        .firstOrNull { it.nombre == nombre }
                        ?.let { estadistica ->
                            FilaEstadistica(etiqueta, estadistica)
                        }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ImagenPokemon(pokemon: Pokemon) {
    var imagenConError by remember(pokemon.imagenUrl) { mutableStateOf(false) }

    if (pokemon.imagenUrl.isNullOrBlank() || imagenConError) {
        Box(
            modifier = Modifier.size(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Imagen no disponible",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        AsyncImage(
            model = pokemon.imagenUrl,
            contentDescription = "Imagen de ${pokemon.nombre}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(250.dp),
            onError = { imagenConError = true }
        )
    }
}

@Composable
private fun FilaMedidas(altura: String, peso: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TarjetaMedida(
            titulo = "Altura",
            valor = altura,
            modifier = Modifier.weight(1f)
        )
        TarjetaMedida(
            titulo = "Peso",
            valor = peso,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TarjetaMedida(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(titulo, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SeccionDetalle(
    titulo: String,
    contenido: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        contenido()
    }
}

@Composable
private fun FilaEstadistica(
    etiqueta: String,
    estadistica: EstadisticaPokemon
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
            Text(
                estadistica.valorBase.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { (estadistica.valorBase / MAX_ESTADISTICA).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EstadoCargaDetalle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Cargando detalle…")
        }
    }
}

@Composable
private fun EstadoErrorDetalle(
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No se pudo cargar el Pokémon", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(mensaje, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onReintentar) {
            Text("Reintentar")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VistaPreviaDetalle() {
    PokeViewTheme {
        ContenidoDetalle(
            pokemon = Pokemon(
                nombre = "pikachu",
                id = 25,
                altura = 4,
                peso = 60,
                imagenUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png"
            )
        )
    }
}

private const val MAX_ESTADISTICA = 255f

