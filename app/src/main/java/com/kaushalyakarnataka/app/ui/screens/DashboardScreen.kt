package com.kaushalyakarnataka.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.JobRequest
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.GradientPrimaryButton
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    profile: UserProfile,
    strings: Strings,
    repo: KaushalyaRepository,
    onOpenRequests: () -> Unit,
    onOpenChat: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val jobs by repo.observeAcceptedJobs(profile.userId).collectAsStateWithLifecycle(emptyList())
    val customers = remember { mutableStateMapOf<String, UserProfile>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(jobs) {
        jobs.forEach { job ->
            if (!customers.containsKey(job.customerId)) {
                val u = repo.fetchUser(job.customerId)
                if (u != null) customers[job.customerId] = u
            }
        }
    }

    GalaxyBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = strings.dashboard,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.activeJobs,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = strings.jobsDone,
                        value = "${profile.jobsCompleted}"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = strings.myRating,
                        value = if (profile.rating > 0) String.format("%.1f", profile.rating) else strings.newLabel
                    )
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenRequests
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = strings.jobRequests,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = strings.checkNewRequests,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp).padding(start = 8.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "${strings.activeJobs} (${jobs.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (jobs.isEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.noActiveJobs,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.newJobsWillAppear,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                items(jobs, key = { it.requestId }) { job ->
                    ActiveJobCard(
                        job = job,
                        customer = customers[job.customerId],
                        strings = strings,
                        onCall = { phone ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }
                        },
                        onChat = {
                            scope.launch {
                                val id = repo.findChatIdBetween(profile.userId, job.customerId)
                                if (id != null) onOpenChat(id) else onMessage(strings.chatNotFound)
                            }
                        },
                        onComplete = {
                            scope.launch {
                                // Using a safer launch pattern to avoid composition errors
                                try {
                                    repo.completeJob(job.requestId, profile.userId)
                                    onMessage(strings.jobDone)
                                } catch (e: Exception) {
                                    if (e !is kotlinx.coroutines.CancellationException) {
                                        onMessage(strings.tryAgain)
                                    }
                                }
                            }
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActiveJobCard(
    job: JobRequest,
    customer: UserProfile?,
    strings: Strings,
    onCall: (String) -> Unit,
    onChat: () -> Unit,
    onComplete: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AsyncImage(
                    model = customer?.profileImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = customer?.name ?: strings.customer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = customer?.location?.ifBlank { strings.locationUnknown } ?: strings.locationUnknown,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val phone = customer?.phone.orEmpty()
                    if (phone.isNotBlank()) {
                        FilledTonalIconButton(
                            onClick = { onCall(phone) },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    FilledTonalIconButton(
                        onClick = onChat,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            GradientPrimaryButton(
                text = strings.markDone,
                onClick = onComplete
            )
        }
    }
}
