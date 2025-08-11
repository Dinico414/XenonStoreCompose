package com.xenon.store.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close // Import the icon for the unchecked state
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.xenon.store.ui.values.ExtraLargePadding
import com.xenon.store.ui.values.LargeCornerRadius
import com.xenon.store.ui.values.LargerPadding
import com.xenon.store.ui.values.LargestPadding

@Composable
fun SettingsSwitchTile(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: ((enabled: Boolean) -> Unit)?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    dividerColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    shape: Shape = RoundedCornerShape(LargeCornerRadius),
    horizontalPadding: Dp = LargestPadding,
    verticalPadding: Dp = ExtraLargePadding,
    switchColors: SwitchColors = SwitchDefaults.colors(),
    iconSpacing: Dp = ExtraLargePadding,
    tileSpacing: Dp = LargerPadding,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick, role = Role.Button)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            it()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = iconSpacing)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            }
        }
        if (onCheckedChange != null) {
            Spacer(modifier = Modifier.width(tileSpacing))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = switchColors,
                thumbContent = {
                    if (checked) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Checked",
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Not Checked",
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                            tint = MaterialTheme.colorScheme.surfaceDim
                        )
                    }
                }
            )
        }
    }
}
