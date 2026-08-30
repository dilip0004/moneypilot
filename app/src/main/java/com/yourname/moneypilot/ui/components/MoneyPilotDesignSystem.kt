package com.yourname.moneypilot.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import coil.compose.AsyncImage
import com.yourname.moneypilot.R
import com.yourname.moneypilot.ui.navigation.Screen
import com.yourname.moneypilot.ui.theme.LocalFinanceColors

/**
 * MoneyPilot Design System: Reusable Premium Components
 * PHASE 3: Adaptive Glass visual identity overhaul.
 */

/**
 * CompositionLocal to track if we are rendering over an atmospheric background
 */
val LocalIsAtmosphericBackground = staticCompositionLocalOf { false }

/**
 * Glass Tints and Opacities for different hierarchy levels
 */
object GlassLevel {
    val Low = 0.15f     // Level 1: Date headers, subtle separation
    val Medium = 0.25f  // Level 2: Top controls, tabs
    val High = 0.4f     // Level 3: Transaction rows, main content cards
    val Critical = 0.7f // Level 4: Bottom navigation, overlays
}

@Composable
fun MoneyPilotBackground(
    backgroundType: String = "DEFAULT", // DEFAULT, NONE, CUSTOM
    customUri: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isAtmospheric = backgroundType != "NONE"
    
    CompositionLocalProvider(LocalIsAtmosphericBackground provides isAtmospheric) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
            if (isAtmospheric) {
                if (backgroundType == "CUSTOM" && !customUri.isNullOrBlank()) {
                    val uri = remember(customUri) { Uri.parse(customUri) }
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.65f,
                        error = painterResource(id = R.drawable.app_background_default),
                        fallback = painterResource(id = R.drawable.app_background_default)
                    )
                } else {
                    // Background Image
                    Image(
                        painter = painterResource(id = R.drawable.app_background_default),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.65f
                    )
                }
                
                // Primary Dark Scrim (60% darkening)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                )

                // Additional gradient scrim for better edge visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            } else {
                // Standard background when NONE is selected
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
            
            content()
        }
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    opacity: Float = GlassLevel.Medium,
    blur: Dp = 0.dp, 
    borderAlpha: Float = 0.15f,
    tint: Color = Color.Black, // Dark glass is essential for financial text readability
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.then(
            if (blur > 0.dp) Modifier.blur(blur) else Modifier
        ),
        shape = shape,
        color = tint.copy(alpha = opacity),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            Color.White.copy(alpha = borderAlpha)
        )
    ) {
        Column(content = content)
    }
}

@Composable
fun MoneyPilotSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    color: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Adaptive Glass evolution of the existing surface
    GlassSurface(
        modifier = modifier,
        shape = shape,
        opacity = GlassLevel.High,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.statusBarsPadding(),
        color = Color.Black.copy(alpha = GlassLevel.Medium),
        contentColor = Color.White
    ) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
    }
}

@Composable
fun FinancialSummarySurface(
    title: String,
    primaryValue: String,
    secondaryInfo: @Composable RowScope.() -> Unit = {},
    progress: Float? = null,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        opacity = GlassLevel.High,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = Color.White
            )
            
            if (progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = progressColor,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = secondaryInfo
            )
        }
    }
}

@Composable
fun MoneyPilotListItem(
    icon: String,
    title: String,
    subtitle: String? = null,
    trailingContent: @Composable ColumnScope.() -> Unit,
    statusColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        opacity = GlassLevel.High
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = statusColor.copy(alpha = 0.25f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = icon, fontSize = 20.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                trailingContent()
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
        )
        action?.invoke()
    }
}

@Composable
fun SummaryStat(label: String, value: String, color: Color = Color.White) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = color,
            fontSize = 13.sp
        )
    }
}

@Composable
fun CompactMetricRow(
    metrics: List<Triple<String, String, Color>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        metrics.forEachIndexed { index, metric ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = metric.first,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = metric.second,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = metric.third
                )
            }
            if (index < metrics.size - 1) {
                VerticalDivider(
                    modifier = Modifier.height(32.dp).padding(horizontal = 8.dp),
                    color = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun MoneyPilotFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String? = null,
    expanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary
    
    if (label != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            expanded = expanded,
            icon = { Icon(icon, contentDescription = null) },
            text = { Text(label, fontWeight = FontWeight.Bold) },
            containerColor = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = CircleShape,
            modifier = modifier
        ) {
            Icon(icon, contentDescription = null)
        }
    }
}

/**
 * Reusable Floating Glass Bottom Navigation
 */
@Composable
fun MoneyPilotBottomNavigation(
    navItems: List<Screen>,
    currentDestination: NavDestination?,
    onItemSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = navItems.indexOfFirst { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        GlassSurface(
            opacity = GlassLevel.Critical,
            shape = RoundedCornerShape(32.dp),
            borderAlpha = 0.2f,
            tint = Color.Black
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                val itemWidth = maxWidth / navItems.size
                
                // Animated indicator capsule
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "indicator_offset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(6.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(28.dp)
                        )
                        .border(
                            0.5.dp, 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), 
                            RoundedCornerShape(28.dp)
                        )
                )

                // Navigation items
                Row(modifier = Modifier.fillMaxSize()) {
                    navItems.forEach { screen ->
                        val isSelected = navItems.indexOf(screen) == selectedIndex
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onItemSelected(screen) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1.0f,
                                animationSpec = tween(200),
                                label = "icon_scale"
                            )
                            
                            val contentAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.5f,
                                animationSpec = tween(200),
                                label = "content_alpha"
                            )

                            Icon(
                                imageVector = screen.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                                    .alpha(contentAlpha)
                            )
                            
                            Spacer(Modifier.height(4.dp))
                            
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.alpha(contentAlpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsGroupSurface(content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        opacity = GlassLevel.High,
        shape = RoundedCornerShape(16.dp),
        content = content
    )
}

@Composable
fun SettingsItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.1f)
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassSurface(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(28.dp),
            opacity = GlassLevel.Medium
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}
