package io.github.wiiznokes.gitnote.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage

@ExperimentalMaterial3Api
@Composable
fun LibrariesScreen(
    onBackClick: () -> Unit,
) {
    AppPage(
        title = stringResource(id = R.string.libraries),
        onBackClick = onBackClick,
        disableVerticalScroll = true
    ) {
        LibrariesContainer(
            showDescription = true,
            modifier = Modifier
                .fillMaxSize(),
        )

    }
}