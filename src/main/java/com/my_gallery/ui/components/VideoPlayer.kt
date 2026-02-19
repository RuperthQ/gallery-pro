package com.my_gallery.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    autoplayEnabled: Boolean = true,
    isActive: Boolean = true,
    isFullScreen: Boolean = false,
    showControls: Boolean = true,
    onFullScreenChange: (Boolean) -> Unit = {},
    onControlsVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // LoadControl optimizado: Menos buffer = Menos RAM
    val loadControl = remember {
        androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5000,  // minBufferMs (reducido)
                15000, // maxBufferMs (reducido)
                1000,  // bufferForPlaybackMs
                1500   // bufferForPlaybackAfterRebufferMs
            )
            .build()
    }

    // Inicializamos ExoPlayer con configuraciones de bajo consumo
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setVideoScalingMode(android.media.MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    // Sincronización inteligente con el Pager e Interfaz
    LaunchedEffect(videoUrl, isActive, autoplayEnabled) {
        if (isActive) {
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = autoplayEnabled
        } else {
            // Liberamos buffers y decodificadores si no estamos en pantalla
            exoPlayer.stop()
        }
    }

    // Gestionamos el ciclo de vida del player
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    val density = LocalDensity.current
    val hPaddingPx = with(density) { (if (isFullScreen) 64.dp else 0.dp).roundToPx() }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                
                // Configuración de Pantalla Completa / Rotación
                setFullscreenButtonClickListener { isFullScreen ->
                    onFullScreenChange(isFullScreen)
                    val activity = ctx as? Activity
                    activity?.requestedOrientation = if (isFullScreen) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }

                setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                    onControlsVisibilityChange(visibility == android.view.View.VISIBLE)
                })

                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            // Sincronización inteligente: Solo actuamos si el estado deseado de Compose
            // difiere de la realidad del reproductor nativo.
            val isCurrentlyVisible = playerView.isControllerFullyVisible
            if (showControls && !isCurrentlyVisible) {
                playerView.showController()
            } else if (!showControls && isCurrentlyVisible) {
                playerView.hideController()
            }
            
            // El truco definitivo: El contenedor de controles debe ser 100% (fondo)
            // pero su CONTENIDO (botones, etc) debe tener el padding.
            for (i in 0 until playerView.childCount) {
                val child = playerView.getChildAt(i)
                if (child !is AspectRatioFrameLayout) {
                    // Quitamos padding del overlay para que el fondo negro translúcido cubra todo
                    child.setPadding(0, 0, 0, 0)
                    
                    // Aplicamos el padding a los hijos internos (los controles reales)
                    if (child is ViewGroup) {
                        for (j in 0 until child.childCount) {
                            val innerChild = child.getChildAt(j)
                            innerChild.setPadding(hPaddingPx, 0, hPaddingPx, 0)
                        }
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    )
}
