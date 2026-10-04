package com.xenonware.store.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.BiggerCornerRadius
import com.xenon.mylibrary.values.ExtraLargePadding
import com.xenon.mylibrary.values.IconSizeExtraLarge
import com.xenon.mylibrary.values.IconSizeMedium
import com.xenon.mylibrary.values.LargestPadding
import com.xenonware.store.R

@Composable
fun SettingsGitHubTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    profilePictureUrl: String? = null,
    isSignedIn: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    mainContextFont: FontFamily = QuicksandTitleVariable,
    subContextFont: FontFamily = QuicksandTitleVariable,
    arrowColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shape: Shape = RoundedCornerShape(BiggerCornerRadius),
    avatarShape: Shape = CircleShape,
    horizontalPadding: Dp = LargestPadding,
    verticalPadding: Dp = ExtraLargePadding,
    iconContentDescription: String = "GitHub profile picture",
    placeholderIcon: Painter = painterResource(id = R.drawable.default_icon)
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
        Box(
            modifier = Modifier
                .size(IconSizeExtraLarge)
                .clip(avatarShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    shape = avatarShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSignedIn && !profilePictureUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profilePictureUrl,
                    contentDescription = iconContentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = placeholderIcon,
                    placeholder = placeholderIcon
                )
            } else {
                Image(
                    painter = placeholderIcon,
                    contentDescription = iconContentDescription,
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = mainContextFont,
                color = contentColor
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = subContextFont,
                    color = subtitleColor
                )
            }
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "Navigate",
            tint = arrowColor,
            modifier = Modifier.size(IconSizeMedium)
        )
    }
}
