package com.example.pokeview.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pokeview.features.detail.ArgumentosDetalle
import com.example.pokeview.features.detail.DetailScreen
import com.example.pokeview.features.detail.DetailViewModel
import com.example.pokeview.features.home.HomeScreen
import com.example.pokeview.features.home.HomeViewModel

@Composable
fun PokeViewNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = RutasPokemon.HOME,
        modifier = modifier
    ) {
        composable(RutasPokemon.HOME) { entradaHome ->
            val viewModelHome: HomeViewModel = hiltViewModel(entradaHome)
            HomeScreen(
                viewModel = viewModelHome,
                onPokemonSeleccionado = { pokemon ->
                    navController.navigate(RutasPokemon.detalle(pokemon.nombre))
                }
            )
        }

        composable(
            route = RutasPokemon.DETALLE,
            arguments = listOf(
                navArgument(ArgumentosDetalle.NOMBRE_POKEMON) {
                    type = NavType.StringType
                }
            )
        ) { entradaDetalle ->
            val viewModelDetalle: DetailViewModel = hiltViewModel(entradaDetalle)
            DetailScreen(
                viewModel = viewModelDetalle,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}

