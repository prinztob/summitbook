package de.drtobiasprinz.summitbook.ui.compose

import de.drtobiasprinz.summitbook.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.theme.ChartTextLightGray
import de.drtobiasprinz.summitbook.ui.theme.DarkCanvasDeep
import de.drtobiasprinz.summitbook.ui.theme.Scrim
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

    val containerSize = LocalWindowInfo.current.containerSize
    val screenHeight = with(LocalDensity.current) { containerSize.height.toDp() }

    var showFullscreenViewer by rememberSaveable { mutableStateOf(false) }
    var selectedImageIndex by rememberSaveable { mutableIntStateOf(0) }
    var imageFiles by remember { mutableStateOf<List<File>?>(null) }

    // Asynchronously prepare the list of files to avoid blocking the UI thread.
    // Key on imageIds too, so re-analyzed summits with changed images reload.
    LaunchedEffect(summit.id, summit.imageIds) {
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
                        text = stringResource(R.string.no_images_available),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Fullscreen image viewer dialog (uses the already IO-loaded file list)
    val fullscreenFiles = imageFiles
    if (showFullscreenViewer && !fullscreenFiles.isNullOrEmpty()) {
        FullscreenImageViewer(
            summit = summit,
            imageFiles = fullscreenFiles,
            startPosition = selectedImageIndex.coerceIn(0, fullscreenFiles.size - 1),
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
            var imageState by remember(imageFiles[page]) {
                mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageFiles[page])
                    .crossfade(true)
                    // Match the decode size to the carousel display size
                    .size(width = 1080, height = 1080)
                    .memoryCacheKey(imageFiles[page].absolutePath)
                    .diskCacheKey(imageFiles[page].absolutePath)
                    .build(),
                contentDescription = stringResource(R.string.summit_image_cd, page + 1),
                contentScale = ContentScale.Crop,
                onState = { imageState = it },
                modifier = Modifier.fillMaxSize()
            )
            when (imageState) {
                is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
                is AsyncImagePainter.State.Error -> Text(
                    text = stringResource(R.string.failed_to_load_image),
                    color = MaterialTheme.colorScheme.error
                )
                else -> Unit
            }
        }
    }
}

@Composable
fun FullscreenImageViewer(
    summit: Summit,
    imageFiles: List<File>,
    startPosition: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkCanvasDeep
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
                val maslText = stringResource(R.string.masl)
                val hmText = stringResource(R.string.hm)
                val kmText = stringResource(R.string.km)
                val imageDescription = remember(summit, pagerState.currentPage, maslText, hmText, kmText) {
                    summit.getImageDescription(maslText, hmText, kmText, pagerState.currentPage)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Scrim)
                        .navigationBarsPadding()
                        .displayCutoutPadding()
                        .padding(16.dp)
                ) {
                    Text(
                        text = imageDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ChartTextLightGray
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
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset() {
        if (scale <= 1f) {
            offset = Offset.Zero
            return
        }
        val maxX = viewSize.width * (scale - 1f) / 2f
        val maxY = viewSize.height * (scale - 1f) / 2f
        offset = Offset(
            offset.x.coerceIn(-maxX, maxX),
            offset.y.coerceIn(-maxY, maxY)
        )
    }

    val state = rememberTransformableState { panChange: Offset, zoomChange: Float, _, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)

        // Only allow panning when zoomed in, clamped so the image stays in view
        if (scale > 1f) {
            offset += panChange
            clampOffset()
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapPosition ->
                        // Toggle between zoomed and normal
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val newScale = 2f
                            // graphicsLayer scales around the center; keep the
                            // content coordinate under the finger fixed
                            val centerX = viewSize.width / 2f
                            val centerY = viewSize.height / 2f
                            offset = Offset(
                                (tapPosition.x - centerX) * (1f - newScale) + offset.x * newScale,
                                (tapPosition.y - centerY) * (1f - newScale) + offset.y * newScale
                            )
                            scale = newScale
                            clampOffset()
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
        var imageState by remember(imageFile) {
            mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageFile)
                .crossfade(true)
                .size(width = 2560, height = 2560) // Limit max size for fullscreen
                .memoryCacheKey(imageFile.absolutePath)
                .diskCacheKey(imageFile.absolutePath)
                .build(),
            contentDescription = stringResource(R.string.cd_fullscreen_image),
            contentScale = ContentScale.Fit,
            onState = { imageState = it },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = state)
        )
        when (imageState) {
            is AsyncImagePainter.State.Loading -> CircularProgressIndicator(
                color = ChartTextLightGray
            )
            is AsyncImagePainter.State.Error -> Text(
                text = stringResource(R.string.failed_to_load_image),
                color = MaterialTheme.colorScheme.error
            )
            else -> Unit
        }
    }
}
