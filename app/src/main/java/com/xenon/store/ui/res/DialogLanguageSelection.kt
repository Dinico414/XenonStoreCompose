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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.DialogProperties
import com.xenon.store.R
import com.xenon.store.ui.values.LargerPadding

data class LanguageOption(val displayName: String, val localeTag: String)

@Composable
fun DialogLanguageSelection(
    availableLanguages: List<LanguageOption>,
    currentLanguageTag: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val initialSelectedIndex = remember(currentLanguageTag, availableLanguages) {
        availableLanguages.indexOfFirst { it.localeTag == currentLanguageTag }.coerceAtLeast(0)
    }
    var selectedIndex by remember(initialSelectedIndex) { mutableIntStateOf(initialSelectedIndex) }

    XenonDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.language_dialog_title),
        confirmButtonText = stringResource(R.string.ok),
        onConfirmButtonClick = { onConfirm() },
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
                itemsIndexed(availableLanguages) { index, languageOption ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.0f))
                            .selectable(
                                selected = (index == selectedIndex),
                                onClick = {
                                    selectedIndex = index
                                    onLanguageSelected(languageOption.localeTag)
                                },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (index == selectedIndex),
                            onClick = {
                                selectedIndex = index
                                onLanguageSelected(languageOption.localeTag)
                            }
                        )
                        Text(
                            text = languageOption.displayName,
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
