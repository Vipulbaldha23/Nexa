package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderViolet
import com.example.ui.theme.GlassBorderWhite
import com.example.ui.theme.NexaCyan
import com.example.ui.theme.NexaViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Core Liquid Glass Modifier for modern futuristic glassmorphism
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0x18172338),
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp,
    hasGlow: Boolean = false,
    glowColor: Color = NexaCyan
): Modifier = this
    .then(
        if (hasGlow) {
            Modifier.drawBehind {
                drawCircle(
                    color = glowColor.copy(alpha = 0.15f),
                    radius = size.maxDimension * 0.6f,
                    center = Offset(size.width * 0.5f, size.height * 0.5f)
                )
            }
        } else Modifier
    )
    .clip(shape)
    .background(
        Brush.linearGradient(
            colors = listOf(
                Color(0x28FFFFFF),
                backgroundColor,
                Color(0x0C0B1220)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    )
    .border(
        width = borderWidth,
        brush = borderBrush ?: if (borderColor != null) {
            SolidColor(borderColor)
        } else {
            Brush.linearGradient(
                colors = listOf(
                    GlassBorderCyan,
                    Color(0x26FFFFFF),
                    GlassBorderViolet
                )
            )
        },
        shape = shape
    )

/**
 * Liquid Glass Card with smooth touch feedback and subtle shine
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    hasGlow: Boolean = false,
    glowColor: Color = NexaCyan,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = NexaCyan.copy(alpha = 0.3f)),
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                hasGlow = hasGlow,
                glowColor = glowColor
            )
            .then(clickableModifier)
    ) {
        content()
    }
}

/**
 * Futuristic Liquid Glass Button with neon glow
 */
@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = NexaCyan,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .liquidGlass(
                shape = RoundedCornerShape(14.dp),
                backgroundColor = accentColor.copy(alpha = 0.18f),
                borderColor = accentColor.copy(alpha = 0.5f),
                hasGlow = true,
                glowColor = accentColor
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Liquid Glass Icon Button
 */
@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    glowColor: Color = NexaCyan,
    hasGlow: Boolean = false,
    size: Dp = 44.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .liquidGlass(
                shape = CircleShape,
                backgroundColor = Color(0x22131F33),
                borderColor = if (hasGlow) glowColor.copy(alpha = 0.6f) else GlassBorderWhite,
                hasGlow = hasGlow,
                glowColor = glowColor
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = glowColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

/**
 * Liquid Glass Search Bar
 */
@Composable
fun LiquidGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search videos, movies, clips...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .liquidGlass(
                shape = RoundedCornerShape(25.dp),
                backgroundColor = Color(0x1F121C2F),
                borderColor = GlassBorderCyan.copy(alpha = 0.35f)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = NexaCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(NexaCyan),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Floating Liquid Glass Navigation Bar
 */
data class NavigationTab(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@Composable
fun LiquidGlassFloatingNavigationBar(
    tabs: List<NavigationTab>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(32.dp),
                    backgroundColor = Color(0x3B0D1626),
                    borderColor = GlassBorderCyan.copy(alpha = 0.4f),
                    borderWidth = 1.2.dp,
                    hasGlow = true,
                    glowColor = NexaCyan.copy(alpha = 0.2f)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab.id == selectedTabId
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.55f,
                        animationSpec = tween(250, easing = FastOutSlowInEasing),
                        label = "tab_alpha"
                    )
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.92f,
                        animationSpec = tween(250),
                        label = "tab_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.liquidGlass(
                                        shape = RoundedCornerShape(26.dp),
                                        backgroundColor = NexaCyan.copy(alpha = 0.18f),
                                        borderColor = NexaCyan.copy(alpha = 0.5f),
                                        borderWidth = 1.dp
                                    )
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, color = NexaCyan),
                                onClick = { onTabSelected(tab.id) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) NexaCyan else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
