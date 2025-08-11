package com.xenon.store.ui.res

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.DialogProperties
import com.xenon.store.R
import com.xenon.store.ui.values.LargerPadding
import com.xenon.store.viewmodel.ThemeSetting

@Composable
fun DialogThemeSelection(
    themeOptions: Array<ThemeSetting>,
    currentThemeIndex: Int,
    onThemeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    XenonDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.theme_dialog_title),
        confirmButtonText = stringResource(R.string.ok),
        onConfirmButtonClick = onConfirm,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        contentManagesScrolling = true,
    ) {
        val listState: LazyListState = rememberLazyListState()

        val showTopDivider by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }

        val showBottomDivider by remember {
            derivedStateOf {
                listState.canScrollForward
            }
        }

        val isScrollable by remember {
            derivedStateOf {
                listState.layoutInfo.visibleItemsInfo.isNotEmpty() &&
                        (listState.canScrollForward || listState.canScrollBackward)
            }
        }

        Box {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .selectableGroup()
                    .fillMaxWidth()
            ) {
                itemsIndexed(themeOptions) { index, theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.0f))
                            .selectable(
                                selected = (index == currentThemeIndex),
                                onClick = { onThemeSelected(index) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (index == currentThemeIndex),
                            onClick = { onThemeSelected(index) }
                        )
                        Text(
                            text = theme.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = LargerPadding)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .alpha(if (showTopDivider && isScrollable) 1f else 0f)
            )

            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .alpha(if (showBottomDivider && isScrollable) 1f else 0f)
            )
        }
    }
}
