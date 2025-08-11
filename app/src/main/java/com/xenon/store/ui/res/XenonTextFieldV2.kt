package com.xenon.store.ui.res

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XenonTextFieldV2(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(100.0f),
    colors: TextFieldColors = xenonTextFieldColors(),
    selectionColor: Color = MaterialTheme.colorScheme.primary
) {
    @Suppress("NAME_SHADOWING")
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    val isFocused = internalInteractionSource.collectIsFocusedAsState().value

    val resolvedTextColor: Color = colors.textColor(
        enabled = enabled,
        isError = isError,
        focused = isFocused
    )
    val finalTextColor: Color = textStyle.color.takeOrElse { resolvedTextColor }
    val mergedTextStyle = textStyle.merge(TextStyle(color = finalTextColor))

    val finalModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .defaultMinSize(
            minWidth = OutlinedTextFieldDefaults.MinWidth,
            minHeight = 48.dp
        )

    val currentSelectionColors: TextSelectionColors = remember(colors) {
        TextSelectionColors(
            handleColor = selectionColor,
            backgroundColor = selectionColor.copy(alpha = 0.4f)
        )
    }

    val cursorActualColor: Color = colors.cursorColor(isError)

    CompositionLocalProvider(LocalTextSelectionColors provides currentSelectionColors) {
        BasicTextField(
            value = value,
            modifier = finalModifier,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(cursorActualColor),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = internalInteractionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = @Composable { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = null,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = null,
                    suffix = null,
                    supportingText = null,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    interactionSource = internalInteractionSource,
                    colors = colors,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = internalInteractionSource,
                            colors = colors,
                            shape = shape,
                            focusedBorderThickness = OutlinedTextFieldDefaults.FocusedBorderThickness,
                            unfocusedBorderThickness = OutlinedTextFieldDefaults.UnfocusedBorderThickness
                        )
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun xenonTextFieldColors(): TextFieldColors {
    val colorScheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colorScheme.primary.copy(alpha = 0.25f),
        unfocusedContainerColor = colorScheme.primary.copy(alpha = 0.25f),
        disabledContainerColor = colorScheme.onSurface.copy(alpha = 0.05f),
        errorContainerColor = colorScheme.errorContainer.copy(alpha = 0.25f),
        focusedTextColor = colorScheme.onPrimaryContainer,
        unfocusedTextColor = colorScheme.onPrimaryContainer,
        disabledTextColor = colorScheme.onSurface.copy(alpha = 0.38f),
        errorTextColor = colorScheme.onErrorContainer,
        cursorColor = colorScheme.primary,
        errorCursorColor = colorScheme.error,
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = colorScheme.onSurface.copy(alpha = 0.12f),
        errorBorderColor = colorScheme.error,
        focusedPlaceholderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        unfocusedPlaceholderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        disabledPlaceholderColor = colorScheme.onSurface.copy(alpha = 0.38f),
        errorPlaceholderColor = colorScheme.onErrorContainer.copy(alpha = 0.7f),
        selectionColors = TextSelectionColors(
            handleColor = colorScheme.primary,
            backgroundColor = colorScheme.primary.copy(alpha = 0.4f)
        ),
        focusedLeadingIconColor = colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor = colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = colorScheme.onSurface.copy(alpha = 0.38f),
        errorLeadingIconColor = colorScheme.onErrorContainer,
        focusedTrailingIconColor = colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = colorScheme.onSurface.copy(alpha = 0.38f),
        errorTrailingIconColor = colorScheme.onErrorContainer
    )
}
