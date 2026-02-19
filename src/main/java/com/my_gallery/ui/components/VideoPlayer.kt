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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.BorderWidthNone

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    videoWidth: Int = 0,
    videoHeight: Int = 0,
    autoplayEnabled: Boolean = true,
    isActive: Boolean = true,
    isFullScreen: Boolean = false,
    showControls: Boolean = true,
    onFullScreenChange: (Boolean) -> Unit = {},
    onControlsVisibilityChange: (Boolean) -> Unit = {},
    onVideoOrientationDetected: (Boolean) -> Unit = {},
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

    var isActuallyHorizontal by remember { mutableStateOf(videoWidth >= videoHeight) }

    // Escuchamos el tamaño real del video procesado (ignora flags de rotación erróneos)
    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val isHorizontal = videoSize.width > videoSize.height
                    isActuallyHorizontal = isHorizontal
                    onVideoOrientationDetected(isHorizontal)
                }
            }
        }
        exoPlayer.addListener(listener)
    }

    // Gestionamos el ciclo de vida del player
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    val density = LocalDensity.current
    val hPaddingPx = with(density) { 
        val padding = when {
            !isFullScreen -> BorderWidthNone
            isActuallyHorizontal -> GalleryDesign.PaddingViewerLockH
            else -> GalleryDesign.PaddingViewerLockH / 2 // 32dp
        }
        padding.roundToPx() 
    }

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
                    
                    // Usamos el videoSize real del reproductor en ese momento
                    val videoSize = exoPlayer.videoSize
                    val isHorizontal = videoSize.width > videoSize.height
                    
                    activity?.requestedOrientation = if (isFullScreen && isHorizontal) {
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
