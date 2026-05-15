package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.ui.AppLanguage
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.categoryTitle
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.KKTextField
import com.kaushalyakarnataka.app.ui.components.ScreenLoading
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors

private val HOME_CATEGORIES = listOf(
    "All", "Electrician", "Plumber", "Carpenter", "Painter", "Cleaner", "Gardener", "Other",
)

@Composable
fun HomeScreen(
    profile: UserProfile?,
    strings: Strings,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    repo: KaushalyaRepository,
    onOpenWorker: (String) -> Unit,
) {
    var workers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tick) {
        loading = true
        workers = runCatching { repo.fetchWorkers() }.getOrElse { emptyList() }
        loading = false
    }

    GalaxyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(48.dp))
            
            HeaderSection(profile, strings, language, onToggleLanguage, onRefresh = { tick++ })

            Spacer(Modifier.height(24.dp))

            KKTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = strings.searchPlaceholder,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
            )

            Spacer(Modifier.height(16.dp))

            CategorySelector(category, strings, onCategorySelected = { category = it })

            Spacer(Modifier.height(24.dp))

            val filtered = workers.filter { w ->
                val q = search.lowercase()
                val matchSearch = w.name.lowercase().contains(q) ||
                        (w.category ?: "").lowercase().contains(q)
                val matchCat = category == null || category == "All" || w.category == category
                matchSearch && matchCat
            }

            if (loading) {
                ScreenLoading(label = strings.loading)
            } else if (filtered.isEmpty()) {
                EmptyState(strings.noWorkers)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.userId }) { worker ->
                        WorkerRowCard(strings, worker, onOpenWorker)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    profile: UserProfile?,
    strings: Strings,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            val first = profile?.name?.split(" ")?.firstOrNull().orEmpty()
            Text(
                text = "${strings.hello}, $first",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleLanguage) {
                Icon(
                    Icons.Outlined.Language,
                    contentDescription = if (language == AppLanguage.EN) "ಕನ್ನಡ" else "English",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: String?,
    strings: Strings,
    onCategorySelected: (String?) -> Unit
) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HOME_CATEGORIES.forEach { cat ->
            val isSelected = (selectedCategory == null && cat == "All") || selectedCategory == cat
            FilterChip(
                selected = isSelected,
                onClick = {
                    onCategorySelected(if (cat == "All") null else cat)
                },
                label = { Text(strings.categories.label(cat), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WorkerRowCard(strings: Strings, worker: UserProfile, onOpenWorker: (String) -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenWorker(worker.userId) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = worker.profileImage,
                contentDescription = worker.name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = worker.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Star,
                            contentDescription = null,
                            tint = KaushalyaColors.Amber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (worker.rating > 0) String.format("%.1f", worker.rating) else strings.newLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = strings.categoryTitle(worker.category ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = worker.location.ifBlank { strings.locationUnknown },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
