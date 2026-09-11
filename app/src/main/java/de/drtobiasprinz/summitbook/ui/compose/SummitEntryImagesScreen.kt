package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import de.drtobiasprinz.summitbook.db.entities.Summit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SummitEntryImagesScreen(
    summit: Summit?,
    modifier: Modifier = Modifier
) {
    if (summit == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    var showFullscreenViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var imageFiles by remember { mutableStateOf<List<File>?>(null) }

    // Asynchronously prepare the list of files to avoid blocking the UI thread.
    LaunchedEffect(summit.id) {
        imageFiles = null // Reset on new summit
        withContext(Dispatchers.IO) {
            imageFiles = if (summit.hasImagePath()) {
                summit.imageIds.map { imageId ->
                    summit.getImagePath(imageId).toFile()
                }
            } else {
                emptyList()
            }
        }
    }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // Use the state variable `imageFiles` here
        val currentImageFiles = imageFiles
        when {
            currentImageFiles == null -> {
                // Files are being loaded
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            currentImageFiles.isNotEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight * 0.7f)
                ) {
                    ImageCarousel(
                        imageFiles = currentImageFiles,
                        onImageClick = { position ->
                            selectedImageIndex = position
                            showFullscreenViewer = true
                        }
                    )
                }
            }

            else -> {
                // Empty list, no images available
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No images available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Fullscreen image viewer dialog
    if (showFullscreenViewer && summit.hasImagePath()) {
        FullscreenImageViewer(
            summit = summit,
            startPosition = selectedImageIndex,
            onDismiss = { showFullscreenViewer = false }
        )
    }
}

@Composable
fun ImageCarousel(
    imageFiles: List<File>,
    onImageClick: (Int) -> Unit
) {
    val pagerState = rememberPagerState(
        pageCount = { imageFiles.size }
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { page -> imageFiles[page].absolutePath }
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onImageClick(page) },
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageFiles[page])
                    .crossfade(true)
                    .size(width = 1920, height = 1920) // Limit max size to prevent memory issues
                    .scale(Scale.FIT)
                    .memoryCacheKey(imageFiles[page].absolutePath)
                    .diskCacheKey(imageFiles[page].absolutePath)
                    .build(),
                contentDescription = "Summit image ${page + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Failed to load image",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun FullscreenImageViewer(
    summit: Summit,
    startPosition: Int,
    onDismiss: () -> Unit
) {
    val imageFiles: List<File> = remember(summit.id) {
        summit.imageIds.map { imageId ->
            summit.getImagePath(imageId).toFile()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val pagerState = rememberPagerState(
                    initialPage = startPosition,
                    pageCount = { imageFiles.size }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> imageFiles[page].absolutePath }
                ) { page ->
                    ZoomableImage(
                        imageFile = imageFiles[page],
                        onDismiss = onDismiss
                    )
                }

                // Image description overlay at the bottom
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = summit.getImageDescription(
                            context.resources,
                            pagerState.currentPage
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(
    imageFile: File,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { panChange: Offset, zoomChange: Float, _, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)

        // Only allow panning when zoomed in
        if (scale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Toggle between zoomed and normal
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2f
                        }
                    },
                    onTap = {
                        // Single tap to dismiss
                        if (scale == 1f) {
                            onDismiss()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageFile)
                .crossfade(true)
                .size(width = 2560, height = 2560) // Limit max size for fullscreen
                .scale(Scale.FIT)
                .memoryCacheKey(imageFile.absolutePath)
                .diskCacheKey(imageFile.absolutePath)
                .build(),
            contentDescription = "Fullscreen image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = state),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Failed to load image",
                        color = Color.White
                    )
                }
            }
        )
    }
}
