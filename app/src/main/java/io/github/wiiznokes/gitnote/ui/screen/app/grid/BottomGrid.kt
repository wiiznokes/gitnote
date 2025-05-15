package io.github.wiiznokes.gitnote.ui.screen.app.grid

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel
import kotlin.math.roundToInt

private const val TAG = "GridViewBottom"


@Composable
fun FloatingActionButtons(
    vm: GridViewModel,
    offset: Float,
    onEditClick: (Note, EditType) -> Unit,
    searchFocusRequester: FocusRequester,
    expanded: MutableState<Boolean>,
) {

    val keyboardController = LocalSoftwareKeyboardController.current

    FABs(
        expanded = expanded,
        modifier = Modifier
            .offset { IntOffset(x = 0, y = -offset.roundToInt()) },
        items = listOf(
            FABItem(
                icon = Icons.Default.Add,
                label = "create",
                type = FABItemType.CREATE,
                containerColor = MaterialTheme.colorScheme.tertiary
            ),
            FABItem(
                icon = Icons.Default.Search,
                label = "search",
                type = FABItemType.SEARCH,
            ),
        ),
        onItemClicked = {

            when (it.type) {
                FABItemType.CREATE -> {
                    onEditClick(
                        vm.defaultNewNote(),
                        EditType.Create
                    )
                }

                FABItemType.SEARCH -> {
                    searchFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
        },
    )
}

/**
 * FloatingActionButtons
 */
@Composable
private fun FABs(
    modifier: Modifier = Modifier,
    items: List<FABItem>,
    onItemClicked: (fabItem: FABItem) -> Unit,
    expanded: MutableState<Boolean>,
) {

    val rotation by animateFloatAsState(
        if (expanded.value) {
            45f
        } else {
            0f
        }, label = "FabRotate"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ListFab(
            expanded = expanded
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                items(items) { item ->

                    FloatingActionButton(
                        modifier = Modifier.size(50.dp),
                        onClick = {
                            expanded.value = false
                            onItemClicked(item)
                        },
                        containerColor = item.containerColor
                            ?: FloatingActionButtonDefaults.containerColor,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                        )
                    }
                }
                item { }
                item { } // Empty items to provide spacing at the end of the list
            }
        }

        FloatingActionButton(
            modifier = modifier,
            onClick = {
                expanded.value = !expanded.value
            },
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

private data class FABItem(
    val icon: ImageVector,
    val label: String,
    val type: FABItemType,
    val containerColor: Color? = null,
)

private enum class FABItemType {
    CREATE,
    SEARCH
}

@Composable
private fun ListFab(
    expanded: MutableState<Boolean>,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded.value

    AnimatedVisibility(
        visibleState = expandedState,
        content = content,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    )
}

private object FABPopupPositionProvider
    : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {

        Log.d(TAG, "anchorBounds = $anchorBounds")
        Log.d(TAG, "windowSize = $windowSize")
        Log.d(TAG, "popupContentSize = $popupContentSize")

        val rightOffset = anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2
        val topOffset = anchorBounds.top - popupContentSize.height
        val offset = IntOffset(rightOffset, topOffset)

        Log.d(TAG, "offset = $offset")
        return offset

    }
}

