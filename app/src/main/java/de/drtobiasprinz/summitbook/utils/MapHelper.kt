package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import de.drtobiasprinz.summitbook.ui.utils.MapProvider
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.getOsmdroidTilesFolder
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


object MapHelper {
    const val TAG = "MapHelper"
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
            val demFolder = getDemFolder()
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

    private fun getDemFolder(): File? {
        return try {
            val demFolder = File(getOsmdroidTilesFolder(), "dem")
            if (demFolder.exists() && demFolder.isDirectory) {
                val hgtFiles = demFolder.listFiles { file ->
                    file.extension.equals("hgt", ignoreCase = true)
                }
                if (hgtFiles != null && hgtFiles.isNotEmpty()) {
                    Log.i(
                        TAG,
                        "Found DEM folder with ${hgtFiles.size} HGT files: ${demFolder.absolutePath}"
                    )
                    demFolder
                } else {
                    Log.w(
                        TAG,
                        "DEM folder exists but contains no HGT files: ${demFolder.absolutePath}"
                    )
                    null
                }
            } else {
                Log.d(TAG, "No DEM folder found at: ${demFolder.absolutePath}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing DEM folder", e)
            null
        }
    }


}
