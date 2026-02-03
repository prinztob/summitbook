package de.drtobiasprinz.summitbook.ui.compose

import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.yalantis.ucrop.UCrop
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Jetpack Compose version of AddImagesActivity
 * Replaces the Activity-based implementation with a modern Compose Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddImagesDialogCompose(
    summit: Summit?,
    onDismiss: () -> Unit,
    onSaveSummit: (Boolean, Summit) -> Job,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var localSummit by remember { mutableStateOf<Summit?>(null) }
    var imageFiles by remember { mutableStateOf<List<Pair<Int, File>>>(emptyList()) }
    var canImageBeOnFirstPosition by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageToDelete by remember { mutableStateOf<Int?>(null) }

    // String resources for use in callbacks
    val deleteImageDone = stringResource(R.string.delete_image_done)
    val deleteCancel = stringResource(R.string.delete_cancel)

    // Crop selection state
    var selectedCropValues by remember { mutableStateOf(CropValues.HORIZONTAL) }

    // Load summit data
    LaunchedEffect(summit) {
        summit?.let { s ->
            localSummit = s.clone()
            loadImageFiles(s) { files, canBeFirst ->
                imageFiles = files
                canImageBeOnFirstPosition = canBeFirst
            }
        }
    }

    // UCrop result launcher (must be defined before filePickerLauncher)
    val uCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val currentSummit = localSummit ?: return@rememberLauncherForActivityResult
            currentSummit.getNextImagePath(true)
            onSaveSummit(true, currentSummit)
            // Reload images
            scope.launch {
                loadImageFiles(currentSummit) { files, canBeFirst ->
                    imageFiles = files
                    canImageBeOnFirstPosition = canBeFirst
                }
            }
        }
    }

    // File picker launcher for adding images
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { sourceUri ->
                val currentSummit = localSummit ?: return@let
                val destinationUri = currentSummit.getNextImagePath().toFile().toUri()

                UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(selectedCropValues.width, selectedCropValues.height)
                    .withMaxResultSize(2048, 2048)
                    .start(context as android.app.Activity, uCropLauncher)
            }
        }
    }

    // Handle image deletion
    fun handleDeleteImage(imageId: Int) {
        val currentSummit = localSummit ?: return
        val imagePath = currentSummit.getImagePath(imageId).toFile()

        if (imagePath.delete()) {
            currentSummit.imageIds.remove(imageId)
            onSaveSummit(true, currentSummit)
            // Reload images
            scope.launch {
                loadImageFiles(currentSummit) { files, canBeFirst ->
                    imageFiles = files
                    canImageBeOnFirstPosition = canBeFirst
                }
            }
            Toast.makeText(
                context,
                deleteImageDone,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Handle moving image up
    fun handleMoveImageUp(position: Int) {
        val currentSummit = localSummit ?: return
        if (position > 0) {
            val temp = currentSummit.imageIds[position]
            currentSummit.imageIds[position] = currentSummit.imageIds[position - 1]
            currentSummit.imageIds[position - 1] = temp
            onSaveSummit(true, currentSummit)
            // Reload images
            scope.launch {
                loadImageFiles(currentSummit) { files, canBeFirst ->
                    imageFiles = files
                    canImageBeOnFirstPosition = canBeFirst
                }
            }
        }
    }

    // Handle moving image down
    fun handleMoveImageDown(position: Int) {
        val currentSummit = localSummit ?: return
        if (position < currentSummit.imageIds.size - 1) {
            val temp = currentSummit.imageIds[position]
            currentSummit.imageIds[position] = currentSummit.imageIds[position + 1]
            currentSummit.imageIds[position + 1] = temp
            onSaveSummit(true, currentSummit)
            // Reload images
            scope.launch {
                loadImageFiles(currentSummit) { files, canBeFirst ->
                    imageFiles = files
                    canImageBeOnFirstPosition = canBeFirst
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
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

                // Images list
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(imageFiles) { index, (imageId, file) ->
                            ImageItem(
                                file = file,
                                index = index,
                                totalImages = imageFiles.size,
                                isVerticalImageOnNextPosition = index == 0 &&
                                        imageFiles.size > 1 &&
                                        (canImageBeOnFirstPosition[imageFiles.getOrNull(1)?.first] == false),
                                isVerticalImageOnSecondPosition = index == 1 &&
                                        (canImageBeOnFirstPosition[imageId] == false),
                                onDelete = {
                                    imageToDelete = imageId
                                    showDeleteDialog = true
                                },
                                onMoveUp = { handleMoveImageUp(index) },
                                onMoveDown = { handleMoveImageDown(index) }
                            )
                        }

                        // Add new image buttons
                        item {
                            AddImageButtons(
                                onAddHorizontal = {
                                    selectedCropValues = CropValues.HORIZONTAL
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                    }
                                    filePickerLauncher.launch(intent)
                                },
                                onAddVertical = {
                                    selectedCropValues = CropValues.VERTICAL
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                    }
                                    filePickerLauncher.launch(intent)
                                },
                                hasImages = imageFiles.isNotEmpty()
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && imageToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(text = stringResource(R.string.delete_image, localSummit?.name ?: ""))
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
                        Toast.makeText(
                            context,
                            deleteCancel,
                            Toast.LENGTH_SHORT
                        ).show()
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
    index: Int,
    totalImages: Int,
    isVerticalImageOnNextPosition: Boolean,
    isVerticalImageOnSecondPosition: Boolean,
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

        // Action buttons overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_delete_black_24dp),
                    contentDescription = stringResource(R.string.delete),
                    tint = Color.White
                )
            }
        }

        // Move buttons (vertical stack on the right)
        if (!isVerticalImageOnNextPosition) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Up button
                val canMoveUp = index > 0 && !isVerticalImageOnSecondPosition
                if (canMoveUp) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
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

                // Down button
                val canMoveDown = index < totalImages - 1 && !isVerticalImageOnNextPosition
                if (canMoveDown) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
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
private fun AddImageButtons(
    onAddHorizontal: () -> Unit,
    onAddVertical: () -> Unit,
    hasImages: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.add_image),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Horizontal image button
            Button(
                onClick = onAddHorizontal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_panorama_horizontal_24),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.horizontal))
            }

            // Vertical image button (only show if there are already images)
            if (hasImages) {
                Button(
                    onClick = onAddVertical,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_panorama_vertical_24),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.vertical))
                }
            }
        }
    }
}

/**
 * Asynchronously load image files and determine which images can be on first position
 */
private suspend fun loadImageFiles(
    summit: Summit,
    onResult: (List<Pair<Int, File>>, Map<Int, Boolean>) -> Unit
) {
    withContext(Dispatchers.IO) {
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

        withContext(Dispatchers.Main) {
            onResult(files, canBeFirst)
        }
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