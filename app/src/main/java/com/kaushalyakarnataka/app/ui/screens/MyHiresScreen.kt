package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.*
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.categoryTitle
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyHiresScreen(
    profile: UserProfile,
    strings: Strings,
    repo: KaushalyaRepository,
    onOpenChat: (String) -> Unit,
    onMessage: (String) -> Unit,
    onSubmitReview: (String, String, Int, String, (Result<Unit>) -> Unit) -> Unit
) {
    val hires by repo.observeRequestsForCustomer(profile.userId).collectAsStateWithLifecycle(emptyList())
    val myReviews by repo.observeReviewsByCustomer(profile.userId).collectAsStateWithLifecycle(emptyList())
    val ratedIds = remember(myReviews) { myReviews.map { it.workerId }.toSet() }
    val workers = remember { mutableStateMapOf<String, UserProfile>() }
    
    var ratingWorkerId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    LaunchedEffect(hires) {
        hires.forEach { h ->
            if (!workers.containsKey(h.workerId)) {
                val u = repo.fetchUser(h.workerId)
                if (u != null) workers[h.workerId] = u
            }
        }
    }

    GalaxyBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(28.dp))
                Text(
                    text = strings.myHires,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Manage your hired professionals",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hires.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxHeight(0.7f), contentAlignment = Alignment.Center) {
                        GlassCard {
                            Text(
                                text = strings.noHiresYet,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = strings.findWorkersHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                items(hires, key = { it.requestId }) { hire ->
                    val worker = workers[hire.workerId]
                    HireCard(
                        hire = hire,
                        worker = worker,
                        strings = strings,
                        fmt = fmt,
                        onChat = {
                            scope.launch {
                                val id = repo.findChatIdBetween(profile.userId, hire.workerId)
                                if (id != null) onOpenChat(id) else onMessage(strings.chatNotFound)
                            }
                        },
                        onRate = { ratingWorkerId = hire.workerId },
                        showRate = hire.status == RequestStatus.completed && !ratedIds.contains(hire.workerId)
                    )
                }
            }
        }
    }

    if (ratingWorkerId != null) {
        RatingDialog(
            strings = strings,
            onDismiss = { ratingWorkerId = null },
            onSubmit = { stars, comment ->
                val wid = ratingWorkerId ?: return@RatingDialog
                onSubmitReview(wid, profile.userId, stars, comment) { res ->
                    res.onSuccess {
                        onMessage(strings.ratingSubmitted)
                        ratingWorkerId = null
                    }.onFailure {
                        onMessage(strings.ratingFailed)
                    }
                }
            }
        )
    }
}

@Composable
private fun HireCard(
    hire: JobRequest,
    worker: UserProfile?,
    strings: Strings,
    fmt: SimpleDateFormat,
    onChat: () -> Unit,
    onRate: () -> Unit,
    showRate: Boolean,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AsyncImage(
                    model = worker?.profileImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = worker?.name ?: strings.customer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = strings.categoryTitle(worker?.category ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                
                val statusColor = when (hire.status) {
                    RequestStatus.accepted -> MaterialTheme.colorScheme.primary
                    RequestStatus.completed -> Color(0xFF34D399)
                    RequestStatus.rejected -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                }
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = hire.status.name.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hire.createdAt?.toDate()?.let { fmt.format(it) } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hire.status == RequestStatus.accepted) {
                        FilledTonalIconButton(
                            onClick = onChat,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Chat, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showRate) {
                        Button(
                            onClick = onRate,
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.rateWorker, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingDialog(
    strings: Strings,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    var stars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.rateWorker, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { idx ->
                        IconButton(onClick = { stars = idx + 1 }) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (idx < stars) Color(0xFFFBBF24) else Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.reviewHint) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(stars, comment) }) {
                Text(strings.submitReview, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}
