package io.github.wiiznokes.gitnote.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.wiiznokes.gitnote.ui.component.AppPage
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer


@ExperimentalMaterial3Api
@Composable
fun LibrariesScreen(
    onBackClick: () -> Unit,
) {
    AppPage(
        title = "Libraries",
        onBackClick = onBackClick,
        disableVerticalScroll = true
    ) {
        LibrariesContainer(
            modifier = Modifier
                .fillMaxSize(),
        )

    }
}