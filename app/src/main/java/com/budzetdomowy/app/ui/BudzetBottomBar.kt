package com.budzetdomowy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.budzetdomowy.core.ui.R

data class BudzetBottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val BarHeight = 58.dp
private val FabSize = 56.dp
private val CutoutRadius = 34.dp
/** FAB drawn above the bar via offset — not counted in Scaffold padding. */
private val FabOverlap = 28.dp

/** Compact cream bottom bar with a center FAB cradle cutout. */
@Composable
fun BudzetBottomBar(
    tabs: List<BudzetBottomTab>,
    selectedRoute: String?,
    onTabSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    require(tabs.size == 4) { "BudzetBottomBar expects exactly 4 tabs" }

    val barColor = MaterialTheme.colorScheme.background
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val left = tabs.take(2)
    val right = tabs.drop(2)

    // Explicit height so Scaffold does not collapse the bar into the gesture inset only.
    // No full-rect background here — only the cutout shape + nav strip, so the notch is open.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight + navBottom)
    ) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(navBottom)
                .align(Alignment.BottomCenter)
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .align(Alignment.TopCenter)
                .clip(BottomBarCutoutShape(CutoutRadius))
                .background(barColor)
                .drawBehind {
                    val r = CutoutRadius.toPx()
                    val cx = size.width / 2f
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(cx - r, 0f)
                        arcTo(
                            rect = Rect(cx - r, -r, cx + r, r),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = -180f,
                            forceMoveTo = false
                        )
                        lineTo(size.width, 0f)
                    }
                    drawPath(path, color = outline, style = Stroke(width = 1.dp.toPx()))
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                left.forEach { tab ->
                    BottomTabItem(
                        tab = tab,
                        selected = selectedRoute == tab.route,
                        onClick = { onTabSelected(tab.route) }
                    )
                }
                Spacer(Modifier.width(CutoutRadius * 2))
                right.forEach { tab ->
                    BottomTabItem(
                        tab = tab,
                        selected = selectedRoute == tab.route,
                        onClick = { onTabSelected(tab.route) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -FabOverlap)
                .size(FabSize)
                .zIndex(1f),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_transaction)
            )
        }
    }
}

@Composable
private fun RowScope.BottomTabItem(
    tab: BudzetBottomTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 28.dp),
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Rectangle with a semicircular bite taken from the top center. */
private class BottomBarCutoutShape(
    private val cutoutRadius: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { cutoutRadius.toPx() }
        val cx = size.width / 2f
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(cx - r, 0f)
            arcTo(
                rect = Rect(cx - r, -r, cx + r, r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
