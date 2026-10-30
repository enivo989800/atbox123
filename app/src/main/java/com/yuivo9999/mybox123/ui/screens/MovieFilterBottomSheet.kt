package com.yuivo9999.mybox123.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuivo9999.mybox123.data.movie.NativeMovieCategory
import com.yuivo9999.mybox123.ui.theme.AppTheme

private val DEFAULT_AREAS = listOf(
    "全部", "美国", "中国大陆", "中国香港", "中国台湾", "韩国", "日本", "英国", "法国", "德国", "印度", "泰国", "欧美", "其他"
)

private val DEFAULT_GENRES = listOf(
    "全部", "科幻", "动作", "喜剧", "爱情", "悬疑", "恐怖", "犯罪", "战争", "冒险", "奇幻", "动画", "惊悚", "武侠", "古装", "历史", "纪录片"
)

private val DEFAULT_YEAR_SHORTCUTS = listOf(
    "全部", "2024", "2023", "2022", "2021", "2020", "2019", "2010年代", "2000年代", "90年代", "80年代"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovieFilterBottomSheet(
    currentCriteria: MovieFilterCriteria,
    availableCategories: List<NativeMovieCategory>,
    onDismiss: () -> Unit,
    onApply: (MovieFilterCriteria) -> Unit,
    onOpenCustomYearDialog: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempCategoryId by remember { mutableStateOf(currentCriteria.categoryId) }
    var tempArea by remember { mutableStateOf(currentCriteria.area) }
    var tempGenre by remember { mutableStateOf(currentCriteria.genre) }
    var tempYear by remember { mutableStateOf(currentCriteria.year) }

    val isCustomYear = tempYear != null && tempYear !in DEFAULT_YEAR_SHORTCUTS

    val activeCount = listOfNotNull(
        tempCategoryId?.takeIf { it != "all" },
        tempArea?.takeIf { it != "全部" },
        tempGenre?.takeIf { it != "全部" },
        tempYear?.takeIf { it != "全部" }
    ).size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colors.card,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppTheme.colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = AppTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "影视多维深度筛选",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable Filter Sections
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Classification (分类)
                FilterSection(
                    title = "影视分类",
                    subtitle = "按大类筛选目标资源"
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            label = "全部分类",
                            selected = tempCategoryId == null || tempCategoryId == "all",
                            onClick = { tempCategoryId = null }
                        )
                        val categoriesToShow = if (availableCategories.isNotEmpty()) {
                            availableCategories.filter { it.id != "all" }
                        } else {
                            listOf(
                                NativeMovieCategory(id = "movie", name = "电影"),
                                NativeMovieCategory(id = "series", name = "电视剧"),
                                NativeMovieCategory(id = "anime", name = "动漫"),
                                NativeMovieCategory(id = "variety", name = "综艺"),
                                NativeMovieCategory(id = "short_drama", name = "短剧")
                            )
                        }
                        categoriesToShow.forEach { cat ->
                            FilterChip(
                                label = cat.name,
                                selected = tempCategoryId == cat.id || tempCategoryId == cat.name,
                                onClick = { tempCategoryId = cat.id }
                            )
                        }
                    }
                }

                // 2. Region (地区)
                FilterSection(
                    title = "地区 / 产地",
                    subtitle = "涵盖欧美、华语、日韩及全球影视"
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DEFAULT_AREAS.forEach { area ->
                            val selected = if (area == "全部") {
                                tempArea.isNullOrBlank() || tempArea == "全部"
                            } else {
                                tempArea == area
                            }
                            FilterChip(
                                label = area,
                                selected = selected,
                                onClick = { tempArea = if (area == "全部") null else area }
                            )
                        }
                    }
                }

                // 3. Genre / Type (类型与题材)
                FilterSection(
                    title = "题材 / 风格",
                    subtitle = "科幻、动作、悬疑、爱情等剧情标签"
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DEFAULT_GENRES.forEach { genre ->
                            val selected = if (genre == "全部") {
                                tempGenre.isNullOrBlank() || tempGenre == "全部"
                            } else {
                                tempGenre == genre
                            }
                            FilterChip(
                                label = genre,
                                selected = selected,
                                onClick = { tempGenre = if (genre == "全部") null else genre }
                            )
                        }
                    }
                }

                // 4. Year (年份与历史经典)
                FilterSection(
                    title = "年代 / 上映年份",
                    subtitle = "精准年份直查服务端海量历史片库"
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DEFAULT_YEAR_SHORTCUTS.forEach { yearShortcut ->
                            val selected = if (yearShortcut == "全部") {
                                tempYear.isNullOrBlank() || tempYear == "全部"
                            } else {
                                tempYear == yearShortcut
                            }
                            FilterChip(
                                label = yearShortcut,
                                selected = selected,
                                onClick = { tempYear = if (yearShortcut == "全部") null else yearShortcut }
                            )
                        }

                        // Custom Year Chip entry (e.g. 1999)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCustomYear) AppTheme.colors.accent else AppTheme.colors.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isCustomYear) AppTheme.colors.accent else AppTheme.colors.border
                            ),
                            modifier = Modifier.clickable {
                                onOpenCustomYearDialog()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    tint = if (isCustomYear) Color.White else AppTheme.colors.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isCustomYear) "${tempYear}年 (自定义)" else "自定义年份 (如1999)...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isCustomYear) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCustomYear) Color.White else AppTheme.colors.accent
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        tempCategoryId = null
                        tempArea = null
                        tempGenre = null
                        tempYear = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppTheme.colors.textSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text("重置全部")
                }

                Button(
                    onClick = {
                        onApply(
                            MovieFilterCriteria(
                                categoryId = tempCategoryId,
                                year = tempYear,
                                area = tempArea,
                                genre = tempGenre
                            )
                        )
                    },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.accent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (activeCount > 0) "确定筛选 (${activeCount}项)" else "确定筛选",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.colors.accent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted
                )
            }
        }
        content()
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) AppTheme.colors.accent else AppTheme.colors.surface,
        border = BorderStroke(
            1.dp,
            if (selected) AppTheme.colors.accent else AppTheme.colors.border
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else AppTheme.colors.textSecondary
            )
        }
    }
}
