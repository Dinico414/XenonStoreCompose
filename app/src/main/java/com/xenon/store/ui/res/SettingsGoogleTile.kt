package com.xenon.store.ui.res

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xenon.store.R
import com.xenon.store.ui.layouts.QuicksandTitleVariable
import com.xenon.store.ui.res.GoogleProfilBorder
import com.xenon.store.ui.values.ExtraLargePadding
import com.xenon.store.ui.values.LargeCornerRadius
import com.xenon.store.ui.values.LargestPadding

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsGoogleTile(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    arrowColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shape: Shape = RoundedCornerShape(LargeCornerRadius),
    horizontalPadding: Dp = LargestPadding,
    verticalPadding: Dp = ExtraLargePadding
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() },
                role = Role.Button,
                enabled = onClick != null || onLongClick != null
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ExtraLargePadding)
    ) {
        Box (
            contentAlignment = Alignment.Center,
        ) {
            GoogleProfilBorder(
                modifier = Modifier.size(48.dp),
            )
            Image(
                painter = painterResource(id = R.mipmap.default_icon),
                contentDescription = stringResource(R.string.open_navigation_menu),
                modifier = Modifier.size(40.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = QuicksandTitleVariable
                ),
                color = contentColor
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = QuicksandTitleVariable
                    ),
                    color = subtitleColor
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Navigate",
            tint = arrowColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
