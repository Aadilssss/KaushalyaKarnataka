package com.kaushalyakarnataka.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kaushalyakarnataka.app.BuildConfig
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.util.findActivity

@Composable
fun LoginScreen(
    strings: Strings,
    onSignInWithGoogle: (String, (Result<Unit>) -> Unit) -> Unit,
    onMessage: (String) -> Unit,
) {
    val activity = LocalContext.current.findActivity()
    var isSigningIn by remember { mutableStateOf(false) }

    val googleSignInClient = remember(activity) {
        val webId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webId)
            .requestEmail()
            .requestProfile()
            .build()
        activity?.let { GoogleSignIn.getClient(it, gso) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account?.idToken
            
            if (token.isNullOrBlank()) {
                isSigningIn = false
                onMessage("Error: Missing token. Check Firebase config.")
            } else {
                onSignInWithGoogle(token) { res ->
                    res.onFailure {
                        isSigningIn = false
                        onMessage(it.message ?: "Login failed")
                    }
                    // onSuccess is handled by the auth observer in MainActivity
                }
            }
        } catch (e: ApiException) {
            isSigningIn = false
            val msg = when (e.statusCode) {
                12501 -> "Sign-in cancelled."
                7 -> "Network error. Try again."
                10 -> "Developer Error (10): Likely SHA-1 mismatch in Firebase."
                else -> "Login failed (Code: ${e.statusCode})."
            }
            onMessage(msg)
        }
    }

    val webIdConfigured = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineMuted = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    GalaxyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = strings.appName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(48.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        enabled = !isSigningIn && activity != null && webIdConfigured,
                        onClick = {
                            val client = googleSignInClient ?: return@OutlinedButton
                            isSigningIn = true
                            launcher.launch(client.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.38f),
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.alpha(if (isSigningIn) 0f else 1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White,
                                ) {
                                    Text(
                                        "G",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = strings.googleSignIn,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (!webIdConfigured) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Google sign-in isn't configured for this install yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = strings.safeLogin,
                style = MaterialTheme.typography.labelMedium,
                color = outlineMuted,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
