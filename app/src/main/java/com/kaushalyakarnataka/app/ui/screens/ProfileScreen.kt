package com.kaushalyakarnataka.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.PortfolioItem
import com.kaushalyakarnataka.app.data.ServiceItem
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.data.UserRole
import com.kaushalyakarnataka.app.ui.AppLanguage
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.KKTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    strings: Strings,
    language: AppLanguage,
    repo: KaushalyaRepository,
    onUpdateProfile: (String, Map<String, Any>, (Result<Unit>) -> Unit) -> Unit,
    onAddService: (String, String, Double, String, (Result<Unit>) -> Unit) -> Unit,
    onDeleteService: (String, (Result<Unit>) -> Unit) -> Unit,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val services by repo.observeServices(profile.userId).collectAsStateWithLifecycle(emptyList())
    val portfolio by repo.observePortfolio(profile.userId).collectAsStateWithLifecycle(emptyList())

    var editing by remember { mutableStateOf(false) }
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone) }
    var location by remember(profile.location) { mutableStateOf(profile.location) }

    var showServiceDialog by remember { mutableStateOf(false) }
    var svcTitle by remember { mutableStateOf("") }
    var svcPrice by remember { mutableStateOf("") }
    var svcDesc by remember { mutableStateOf("") }

    var showPortfolioDialog by remember { mutableStateOf(false) }
    var portDesc by remember { mutableStateOf("") }
    var pendingImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pickProfileImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val url = repo.uploadImage(bytes, "profiles/${profile.userId}")
                onUpdateProfile(profile.userId, mapOf("profileImage" to url)) { res ->
                    res.onSuccess { onMessage(strings.imageUpdated) }
                }
            } catch (e: Exception) {
                onMessage("Upload Error: ${e.localizedMessage}")
            }
        }
    }

    val pickPortfolioImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            pendingImageBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            onMessage("Error reading image")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.profile, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = onToggleLanguage) {
                        Icon(Icons.Outlined.Language, contentDescription = null, tint = Color.White)
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = strings.signOut, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        GalaxyBackground {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.size(80.dp)) {
                        AsyncImage(
                            model = profile.profileImage,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable { pickProfileImage.launch("image/*") },
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                        Text(
                            text = if (profile.role == UserRole.worker) strings.roleWorker else strings.roleCustomer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                SectionHeader(strings.myProfile)
                GlassCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Account Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(
                            onClick = {
                                if (editing) {
                                    if (phone.length != 10) {
                                        onMessage(strings.invalidPhone)
                                        return@TextButton
                                    }
                                    onUpdateProfile(profile.userId, mapOf("name" to name, "phone" to phone, "location" to location)) { res ->
                                        res.onSuccess {
                                            onMessage(strings.profileUpdated)
                                            editing = false
                                        }.onFailure { onMessage(strings.updateFailed) }
                                    }
                                } else {
                                    editing = true
                                }
                            },
                        ) {
                            Text(if (editing) strings.saveChanges else strings.editProfile, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (editing) {
                        KKTextField(name, { name = it }, label = "Full Name")
                        Spacer(Modifier.height(12.dp))
                        KKTextField(phone, { phone = it.filter { ch -> ch.isDigit() }.take(10) }, label = strings.phone, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        Spacer(Modifier.height(12.dp))
                        KKTextField(location, { location = it }, label = strings.location)
                    } else {
                        ProfileInfoItem(Icons.Outlined.Person, profile.name)
                        ProfileInfoItem(Icons.Outlined.Phone, profile.phone.ifBlank { "Not provided" })
                        ProfileInfoItem(Icons.Outlined.LocationOn, profile.location.ifBlank { strings.locationUnknown })
                    }
                }

                if (profile.role == UserRole.worker) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SectionHeader(strings.services) }
                        IconButton(onClick = { showServiceDialog = true }) {
                            Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                    }
                    if (services.isEmpty()) {
                        Text(strings.noServices, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                    } else {
                        services.forEach { s ->
                            ServiceRow(s, strings, onDelete = {
                                onDeleteService(s.serviceId) { res ->
                                    res.onSuccess { onMessage(strings.serviceDeleted) }
                                        .onFailure { onMessage(strings.deleteFailed) }
                                }
                            })
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SectionHeader(strings.portfolio) }
                        IconButton(onClick = { pendingImageBytes = null; portDesc = ""; showPortfolioDialog = true }) {
                            Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                    }
                    if (portfolio.isEmpty()) {
                        Text(strings.noPhotosYet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                    } else {
                        portfolio.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { item ->
                                    PortfolioThumb(item, modifier = Modifier.weight(1f), onDelete = {
                                        scope.launch {
                                            try {
                                                repo.deletePortfolio(item.portfolioId)
                                                onMessage(strings.itemDeleted)
                                            } catch (e: Exception) {
                                                onMessage("Delete Error: ${e.localizedMessage}")
                                            }
                                        }
                                    })
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                Spacer(Modifier.height(64.dp))
            }
        }
    }

    if (showServiceDialog) {
        AlertDialog(
            onDismissRequest = { showServiceDialog = false },
            title = { Text(strings.services, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    KKTextField(svcTitle, { svcTitle = it }, label = strings.serviceName)
                    KKTextField(svcPrice, { svcPrice = it }, label = strings.price, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    KKTextField(svcDesc, { svcDesc = it }, label = strings.serviceDetails, singleLine = false)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val price = svcPrice.toDoubleOrNull() ?: 0.0
                        onAddService(profile.userId, svcTitle.trim(), price, svcDesc.trim()) { res ->
                            res.onSuccess {
                                onMessage(strings.serviceAdded)
                                showServiceDialog = false
                                svcTitle = ""; svcPrice = ""; svcDesc = ""
                            }.onFailure { onMessage(strings.serviceAddFailed) }
                        }
                    },
                ) { Text(strings.save, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showServiceDialog = false }) { Text(strings.cancel) } },
        )
    }

    if (showPortfolioDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) showPortfolioDialog = false },
            title = { Text(strings.portfolio, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    KKTextField(portDesc, { portDesc = it }, label = strings.description, singleLine = false, enabled = !isUploading)
                    Spacer(Modifier.height(16.dp))
                    if (pendingImageBytes != null) Text("Image selected ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { pickPortfolioImage.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isUploading) {
                        Icon(Icons.Outlined.FileUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (pendingImageBytes == null) strings.uploadImage else "Change Image")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pendingImageBytes != null && !isUploading,
                    onClick = {
                        val bytes = pendingImageBytes ?: return@TextButton
                        isUploading = true
                        scope.launch {
                            try {
                                val url = repo.uploadImage(bytes, "portfolio/${profile.userId}")
                                repo.addPortfolioItem(profile.userId, url, portDesc.trim())
                                onMessage(strings.portfolioAdded)
                                showPortfolioDialog = false
                            } catch (e: Exception) {
                                onMessage("Upload Error: ${e.localizedMessage}")
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                ) {
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(strings.save, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { 
                TextButton(enabled = !isUploading, onClick = { showPortfolioDialog = false }) { Text(strings.cancel) } 
            },
        )
    }
}

@Composable
private fun ProfileInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ServiceRow(item: ServiceItem, strings: Strings, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                if (item.description.isNotBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                Text(text = "₹${item.price.toInt()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PortfolioThumb(item: PortfolioItem, modifier: Modifier = Modifier, onDelete: () -> Unit) {
    Column(modifier) {
        Box {
            AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)) {
                Icon(Icons.Outlined.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        if (item.description.isNotBlank()) {
            Text(text = item.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.LightGray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
}
