package io.github.wiiznokes.gitnote.ui.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.util.TypedValueCompat.pxToDp


private val TAG = "CustomDropDown"
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
    onDismissRequest: (() -> Unit)? = null,
    clickPosition: MutableState<Offset> = remember {
        mutableStateOf(Offset.Zero)
    }
) {
    val m = LocalContext.current.resources.displayMetrics
    val x = pxToDp(clickPosition.value.x, m).dp
    val y = pxToDp(clickPosition.value.y, m).dp
    val offset = DpOffset(x, y)

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
            offset = offset,
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