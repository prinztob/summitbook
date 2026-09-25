package de.drtobiasprinz.summitbook.ui.compose

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.yalantis.ucrop.UCrop
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_PICKED_IMAGES = 10

/**
 * A single entry of the sequential crop queue.
 */
data class PendingCrop(val uri: String, val landscape: Boolean)

private val PendingCropsSaver = listSaver<List<PendingCrop>, Any>(
    save = { list -> list.flatMap { listOf(it.uri, it.landscape) } },
    restore = { flat -> flat.chunked(2).map { PendingCrop(it[0] as String, it[1] as Boolean) } }
)

/**
 * Jetpack Compose version of AddImagesActivity
 * Replaces the Activity-based implementation with a modern Compose Dialog
 */
@Composable
fun AddImagesDialogCompose(
    summit: Summit?,
    onDismiss: () -> Unit,
    onSaveSummit: (Boolean, Summit) -> Job,
    onShowSnackbar: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Eager, isolated clone: mutations of imageIds must not leak into the caller's Summit
    var localSummit by remember(summit) {
        mutableStateOf(
            summit?.let { s -> s.clone().also { cloned -> cloned.imageIds = s.imageIds.toMutableList() } }
        )
    }
    var imageFiles by remember { mutableStateOf<List<Pair<Int, File>>>(emptyList()) }
    var canImageBeOnFirstPosition by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageToDelete by remember { mutableStateOf<Int?>(null) }

    // Sequential crop queue state
    var pendingCrops by rememberSaveable(stateSaver = PendingCropsSaver) {
        mutableStateOf(emptyList<PendingCrop>())
    }
    var cropInFlight by rememberSaveable { mutableStateOf(false) }
    var batchTotal by rememberSaveable { mutableIntStateOf(0) }
    var batchAddedCount by rememberSaveable { mutableIntStateOf(0) }

    // In-dialog feedback (activity snackbars are invisible behind the dialog window)
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackAddedImages by remember { mutableStateOf<Int?>(null) }

    // String resources for use in callbacks
    val deleteImageDone = stringResource(R.string.delete_image_done)
    val addImageCanceled = stringResource(R.string.add_image_canceled)
    val firstImageMustBeLandscape = stringResource(R.string.error_first_image_must_be_landscape)
    val errorCroppingImage = stringResource(R.string.error_cropping_image)
    val errorDeleteImage = stringResource(R.string.error_delete_image)
    val maxImagesNotice = stringResource(R.string.max_images_notice, MAX_PICKED_IMAGES)

    // Auto-clear feedback
    LaunchedEffect(feedbackMessage, feedbackAddedImages) {
        if (feedbackMessage != null || feedbackAddedImages != null) {
            delay(4000)
            feedbackMessage = null
            feedbackAddedImages = null
        }
    }

    // Load image files whenever the underlying summit instance changes
    LaunchedEffect(summit) {
        val s = localSummit ?: return@LaunchedEffect
        isLoading = true
        val (files, canBeFirst) = loadImageFiles(s)
        imageFiles = files
        canImageBeOnFirstPosition = canBeFirst
        isLoading = false
    }

    fun reloadImages() {
        val currentSummit = localSummit ?: return
        scope.launch {
            val (files, canBeFirst) = loadImageFiles(currentSummit)
            imageFiles = files
            canImageBeOnFirstPosition = canBeFirst
            isLoading = false
        }
    }

    fun canBeFirst(imageId: Int): Boolean = canImageBeOnFirstPosition[imageId] ?: true

    // A swap is only allowed if the image landing on position 0 is landscape
    fun swapAllowed(position: Int, target: Int): Boolean {
        val ids = localSummit?.imageIds ?: return false
        if (position !in ids.indices || target !in ids.indices) return false
        val landingOnFirstPosition = when {
            target == 0 -> ids[position]
            position == 0 -> ids[target]
            else -> null
        }
        return landingOnFirstPosition == null || canBeFirst(landingOnFirstPosition)
    }

    fun moveImage(position: Int, delta: Int) {
        val currentSummit = localSummit ?: return
        val target = position + delta
        if (!swapAllowed(position, target)) return
        val ids = currentSummit.imageIds
        val temp = ids[position]
        ids[position] = ids[target]
        ids[target] = temp
        isLoading = true
        onSaveSummit(true, currentSummit)
        reloadImages()
    }

    fun handleDeleteImage(imageId: Int) {
        val currentSummit = localSummit ?: return
        val imagePath = currentSummit.getImagePath(imageId).toFile()

        isLoading = true
        scope.launch {
            val deleted = withContext(Dispatchers.IO) { !imagePath.exists() || imagePath.delete() }
            if (deleted) {
                currentSummit.imageIds.remove(imageId)
                // Keep a landscape image on the first position if possible
                val ids = currentSummit.imageIds
                if (ids.isNotEmpty() && !canBeFirst(ids.first())) {
                    val firstLandscape = ids.firstOrNull { canBeFirst(it) }
                    if (firstLandscape != null) {
                        ids.remove(firstLandscape)
                        ids.add(0, firstLandscape)
                    }
                }
                onSaveSummit(true, currentSummit)
                reloadImages()
                feedbackMessage = deleteImageDone
            } else {
                isLoading = false
                feedbackMessage = errorDeleteImage
            }
        }
    }

    // UCrop result launcher
    val uCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        cropInFlight = false
        val currentSummit = localSummit ?: return@rememberLauncherForActivityResult
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                currentSummit.getNextImagePath(true)
                onSaveSummit(true, currentSummit)
                batchAddedCount++
                if (pendingCrops.isEmpty()) {
                    feedbackMessage = null
                    feedbackAddedImages = batchAddedCount
                    batchTotal = 0
                    reloadImages()
                }
            }
            UCrop.RESULT_ERROR -> {
                pendingCrops = emptyList()
                batchTotal = 0
                feedbackMessage = errorCroppingImage
                feedbackAddedImages = null
                reloadImages()
            }
            else -> {
                val added = batchAddedCount
                pendingCrops = emptyList()
                batchTotal = 0
                if (added > 0) {
                    feedbackMessage = null
                    feedbackAddedImages = added
                } else {
                    feedbackMessage = addImageCanceled
                    feedbackAddedImages = null
                }
                reloadImages()
            }
        }
    }

    // Starts the next queued crop session whenever the queue is non-empty and no crop is running
    LaunchedEffect(pendingCrops, cropInFlight) {
        if (pendingCrops.isEmpty() || cropInFlight) return@LaunchedEffect
        val currentSummit = localSummit
        val activity = context as? Activity
        if (currentSummit == null || activity == null) {
            pendingCrops = emptyList()
            return@LaunchedEffect
        }
        cropInFlight = true
        val next = pendingCrops.first()
        pendingCrops = pendingCrops.drop(1)
        val cropValues = if (next.landscape) CropValues.HORIZONTAL else CropValues.VERTICAL
        val destinationUri = currentSummit.getNextImagePath().toFile().toUri()
        val options = UCrop.Options().apply {
            setToolbarTitle("${batchTotal - pendingCrops.size}/$batchTotal")
        }
        UCrop.of(next.uri.toUri(), destinationUri)
            .withAspectRatio(cropValues.width, cropValues.height)
            .withMaxResultSize(2048, 2048)
            .withOptions(options)
            .start(activity, uCropLauncher)
    }

    // File picker launcher for adding images (multiple selection)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            }
            if (uris.isEmpty()) {
                result.data?.data?.let { uris.add(it) }
            }
            val currentSummit = localSummit
            if (uris.isEmpty() || currentSummit == null) {
                return@rememberLauncherForActivityResult
            }
            if (uris.size > MAX_PICKED_IMAGES) {
                feedbackMessage = maxImagesNotice
            }
            val selectedUris = uris.take(MAX_PICKED_IMAGES)

            isLoading = true
            scope.launch {
                val ordered = withContext(Dispatchers.IO) {
                    selectedUris.map { uri -> uri to isLandscapeImage(context.contentResolver, uri) }
                        .sortedBy { !it.second } // landscape images first, stable order
                }
                if (currentSummit.imageIds.isEmpty() && ordered.none { it.second }) {
                    isLoading = false
                    feedbackMessage = firstImageMustBeLandscape
                } else {
                    batchTotal = ordered.size
                    batchAddedCount = 0
                    pendingCrops = ordered.map { (uri, landscape) ->
                        PendingCrop(uri.toString(), landscape)
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        localSummit?.let { s ->
                            Text(
                                text = s.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = s.sportType.imageIdBlack),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = s.sportType.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_cancel_24),
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image list with empty state and loading overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (imageFiles.isEmpty() && !isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_images_yet),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                imageFiles,
                                key = { _, (imageId, _) -> imageId }
                            ) { index, (imageId, file) ->
                                ImageItem(
                                    file = file,
                                    canMoveUp = index > 0 && swapAllowed(index, index - 1),
                                    canMoveDown = index < imageFiles.size - 1 && swapAllowed(index, index + 1),
                                    onDelete = {
                                        imageToDelete = imageId
                                        showDeleteDialog = true
                                    },
                                    onMoveUp = { moveImage(index, -1) },
                                    onMoveDown = { moveImage(index, 1) }
                                )
                            }
                        }
                    }

                    // Loading overlay: keeps the list visible but blocks interaction
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                                .pointerInput(Unit) { detectTapGestures { } },
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // In-dialog feedback banner
                val feedbackText = feedbackMessage
                    ?: feedbackAddedImages?.let { pluralStringResource(R.plurals.images_added, it, it) }
                feedbackText?.let { message ->
                    FeedbackBanner(message = message, onDismiss = {
                        feedbackMessage = null
                        feedbackAddedImages = null
                    })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add images button
                AddImageButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                        filePickerLauncher.launch(intent)
                    }
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && imageToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(text = stringResource(R.string.delete_image_title, localSummit?.name ?: ""))
            },
            text = {
                Text(text = stringResource(R.string.delete_image_text))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        imageToDelete?.let { handleDeleteImage(it) }
                        showDeleteDialog = false
                        imageToDelete = null
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        imageToDelete = null
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_delete_black_24dp),
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun ImageItem(
    file: File,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Image
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(file)
                .crossfade(true)
                .build(),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Delete button overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_delete_black_24dp),
                    contentDescription = stringResource(R.string.delete_image_action),
                    tint = Color.White
                )
            }
        }

        // Move buttons (vertical stack on the right)
        if (canMoveUp || canMoveDown) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (canMoveUp) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_arrow_upward_24),
                            contentDescription = stringResource(R.string.move_up),
                            tint = Color.White
                        )
                    }
                }

                if (canMoveDown) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_arrow_downward_24),
                            contentDescription = stringResource(R.string.move_down),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddImageButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_add_photo_alternate_black_24dp),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.add_images))
    }
}

@Composable
private fun FeedbackBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_cancel_24),
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Loads the image files for a summit and determines which images can be on first position
 */
private suspend fun loadImageFiles(
    summit: Summit
): Pair<List<Pair<Int, File>>, Map<Int, Boolean>> = withContext(Dispatchers.IO) {
    val files = mutableListOf<Pair<Int, File>>()
    val canBeFirst = mutableMapOf<Int, Boolean>()

    if (summit.hasImagePath()) {
        for (imageId in summit.imageIds) {
            val file = summit.getImagePath(imageId).toFile()
            files.add(imageId to file)

            // Determine if image can be on first position (width > height)
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                canBeFirst[imageId] = options.outWidth > options.outHeight
            } catch (_: Exception) {
                canBeFirst[imageId] = true // Default to true if we can't determine
            }
        }
    }

    files to canBeFirst
}

/**
 * Determines whether the image behind the given Uri is in landscape orientation
 * (width > height). Defaults to true if the bounds cannot be read.
 */
private fun isLandscapeImage(resolver: ContentResolver, uri: Uri): Boolean {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        if (options.outWidth > 0 && options.outHeight > 0) {
            options.outWidth > options.outHeight
        } else {
            true
        }
    } catch (_: Exception) {
        true
    }
}

/**
 * Data class for crop aspect ratio values
 */
data class CropValues(
    val width: Float,
    val height: Float
) {
    companion object {
        val HORIZONTAL = CropValues(16f, 9f)
        val VERTICAL = CropValues(9f, 16f)
    }
}
