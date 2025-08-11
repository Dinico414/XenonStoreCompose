package com.xenon.store.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.store.ui.layouts.QuicksandTitleVariable

object XenonSnackbarDefault {
    val backgroundColor: Color @Composable get() = MaterialTheme.colorScheme.inverseSurface
    val contentColor: Color @Composable get() = MaterialTheme.colorScheme.inverseOnSurface
    val actionColor: Color @Composable get() = MaterialTheme.colorScheme.inversePrimary
    val shape: RoundedCornerShape = RoundedCornerShape(24.dp)
    val startPadding: Dp = 16.dp
    val endPadding: Dp = 8.dp
    val textStyle: TextStyle @Composable get() = TextStyle(
        fontFamily = QuicksandTitleVariable,
        fontWeight = FontWeight.Thin,
        fontSize = 14.sp
    )
    val actionTextStyle: TextStyle @Composable get() = TextStyle(
        fontFamily = QuicksandTitleVariable,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
}

@Composable
fun XenonSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    backgroundColor: Color = XenonSnackbarDefault.backgroundColor,
    contentColor: Color = XenonSnackbarDefault.contentColor,
    actionColor: Color = XenonSnackbarDefault.actionColor,
    shape: RoundedCornerShape = XenonSnackbarDefault.shape,
    contentTextStyle: TextStyle = XenonSnackbarDefault.textStyle,
    actionTextStyle: TextStyle = XenonSnackbarDefault.actionTextStyle,
    padding: Dp = XenonSnackbarDefault.startPadding,
    actionPadding: Dp = XenonSnackbarDefault.endPadding
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = snackbarData.visuals.message,
            style = contentTextStyle,
            color = contentColor,
            modifier = Modifier.weight(1f).padding(horizontal = padding)
        )
        snackbarData.visuals.actionLabel?.let { actionLabel ->
            TextButton(
                onClick = { snackbarData.performAction() },
                colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
                modifier = Modifier.padding(end = actionPadding)
            ) {
                Text(
                    text = actionLabel,
                    style = actionTextStyle
                )
            }
        }
    }
}
