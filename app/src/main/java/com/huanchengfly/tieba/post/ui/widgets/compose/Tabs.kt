package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.TabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerTabIndicator(
    pagerState: PagerState,
    tabPositions: List<TabPosition>
) {
    if (tabPositions.isNotEmpty()) {
        val tabWidth = 16.dp
        val currentPage = minOf(tabPositions.lastIndex, pagerState.currentPage)
        val currentTab = tabPositions[currentPage]
        val prevTab = tabPositions.getOrNull(currentPage - 1)
        val nextTab = tabPositions.getOrNull(currentPage + 1)
        val fraction = pagerState.currentPageOffsetFraction
        val currentTabLeft = currentTab.left + (currentTab.width / 2 - tabWidth / 2)
        val indicatorOffset = if (fraction > 0 && nextTab != null) {
            val nextTabLeft = nextTab.left + (nextTab.width / 2 - tabWidth / 2)
            lerp(currentTabLeft, nextTabLeft, fraction)
        } else if (fraction < 0 && prevTab != null) {
            val prevTabLeft = prevTab.left + (prevTab.width / 2 - tabWidth / 2)
            lerp(currentTabLeft, prevTabLeft, -fraction)
        } else {
            currentTabLeft
        }
        val animatedIndicatorOffset by animateDpAsState(targetValue = indicatorOffset)
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.BottomStart)
                .offset(x = animatedIndicatorOffset, y = (-8).dp)
                .width(16.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(100))
                .background(color = LocalContentColor.current)
        )
    }
}

@Composable
fun TabIndicator(
    selectedTabIndex: Int,
    tabPositions: List<TabPosition>
) {
    if (tabPositions.isNotEmpty()) {
        val tabIndicatorWidth = 16.dp
        val currentTab = tabPositions[selectedTabIndex]
        val currentTabLeft = currentTab.left + (currentTab.width / 2 - tabIndicatorWidth / 2)
        val animatedIndicatorOffset by animateDpAsState(targetValue = currentTabLeft)
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.BottomStart)
                .offset(x = animatedIndicatorOffset, y = (-8).dp)
                .width(tabIndicatorWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(100))
                .background(color = LocalContentColor.current)
        )
    }
}