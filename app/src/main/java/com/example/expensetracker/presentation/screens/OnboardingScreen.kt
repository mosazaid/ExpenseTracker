package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.presentation.theme.withTabularNums
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val titleRes: Int,
    val descRes: Int,
    val tagRes: Int,
    val icon: ImageVector,
    val iconColor: Color,
    val highlights: List<Int>,
    val proTipRes: Int
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val pages = listOf(
        // 1. Accounts & Safe Vaults
        OnboardingPageData(
            titleRes = R.string.onboarding_title_1,
            descRes = R.string.onboarding_desc_1,
            tagRes = R.string.onboarding_tag_1,
            icon = Icons.Outlined.AccountBalanceWallet,
            iconColor = MaterialTheme.colorScheme.primary,
            highlights = listOf(
                R.string.onboarding_h1_1,
                R.string.onboarding_h1_2,
                R.string.onboarding_h1_3
            ),
            proTipRes = R.string.onboarding_tip_1
        ),
        // 2. Proactive Financial Insights & Pacing
        OnboardingPageData(
            titleRes = R.string.onboarding_title_2,
            descRes = R.string.onboarding_desc_2,
            tagRes = R.string.onboarding_tag_2,
            icon = Icons.Outlined.Insights,
            iconColor = MaterialTheme.colorScheme.secondary,
            highlights = listOf(
                R.string.onboarding_h2_1,
                R.string.onboarding_h2_2,
                R.string.onboarding_h2_3
            ),
            proTipRes = R.string.onboarding_tip_2
        ),
        // 3. Deep Comparisons & Analytics
        OnboardingPageData(
            titleRes = R.string.onboarding_title_4,
            descRes = R.string.onboarding_desc_4,
            tagRes = R.string.onboarding_tag_3,
            icon = Icons.Outlined.BarChart,
            iconColor = MaterialTheme.colorScheme.tertiary,
            highlights = listOf(
                R.string.onboarding_h3_1,
                R.string.onboarding_h3_2,
                R.string.onboarding_h3_3
            ),
            proTipRes = R.string.onboarding_tip_3
        ),
        // 4. Split Receipts & Loans
        OnboardingPageData(
            titleRes = R.string.onboarding_title_6,
            descRes = R.string.onboarding_desc_6,
            tagRes = R.string.onboarding_tag_4,
            icon = Icons.AutoMirrored.Outlined.CallSplit,
            iconColor = Color(0xFF0284C7),
            highlights = listOf(
                R.string.onboarding_h4_1,
                R.string.onboarding_h4_2,
                R.string.onboarding_h4_3
            ),
            proTipRes = R.string.onboarding_tip_4
        ),
        // 5. Bank SMS & Recurring Bills
        OnboardingPageData(
            titleRes = R.string.onboarding_title_7,
            descRes = R.string.onboarding_desc_7,
            tagRes = R.string.onboarding_tag_5,
            icon = Icons.Outlined.Autorenew,
            iconColor = Color(0xFF8B5CF6),
            highlights = listOf(
                R.string.onboarding_h5_1,
                R.string.onboarding_h5_2,
                R.string.onboarding_h5_3
            ),
            proTipRes = R.string.onboarding_tip_5
        ),
        // 6. Travel Mode & Multi-Currency
        OnboardingPageData(
            titleRes = R.string.onboarding_title_8,
            descRes = R.string.onboarding_desc_8,
            tagRes = R.string.onboarding_tag_6,
            icon = Icons.Default.FlightTakeoff,
            iconColor = Color(0xFF0D9488),
            highlights = listOf(
                R.string.onboarding_h6_1,
                R.string.onboarding_h6_2,
                R.string.onboarding_h6_3
            ),
            proTipRes = R.string.onboarding_tip_6
        ),
        // 7. Zakah & Virtual Sadaqah Jar
        OnboardingPageData(
            titleRes = R.string.onboarding_title_9,
            descRes = R.string.onboarding_desc_9,
            tagRes = R.string.onboarding_tag_7,
            icon = Icons.Outlined.VolunteerActivism,
            iconColor = Color(0xFF10B981),
            highlights = listOf(
                R.string.onboarding_h7_1,
                R.string.onboarding_h7_2,
                R.string.onboarding_h7_3
            ),
            proTipRes = R.string.onboarding_tip_7
        ),
        // 8. Glance Widgets & Biometric Privacy
        OnboardingPageData(
            titleRes = R.string.onboarding_title_3,
            descRes = R.string.onboarding_desc_3,
            tagRes = R.string.onboarding_tag_8,
            icon = Icons.Outlined.Security,
            iconColor = MaterialTheme.colorScheme.primary,
            highlights = listOf(
                R.string.onboarding_h8_1,
                R.string.onboarding_h8_2,
                R.string.onboarding_h8_3
            ),
            proTipRes = R.string.onboarding_tip_8
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Navigation Bar: Step Badge & Skip Button ────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = stringResource(
                            R.string.onboarding_step_format,
                            pagerState.currentPage + 1,
                            pages.size
                        ),
                        style = MaterialTheme.typography.labelMedium.withTabularNums(),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (!isLastPage) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFinish()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // ── Carousel Pager ──────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Hero Icon Badge
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(page.iconColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = page.iconColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Category Tag Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = page.iconColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = stringResource(page.tagRes).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = page.iconColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Headline Title
                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Description
                    Text(
                        text = stringResource(page.descRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )

                    // Feature Highlights Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            page.highlights.forEach { highlightRes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = page.iconColor.copy(alpha = 0.12f),
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = page.iconColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = stringResource(highlightRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Pro-Tip Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = page.iconColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${stringResource(R.string.onboarding_pro_tip_label)}:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = page.iconColor
                                )
                                Text(
                                    text = stringResource(page.proTipRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Indicator Dots ──────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 10.dp, bottom = 16.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isSelected) 22.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                    )
                }
            }

            // ── Bottom Action Row: Back Button & Next/Get Started ───────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.onboarding_back),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isLastPage) {
                            onFinish()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(if (pagerState.currentPage > 0) 1.5f else 1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (isLastPage) {
                            stringResource(R.string.onboarding_get_started)
                        } else {
                            stringResource(R.string.onboarding_next)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isLastPage) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
