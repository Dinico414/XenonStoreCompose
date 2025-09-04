package com.xenon.store.ui.res

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xenon.store.R
import com.xenon.store.ui.layouts.QuicksandTitleVariable
import com.xenon.store.ui.values.DialogCornerRadius
import com.xenon.store.ui.values.DialogPadding
import com.xenon.store.ui.values.LargestPadding
import com.xenon.store.ui.values.MediumPadding

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun XenonDialog(
    onDismissRequest: () -> Unit,
    title: String,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnClickOutside = true, dismissOnBackPress = true
    ),
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(DialogCornerRadius),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 6.dp,

    dialogPadding: PaddingValues = PaddingValues(DialogPadding/2),
    dialogTitleRowPadding: PaddingValues = PaddingValues(
        start = DialogPadding, end = DialogPadding, top = 0.dp, bottom = LargestPadding
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = DialogPadding),
    buttonRowPadding: PaddingValues = PaddingValues(
        horizontal = MediumPadding, vertical = 0.dp
    ),

    actionButton1Text: String? = null,
    onActionButton1Click: (() -> Unit)? = null,
    actionButton1ContentColor: Color = MaterialTheme.colorScheme.primary,
    confirmButtonText: String? = null,
    onConfirmButtonClick: (() -> Unit)? = null,
    isConfirmButtonEnabled: Boolean = true,
    confirmContainerColor: Color = MaterialTheme.colorScheme.primary,
    confirmContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    actionButton2Text: String? = null,
    onActionButton2Click: (() -> Unit)? = null,
    actionButton2ContentColor: Color = MaterialTheme.colorScheme.primary,
    dismissIconButtonContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    dismissIconButtonContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,


    contentManagesScrolling: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest, properties = properties
    ) {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val maxDialogHeight = screenHeight * 0.9f

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = maxDialogHeight),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation
        ) {
            Column(
                modifier = Modifier.padding(
                    top = dialogPadding.calculateTopPadding(),
                    bottom = dialogPadding.calculateBottomPadding()
                )
            ) {
                var titleLineCount by remember { mutableIntStateOf(0) }
                val titleVerticalAlignment =
                    if (titleLineCount > 1) Alignment.Top else Alignment.CenterVertically

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dialogTitleRowPadding.calculateStartPadding(LayoutDirection.Ltr),
                            end = dialogTitleRowPadding.calculateEndPadding(LayoutDirection.Ltr),
                            top = dialogTitleRowPadding.calculateTopPadding(),
                            bottom = dialogTitleRowPadding.calculateBottomPadding()
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = titleVerticalAlignment
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = QuicksandTitleVariable
                        ),                        modifier = Modifier
                            .weight(1f)
                            .align(titleVerticalAlignment),
                        onTextLayout = { textLayoutResult: TextLayoutResult ->
                            titleLineCount = textLayoutResult.lineCount
                        })
                    FilledTonalIconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(32.dp)
                            .align(titleVerticalAlignment),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = dismissIconButtonContainerColor,
                            contentColor = dismissIconButtonContentColor
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Dismiss Dialog (Close)"
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(contentPadding)
                ) {
                    if (contentManagesScrolling) {
                        content()
                    } else {
                        val scrollState = rememberScrollState()
                        val topDividerAlpha by remember {
                            derivedStateOf { if (scrollState.value > 0) 1f else 0f }
                        }
                        val bottomDividerAlpha by remember {
                            derivedStateOf { if (scrollState.canScrollForward) 1f else 0f }
                        }

                        val maxHeightForScrollableInternalContent = screenHeight * 0.5f

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(topDividerAlpha)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxHeightForScrollableInternalContent)
                                .verticalScroll(scrollState), content = content
                        )

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(bottomDividerAlpha)
                        )
                    }
                }


                val isAction1Present = actionButton1Text != null && onActionButton1Click != null
                val isAction2Present = actionButton2Text != null && onActionButton2Click != null

                val action1Composable: (@Composable RowScope.() -> Unit)? = if (isAction1Present) {
                    {
                        TextButton(
                            onClick = onActionButton1Click,
                            modifier = if (isAction2Present && (confirmButtonText != null && onConfirmButtonClick != null))
                                Modifier.weight(1.2f)
                            else Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = actionButton1ContentColor)
                        ) {
                            Text(actionButton1Text)
                        }
                    }
                } else null

                val confirmComposable: (@Composable RowScope.() -> Unit)? =
                    if (confirmButtonText != null && onConfirmButtonClick != null) {
                        {
                            FilledTonalButton(
                                onClick = onConfirmButtonClick,
                                enabled = isConfirmButtonEnabled,
                                modifier = if (isAction1Present && isAction2Present) {
                                    Modifier.weight(1.2f)
                                } else {
                                    Modifier.weight(1.5f)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = confirmContainerColor,
                                    contentColor = confirmContentColor
                                )
                            ) {
                                Text(confirmButtonText)
                            }
                        }
                    } else null

                val action2Composable: (@Composable RowScope.() -> Unit)? = if (isAction2Present) {
                    {
                        TextButton(
                            onClick = onActionButton2Click,
                            modifier = if (isAction1Present && (confirmButtonText != null && onConfirmButtonClick != null))
                                Modifier.weight(1.2f)
                            else Modifier.weight(1.2f),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = actionButton2ContentColor)
                        ) {
                            Text(actionButton2Text)
                        }
                    }
                } else null

                val hasAction1 = action1Composable != null
                val hasConfirm = confirmComposable != null
                val hasAction2 = action2Composable != null

                val anyButtonPresent = hasAction1 || hasConfirm || hasAction2

                if (anyButtonPresent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LargestPadding)
                            .padding(buttonRowPadding),
                        horizontalArrangement = if (hasAction1 && hasConfirm && hasAction2) Arrangement.spacedBy(
                            8.dp
                        )
                        else Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        @Suppress("KotlinConstantConditions")
                        if (hasAction1 && hasConfirm && hasAction2) {
                            action1Composable.invoke(this)
                            confirmComposable.invoke(this)
                            action2Composable.invoke(this)
                        } else if (!hasAction1 && hasConfirm && !hasAction2) {
                            Spacer(Modifier.weight(1f))
                            confirmComposable.invoke(this)
                            Spacer(Modifier.weight(1f))
                        } else if (hasAction1 && hasConfirm && !hasAction2) {
                            Spacer(Modifier.weight(1f))
                            action1Composable.invoke(this)
                            confirmComposable.invoke(this)
                            Spacer(Modifier.weight(1f))
                        } else if (!hasAction1 && hasConfirm && hasAction2) {
                            Spacer(Modifier.weight(1f))
                            confirmComposable.invoke(this)
                            action2Composable.invoke(this)
                            Spacer(Modifier.weight(1f))
                        } else if (hasAction1 && !hasConfirm && !hasAction2) {
                            Spacer(Modifier.weight(1f))
                            action1Composable.invoke(this)
                            Spacer(Modifier.weight(1f))
                        } else if (!hasAction1 && !hasConfirm && hasAction2) {
                            Spacer(Modifier.weight(1f))
                            action2Composable.invoke(this)
                            Spacer(Modifier.weight(1f))
                        } else if (hasAction1 && !hasConfirm && hasAction2) {
                            action1Composable.invoke(this)
                            Spacer(Modifier.weight(0.5f))
                            action2Composable.invoke(this)
                        }
                    }
                }
            }
        }
    }
}
