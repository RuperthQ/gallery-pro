package com.my_gallery.ui.security

import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.theme.GalleryDesign

@Composable
fun AppLockScreen(
    onAuthenticated: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    val biometricManager = remember {
        if (context is FragmentActivity) BiometricPromptManager(context) else null
    }

    LaunchedEffect(Unit) {
        if (biometricManager?.canAuthenticate() == true) {
            biometricManager.authenticate(
                title = "Autenticación requerida",
                subtitle = "Usa tu huella para desbloquear la galería",
                onSuccess = {
                    viewModel.setAuthenticated(true)
                    onAuthenticated()
                },
                onError = { error ->
                    viewModel.setError(error)
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(GalleryDesign.PaddingLarge)
        ) {
            @OptIn(ExperimentalFoundationApi::class)
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Pantalla Bloqueada",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(80.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = {
                            viewModel.enterDecoyMode()
                            onAuthenticated()
                        }
                    )
            )

            Spacer(modifier = Modifier.height(GalleryDesign.PaddingMedium))

            Text(
                text = "Galería Bloqueada",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(GalleryDesign.PaddingSmall))

            Text(
                text = "Requiere autenticación biométrica.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (authError != null) {
                Spacer(modifier = Modifier.height(GalleryDesign.PaddingMedium))
                Text(
                    text = authError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(GalleryDesign.PaddingLarge))

            Button(
                onClick = {
                    if (biometricManager?.canAuthenticate() == true) {
                        biometricManager.authenticate(
                            title = "Autenticación requerida",
                            subtitle = "Usa tu huella para desbloquear",
                            onSuccess = {
                                viewModel.setAuthenticated(true)
                                onAuthenticated()
                            },
                            onError = { error ->
                                viewModel.setError(error)
                            }
                        )
                    } else {
                        viewModel.setError("Hardware biométrico no disponible.")
                    }
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(GalleryDesign.PaddingSmall))
                Text("Desbloquear")
            }
        }
    }
}
