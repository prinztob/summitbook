package de.drtobiasprinz.summitbook.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling.Companion.getOsmdroidTilesFolder
import de.drtobiasprinz.summitbook.ui.MapProvider
import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.hills.DemFolderFS
import org.mapsforge.map.layer.hills.HillsRenderConfig
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource
import org.mapsforge.map.layer.hills.SimpleShadingAlgorithm
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback
import org.osmdroid.mapsforge.MapsForgeTileProvider
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.modules.MapTileDownloader
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck
import org.osmdroid.tileprovider.modules.TileWriter
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import java.io.File
import java.io.FileInputStream


object MapTilesHelper {
    const val TAG = "MapTilesHelper"
    fun getOnlineMapProvider(tileSource: ITileSource, context: Context): MapTileProviderArray {
        val tileWriter = TileWriter()
        val registerReceiver = SimpleRegisterReceiver(context)
        val fileSystemProvider = MapTileFilesystemProvider(
            registerReceiver, tileSource
        )
        val networkAvailabilityCheck = NetworkAvailabliltyCheck(context)
        val downloaderProvider = MapTileDownloader(
            tileSource, tileWriter, networkAvailabilityCheck
        )
        val mapTileApprox = MapTileApproximater()
        return MapTileProviderArray(
            tileSource, registerReceiver, arrayOf(
                fileSystemProvider, mapTileApprox, downloaderProvider
            )
        )
    }

    /**
     * Create a hill shading configuration for Mapsforge maps
     * @param demFolder Directory containing HGT files for digital elevation model
     * @param graphicFactory GraphicFactory instance (use AndroidGraphicFactory.INSTANCE)
     * @return HillsRenderConfig configured for hill shading, or null if setup fails
     */
    fun createHillShadingConfig(
        demFolder: File, graphicFactory: GraphicFactory = AndroidGraphicFactory.INSTANCE
    ): HillsRenderConfig? {
        return try {
            if (!demFolder.exists() || !demFolder.isDirectory) {
                Log.w(
                    "MapHelper",
                    "DEM folder does not exist or is not a directory: ${demFolder.absolutePath}"
                )
                return null
            }
            val demFolderWrapper = DemFolderFS(demFolder)
            val hgtCache = MemoryCachingHgtReaderTileSource(
                demFolderWrapper, SimpleShadingAlgorithm(), graphicFactory
            )
            HillsRenderConfig(hgtCache)
        } catch (e: Exception) {
            Log.e("MapHelper", "Error creating hill shading config", e)
            null
        }
    }

    /**
     * Get offline map provider with hill shading support
     * Note: Hill shading with osmdroid-mapsforge requires direct integration with the underlying
     * Mapsforge rendering. The HillsRenderConfig created by createHillShadingConfig() can be
     * used with pure Mapsforge implementations. For osmdroid integration, hill shading would
     * need to be implemented at a lower level in the rendering pipeline.
     *
     * @param context Android context
     * @param mapFiles List of map files to use
     * @param item MapProvider configuration
     * @return MapsForgeTileProvider, or null if setup fails
     */
    fun getOfflineMapProviderWithHillShading(
        context: Context, mapFiles: List<DocumentFile>, item: MapProvider
    ): MapsForgeTileProvider? {
        try {
            val theme = AssetsRenderTheme(
                context.assets, "rendertheme/", "Elevate.xml", XmlRenderThemeMenuCallback { style ->
                    val renderThemeStyleLayer = style.getLayer(item.offlineStyle)
                    if (renderThemeStyleLayer == null) {
                        Log.w("AssetsRenderTheme", "Invalid style")
                        return@XmlRenderThemeMenuCallback null
                    }

                    val categories: MutableSet<String> = renderThemeStyleLayer.categories

                    for (overlay in renderThemeStyleLayer.overlays) {
                        if (overlay.isEnabled) categories.addAll(overlay.categories)
                    }
                    categories
                })
            val mapFileInputStreams: Array<FileInputStream> =
                FileHelper.getOnDeviceMapFileInputStreams(context, mapFiles)
            val demFolder = getDemFolder(context)
            val hillsRenderConfig = if (demFolder != null) createHillShadingConfig(
                demFolder, AndroidGraphicFactory.INSTANCE
            ) else null
            val mapsForgeTileSource: MapsForgeTileSource =
                MapsForgeTileSource.createFromFileInputStream(
                    mapFileInputStreams,
                    theme,
                    "Elevate.xml",
                    MultiMapDataStore.DataPolicy.RETURN_ALL,
                    hillsRenderConfig
                )
            mapsForgeTileSource.setUserScaleFactor(0.5f)
            return MapsForgeTileProvider(SimpleRegisterReceiver(context), mapsForgeTileSource, null)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }


    }

    /**
     * Check if the app has MANAGE_EXTERNAL_STORAGE permission for direct file access
     */
    private fun hasManageExternalStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get DEM folder with HGT files for hill shading.
     * On Android 11+, if MANAGE_EXTERNAL_STORAGE permission is granted, we can access files directly.
     * Otherwise, we need to use DocumentFile to access external storage.
     */
    private fun getDemFolder(context: Context): File? {
        return try {
            // First try direct file access if we have MANAGE_EXTERNAL_STORAGE permission
            if (hasManageExternalStoragePermission(context)) {
                Log.d(TAG, "Has MANAGE_EXTERNAL_STORAGE permission, trying direct file access")
                val demFolder = File(getOsmdroidTilesFolder(), "dem")
                Log.d(TAG, "Checking DEM folder at: ${demFolder.absolutePath}")
                Log.d(TAG, "DEM folder exists: ${demFolder.exists()}, isDirectory: ${demFolder.isDirectory}")
                
                if (demFolder.exists() && demFolder.isDirectory) {
                    val allFiles = demFolder.listFiles()
                    Log.d(TAG, "Total files in DEM folder: ${allFiles?.size ?: 0}")
                    allFiles?.forEach { file ->
                        Log.d(TAG, "  File: ${file.name}, isFile: ${file.isFile}, extension: '${file.extension}'")
                    }
                    
                    // HGT files can have .hgt extension or no extension at all (e.g., N48E011.hgt or N48E011)
                    // They are typically named with latitude/longitude like N48E011
                    val hgtFiles = demFolder.listFiles { file ->
                        file.isFile && (
                            file.name.lowercase().endsWith(".hgt") ||
                            // HGT files without extension: match pattern like N48E011, S48W011, etc.
                            file.name.matches(Regex("^[NS]\\d{2}[EW]\\d{3}$", RegexOption.IGNORE_CASE))
                        )
                    }
                    if (hgtFiles != null && hgtFiles.isNotEmpty()) {
                        Log.i(
                            TAG,
                            "Found DEM folder with ${hgtFiles.size} HGT files: ${demFolder.absolutePath}"
                        )
                        hgtFiles.forEach { Log.d(TAG, "  HGT file: ${it.name}") }
                        return demFolder
                    } else {
                        Log.w(
                            TAG,
                            "DEM folder exists but contains no HGT files: ${demFolder.absolutePath}"
                        )
                    }
                } else {
                    Log.d(TAG, "No DEM folder found at: ${demFolder.absolutePath}")
                }
            } else {
                Log.d(TAG, "No MANAGE_EXTERNAL_STORAGE permission, falling back to DocumentFile access")
            }
            
            // Fallback: try DocumentFile access (for Android 11+ without MANAGE_EXTERNAL_STORAGE)
            val hgtDocumentFiles = FileHelper.getHgtFiles(context)
            Log.d(TAG, "Found ${hgtDocumentFiles.size} HGT files via DocumentFile")
            
            if (hgtDocumentFiles.isNotEmpty()) {
                // Copy HGT files to cache directory
                val demCacheDir = File(context.cacheDir, "dem_hgt")
                if (!demCacheDir.exists()) {
                    demCacheDir.mkdirs()
                }
                
                // Clean up old cached files that are no longer in the document files list
                val currentUris = hgtDocumentFiles.map { it.uri.toString() }.toSet()
                demCacheDir.listFiles()?.forEach { cachedFile ->
                    val uriInName = cachedFile.nameWithoutExtension
                    if (!currentUris.any { it.hashCode().toString() == uriInName }) {
                        cachedFile.delete()
                    }
                }
                
                // Copy HGT files to cache
                hgtDocumentFiles.forEach { docFile ->
                    val cacheFileName = docFile.uri.toString().hashCode().toString() + "_" + (docFile.name ?: "unknown")
                    val cachedFile = File(demCacheDir, cacheFileName)
                    
                    // Copy to cache if not exists or source is newer
                    if (!cachedFile.exists() || docFile.lastModified() > cachedFile.lastModified()) {
                        try {
                            context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                                cachedFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Log.d(TAG, "Copied HGT file: ${docFile.name}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to copy HGT file ${docFile.name}: ${e.message}")
                        }
                    }
                }
                
                // Check if we have any cached HGT files
                val cachedHgtFiles = demCacheDir.listFiles { file ->
                    file.isFile
                }
                if (cachedHgtFiles != null && cachedHgtFiles.isNotEmpty()) {
                    Log.i(TAG, "Using cached DEM folder with ${cachedHgtFiles.size} HGT files: ${demCacheDir.absolutePath}")
                    return demCacheDir
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing DEM folder", e)
            null
        }
    }


}
