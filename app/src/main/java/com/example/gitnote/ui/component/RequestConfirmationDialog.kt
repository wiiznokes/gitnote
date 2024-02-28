package com.example.gitnote.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gitnote.R
import com.example.gitnote.ui.theme.LocalSpaces

@Composable
fun RequestConfirmationDialog(
    expanded: MutableState<Boolean>,
    text: String,
    onConfirmation: () -> Unit,
) {

    BaseDialog(
        expanded = expanded
    ) {

        Card(
            modifier = Modifier
                .padding(20.dp)
        ) {
            Text(
                modifier = Modifier
                    .padding(20.dp),
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center
                )
            )
        }

        Spacer(modifier = Modifier.height(LocalSpaces.current.dialogSeparation))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Button(
                onClick = { expanded.value = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.no),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                SimpleIcon(
                    imageVector = Icons.Default.Close
                )
            }

            Button(
                onClick = {
                    expanded.value = false
                    onConfirmation()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.yes),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                SimpleIcon(
                    imageVector = Icons.Default.Done
                )
            }
        }

    }
}


@Preview
@Composable
private fun DialogPreview() {
    RequestConfirmationDialog(
        expanded = remember {
            mutableStateOf(true)
        },
        text = "Do you wanna have sex ? kvfeznfezlzelfnklze" +
                "geg g ggre gegljerngljerngljrnljrengljneglnerlgnelrnglerngenrgne lnglng lenrgln elgrne lgn lgknergner ngegn lekgner nengk enrgl e"
    ) {

    }
}