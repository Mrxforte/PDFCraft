package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.PdfToolType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallpdfTopBar(
    title: String = "Smallpdf",
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!canNavigateBack) {
                    // Smallpdf Logo Icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SmallpdfPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "s",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SmallpdfNavy
                    )
                )
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("topbar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SmallpdfNavy
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = SmallpdfNavy,
            actionIconContentColor = SmallpdfNavy
        )
    )
}

@Composable
fun SmallpdfBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        val items = listOf(
            Triple("home", "Home", Icons.Filled.Home),
            Triple("tools", "Tools", Icons.Filled.GridView),
            Triple("history", "History", Icons.Filled.History),
            Triple("settings", "Settings", Icons.Filled.Settings)
        )

        items.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) SmallpdfPrimary else SmallpdfTextSecondary
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (selected) SmallpdfPrimary else SmallpdfTextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = SmallpdfInfoBg
                )
            )
        }
    }
}

@Composable
fun PdfToolCard(
    tool: PdfToolType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tool_card_${tool.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SmallpdfBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored icon container matching the Smallpdf screenshot
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tool.iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getToolIcon(tool),
                        contentDescription = tool.title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                tool.badge?.let { badgeText ->
                    Surface(
                        color = if (badgeText == "PRO") Color(0xFFFEF08A) else SmallpdfInfoBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (badgeText == "PRO") Color(0xFF854D0E) else SmallpdfPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SmallpdfTextPrimary,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SmallpdfTextSecondary,
                    lineHeight = 16.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun getToolIcon(tool: PdfToolType): ImageVector {
    return when (tool) {
        PdfToolType.MERGE -> Icons.Filled.CallMerge
        PdfToolType.COMPRESS -> Icons.Filled.Compress
        PdfToolType.WATERMARK_DELETE -> Icons.Filled.AutoFixHigh
        PdfToolType.OCR -> Icons.Filled.DocumentScanner
        PdfToolType.SPLIT -> Icons.Filled.CallSplit
        PdfToolType.ANNOTATE -> Icons.Filled.Draw
        PdfToolType.EDIT -> Icons.Filled.Edit
        PdfToolType.PDF_TO_WORD -> Icons.Filled.Description
        PdfToolType.PDF_TO_IMAGE -> Icons.Filled.Image
        PdfToolType.IMAGE_TO_PDF -> Icons.Filled.PictureAsPdf
        PdfToolType.WATERMARK_ADD -> Icons.Filled.BrandingWatermark
        PdfToolType.SIGN -> Icons.Filled.Gesture
        PdfToolType.SUMMARIZE -> Icons.Filled.ShortText
        PdfToolType.TRANSLATE -> Icons.Filled.Translate
        PdfToolType.REQUEST_SIGNATURES -> Icons.Filled.Send
        PdfToolType.PROTECT -> Icons.Filled.Lock
    }
}

@Composable
fun UpgradeBannerCard(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upgrade_banner_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SmallpdfPromoBg),
        border = BorderStroke(1.dp, Color(0xFFD8B4FE))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⭐ Smallpdf Pro",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF581C87)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Unlimited conversions, strong compression & high-speed OCR scanner.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B21A8)
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onUpgradeClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmallpdfPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Try Pro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
