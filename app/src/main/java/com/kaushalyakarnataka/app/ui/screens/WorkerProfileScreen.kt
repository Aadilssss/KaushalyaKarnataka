package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.kaushalyakarnataka.app.data.*
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.categoryTitle
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.GradientPrimaryButton
import com.kaushalyakarnataka.app.ui.components.ScreenLoading
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileScreen(
    workerId: String,
    customer: UserProfile?,
    strings: Strings,
    repo: KaushalyaRepository,
    onBack: () -> Unit,
    onHired: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var worker by remember { mutableStateOf<UserProfile?>(null) }
    var reviews by remember { mutableStateOf<List<ReviewEntry>>(emptyList()) }
    var services by remember { mutableStateOf<List<ServiceItem>>(emptyList()) }
    var portfolio by remember { mutableStateOf<List<PortfolioItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var hiring by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(workerId) {
        loading = true
        worker = repo.fetchUser(workerId)
        reviews = runCatching { repo.fetchReviewsForWorker(workerId) }.getOrElse { emptyList() }
        services = runCatching { repo.fetchServices(workerId) }.getOrElse { emptyList() }
        portfolio = runCatching { repo.fetchPortfolio(workerId) }.getOrElse { emptyList() }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.workerProfile, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            val w = worker
            if (w != null && customer != null) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    GradientPrimaryButton(
                        text = if (hiring) strings.loading else strings.hire,
                        onClick = {
                            hiring = true
                            scope.launch {
                                val res = repo.hireWorker(customer.userId, workerId)
                                hiring = false
                                res.onSuccess {
                                    onMessage(strings.hireSent)
                                    onHired()
                                }.onFailure { e ->
                                    val msg = when (e.message) {
                                        "duplicate_request" -> strings.duplicateRequest
                                        else -> strings.hireFailed
                                    }
                                    onMessage(msg)
                                }
                            }
                        },
                        modifier = Modifier.padding(20.dp),
                        loading = hiring,
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        GalaxyBackground {
            if (loading) {
                ScreenLoading(label = strings.loading)
            } else if (worker == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.workerNotFound, style = MaterialTheme.typography.titleLarge)
                }
            } else {
                val w = worker!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // Profile Header
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = w.profileImage,
                            contentDescription = null,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = w.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.categoryTitle(w.category ?: ""),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Stats Row
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatItem(
                            modifier = Modifier.weight(1f),
                            label = strings.reviews,
                            value = if (w.rating > 0) String.format("%.1f", w.rating) else strings.newLabel
                        )
                        StatItem(
                            modifier = Modifier.weight(1f),
                            label = strings.jobsDone,
                            value = "${w.jobsCompleted}"
                        )
                        StatItem(
                            modifier = Modifier.weight(1f),
                            label = strings.location,
                            value = w.location.split(",").firstOrNull()?.trim().orEmpty()
                                .ifBlank { strings.locationUnknown }
                        )
                    }

                    // Bio
                    SectionHeader(strings.workerProfile)
                    GlassCard {
                        Text(
                            text = w.bio?.takeIf { it.isNotBlank() } ?: strings.bioFallback,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Services
                    if (services.isNotEmpty()) {
                        SectionHeader(strings.services)
                        services.forEach { s ->
                            GlassCard(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = s.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (s.description.isNotBlank()) {
                                            Text(
                                                text = s.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        text = "₹${s.price.toInt()}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Portfolio
                    SectionHeader(strings.portfolio)
                    if (portfolio.isEmpty()) {
                        Text(
                            text = strings.noPhotosYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        portfolio.chunked(2).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                row.forEach { item ->
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Reviews
                    if (reviews.isNotEmpty()) {
                        SectionHeader(strings.reviews)
                        reviews.forEach { r ->
                            GlassCard(Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    repeat(5) { idx ->
                                        Icon(
                                            imageVector = Icons.Outlined.Star,
                                            contentDescription = null,
                                            tint = if (idx < r.rating) Color(0xFFFBBF24) else Color.Gray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (r.comment.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = r.comment,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
private fun StatItem(modifier: Modifier, label: String, value: String) {
    GlassCard(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
