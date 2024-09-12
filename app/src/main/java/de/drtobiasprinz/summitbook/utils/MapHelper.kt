package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import de.drtobiasprinz.summitbook.ui.utils.MapProvider
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

    fun getOfflineMapProvider(
        context: Context,
        mapFiles: List<DocumentFile>,
        item: MapProvider
    ): MapsForgeTileProvider {
        var theme: XmlRenderTheme? = null
        try {
            theme = AssetsRenderTheme(
                context.assets,
                "rendertheme/",
                "Elevate.xml",
                XmlRenderThemeMenuCallback { style ->
                    val renderThemeStyleLayer = style.getLayer(item.offlineStyle)
                    if (renderThemeStyleLayer == null) {
                        Log.w("AssetsRenderTheme", "Invalid style")
                        return@XmlRenderThemeMenuCallback null
                    }

                    val categories: MutableSet<String> = renderThemeStyleLayer.categories

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