package com.example.docx.presentation.navigator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.docx.presentation.file_display.HorizontalPdfViewer
import com.example.docx.presentation.file_display.ResourceType
import com.example.docx.presentation.file_display.rememberHorizontalVueReaderState
import com.example.docx.presentation.home.HomeScreen
import com.example.docx.presentation.home.HomeViewModel
import java.io.File

@Composable
fun Navigator(modifier: Modifier = Modifier){
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = Route.Home.route) {
        composable(Route.Home.route){
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    addInitializer(HomeViewModel::class){
                        HomeViewModel(context = context,)
                    }
                }
            )
            val state = viewModel.state
            val event = viewModel::onEvent
            HomeScreen(
                state = state.value,
                event = event,
                modifier = modifier,
                navigateToPdfReader = { file ->
                                      navigateToPdfReader(file = file, navController = navController)
                },
            )
        }
        composable(Route.PdfReader.route){
            navController.previousBackStackEntry?.savedStateHandle?.get<File>("file")?.let {
                val localPdf = rememberHorizontalVueReaderState(
                    resource = ResourceType.Local(file = it, )
                )
                HorizontalPdfViewer(horizontalReaderState = localPdf)
            }
        }
    }
}

sealed class Route(val route: String){
    data object Home : Route("home")
    data object PdfReader : Route("pdf_reader")
}

private fun navigateToPdfReader(file: File, navController: NavController){
    navController.currentBackStackEntry?.savedStateHandle?.set("file", file)
    navController.navigate(Route.PdfReader.route)
}