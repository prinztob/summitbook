package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderTheme
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
import java.io.FileInputStream


object MapHelper {

    /* Get a tile provider for online maps */
    fun getOnlineMapProvider(tileSource: ITileSource, context: Context): MapTileProviderArray {
        val tileWriter = TileWriter()
        val registerReceiver = SimpleRegisterReceiver(context)
        val fileSystemProvider = MapTileFilesystemProvider(
            registerReceiver, tileSource
        )
        val networkAvailablityCheck = NetworkAvailabliltyCheck(context)
        val downloaderProvider = MapTileDownloader(
            tileSource, tileWriter, networkAvailablityCheck
        )
        val mapTileApprox = MapTileApproximater()
        return MapTileProviderArray(
            tileSource, registerReceiver, arrayOf(
                fileSystemProvider, mapTileApprox, downloaderProvider
            )
        )
    }

    /* Get a tile provider for offline maps */
    fun getOfflineMapProvider(
        context: Context,
        mapFiles: List<DocumentFile>,
        item: Int = 0
    ): MapsForgeTileProvider {
        var theme: XmlRenderTheme? = null
        try {
            theme = AssetsRenderTheme(
                context.assets,
                "rendertheme/",
                "Elevate.xml",
                XmlRenderThemeMenuCallback { style ->
                    val styleId: String = when (item) {
                        1 -> "elv-city"
                        2 -> "elv-cycling"
                        3 -> "elv-mtb"
                        else -> style.defaultValue
                    }
                    val renderThemeStyleLayer = style.getLayer(styleId)
                    if (renderThemeStyleLayer == null) {
                        Log.w("AssetsRenderTheme", "Invalid style")
                        return@XmlRenderThemeMenuCallback null
                    }

                    // First get the selected layer's categories that are enabled together
                    val categories: MutableSet<String> = renderThemeStyleLayer.categories

                    // Then add the selected layer's overlays that are enabled individually
                    // Here we use the style menu, but users can use their own preferences
                    for (overlay in renderThemeStyleLayer.getOverlays()) {
                        if (overlay.isEnabled) categories.addAll(overlay.categories)
                    }
                    categories
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val mapFileInputStreams: Array<FileInputStream> =
            FileHelper.getOnDeviceMapFileInputStreams(context, mapFiles)
        val mapsForgeTileSource: MapsForgeTileSource =
            MapsForgeTileSource.createFromFileInputStream(
                mapFileInputStreams,
                theme,
                "Elevate.xml"
            )
        mapsForgeTileSource.setUserScaleFactor(0.5f)
        return MapsForgeTileProvider(SimpleRegisterReceiver(context), mapsForgeTileSource, null)
    }

}