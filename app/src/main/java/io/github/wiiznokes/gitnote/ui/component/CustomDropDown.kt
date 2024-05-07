package io.github.wiiznokes.gitnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


data class CustomDropDownModel(
    val text: String,
    val onClick: () -> Unit
)

@Composable
fun CustomDropDown(
    expanded: MutableState<Boolean>,
    shape: CornerBasedShape = MaterialTheme.shapes.extraSmall,
    options: List<CustomDropDownModel?>,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(15.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onDismissRequest: (() -> Unit)? = null
) {

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = shape)
    ) {

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = {
                expanded.value = false
                onDismissRequest?.invoke()
            },
            modifier = Modifier
                .background(containerColor)
                .clip(shape = shape),
        ) {

            options.filterNotNull().forEach { model ->
                DropdownMenuItem(
                    text = { Text(text = model.text) },
                    onClick = {
                        expanded.value = false
                        model.onClick()
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = contentColor
                    )
                )
            }
        }

    }
}

@Preview
@Composable
private fun DropDownPreview() {
    CustomDropDown(
        expanded = remember {
            mutableStateOf(true)
        },
        shape = RoundedCornerShape(100.dp),
        options = listOf(
            CustomDropDownModel(
                text = "Delete this folder",
                onClick = {
                }
            ),

            CustomDropDownModel(
                text = "Delete this folder",
                onClick = {
                }
            ),
            CustomDropDownModel(
                text = "Delete this folder",
                onClick = {
                }
            ),
        )
    )
}