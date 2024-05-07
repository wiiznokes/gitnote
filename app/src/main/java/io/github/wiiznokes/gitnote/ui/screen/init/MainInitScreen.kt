package io.github.wiiznokes.gitnote.ui.screen.init

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.destination.InitDestination
import io.github.wiiznokes.gitnote.ui.destination.NewRepoSource
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel
import io.github.wiiznokes.gitnote.ui.viewmodel.viewModelFactory
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate

@Composable
fun MainScreen(
    navController: NavController<InitDestination>,
) {
    val vm = viewModel<InitViewModel>(
        factory = viewModelFactory { InitViewModel() }
    )


    AppPage(
        title = "Choose method",
        verticalArrangement = Arrangement.spacedBy(80.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val repoPath = vm.prefs.repoPath.getAsState()

        Button(
            onClick = {
                navController.navigate(
                    InitDestination.FileExplorer(
                        title = "Choose the repo folder",
                        path = repoPath.value,
                        newRepoSource = NewRepoSource.Create,
                    )
                )
            }
        ) {
            Text(
                text = "Create local repository"
            )
        }


        Button(
            onClick = {
                navController.navigate(
                    InitDestination.FileExplorer(
                        title = "Pick the repo folder",
                        path = repoPath.value,
                        newRepoSource = NewRepoSource.Open,
                    )
                )
            }
        ) {
            Text(
                text = "Open local repository"
            )
        }


        Button(
            onClick = {
                navController.navigate(
                    InitDestination.FileExplorer(
                        title = "Choose the folder where the repo will be cloned",
                        path = repoPath.value,
                        newRepoSource = NewRepoSource.Clone,
                    )
                )
            }
        ) {
            Text(
                text = "Clone remote repository"
            )
        }
    }
}