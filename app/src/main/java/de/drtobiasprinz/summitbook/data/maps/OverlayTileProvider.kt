package de.drtobiasprinz.summitbook.data.maps

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.modules.IArchiveFile
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.modules.MapTileFileStorageProviderBase
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.tileprovider.util.StreamUtils
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * Lean tile provider for a single mbtiles archive, used for map overlay
 * layers and heatmaps.
 *
 * Unlike [org.osmdroid.tileprovider.modules.OfflineTileProvider] it runs a
 * small fixed loader pool instead of the default `tileFileSystemThreads` (8)
 * so that many layers do not decode dozens of tiles simultaneously and trip
 * blocking GCs. Missing tiles - beyond the zoom range actually stored in the
 * archive, or past the zoom 18 that [FileBasedTileSource] caps at - are
 * approximated by upscaling the matching quadrant of a lower-zoom tile, so
 * the layer stays visible when zooming in instead of vanishing. The
 * approximation runs on its own small pool and only kicks in on a miss.
 *
 * The mbtiles SQLite database is opened in the constructor; call it from a
 * background dispatcher.
 */
class OverlayTileProvider private constructor(
    tileSource: ITileSource,
    registerReceiver: IRegisterReceiver,
    archives: List<IArchiveFile>
) : MapTileProviderArray(tileSource, registerReceiver) {

    init {
        val archiveProvider = ArchiveProvider(registerReceiver, tileSource, archives)
        mTileProviderList.add(archiveProvider)
        val approximater = MapTileApproximater(LOADER_THREADS, PENDING_QUEUE_SIZE)
        approximater.addProvider(archiveProvider)
        mTileProviderList.add(approximater)
        setUseDataConnection(false)
    }

    companion object {
        private const val TAG = "OverlayTileProvider"
        private const val LOADER_THREADS = 2
        private const val PENDING_QUEUE_SIZE = 20

        /**
         * Opens [file] and wraps it in a ready-to-attach [TilesOverlay].
         *
         * [mapView]'s tile request complete handler is registered on the new
         * provider so the map is invalidated as soon as one of the overlay's
         * tiles finishes decoding - without it overlay tiles only appear on
         * the next unrelated repaint, which looks like slow loading.
         *
         * Opens the SQLite database synchronously; call from a background
         * dispatcher.
         */
        fun createTilesOverlay(context: Context, file: File, mapView: MapView?): TilesOverlay {
            val archive = ArchiveFileFactory.getArchiveFile(file)
                ?: throw IllegalArgumentException("No archive provider registered for ${file.name}")
            val provider = OverlayTileProvider(
                FileBasedTileSource.getSource(file.name),
                SimpleRegisterReceiver(context),
                listOf(archive)
            )
            mapView?.tileRequestCompleteHandler?.let { handler ->
                provider.tileRequestCompleteHandlers.add(handler)
            }
            return TilesOverlay(provider, context).apply {
                loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                loadingLineColor = android.graphics.Color.TRANSPARENT
            }
        }
    }

    /**
     * Archive module mirroring
     * [org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider], but with
     * an explicit small thread pool and pending queue.
     */
    private class ArchiveProvider(
        registerReceiver: IRegisterReceiver,
        tileSource: ITileSource,
        private val archiveFiles: List<IArchiveFile>
    ) : MapTileFileStorageProviderBase(registerReceiver, LOADER_THREADS, PENDING_QUEUE_SIZE) {

        private val tileSource = AtomicReference<ITileSource>()

        init {
            this.tileSource.set(tileSource)
        }

        override fun getUsesDataConnection(): Boolean = false

        override fun getName(): String = "Mbtiles Overlay Provider"

        override fun getThreadGroupName(): String = "mbtilesoverlay"

        override fun getTileLoader(): MapTileModuleProviderBase.TileLoader = TileLoader()

        override fun getMinimumZoomLevel(): Int =
            tileSource.get()?.minimumZoomLevel ?: 0

        override fun getMaximumZoomLevel(): Int =
            tileSource.get()?.maximumZoomLevel ?: TileSystem.getMaximumZoomLevel()

        override fun setTileSource(tileSource: ITileSource) {
            this.tileSource.set(tileSource)
        }

        override fun detach() {
            archiveFiles.forEach { it.close() }
            super.detach()
        }

        private fun getInputStream(mapTileIndex: Long): InputStream? {
            archiveFiles.forEach { archive ->
                archive.getInputStream(tileSource.get(), mapTileIndex)?.let { return it }
            }
            return null
        }

        private inner class TileLoader : MapTileModuleProviderBase.TileLoader() {
            override fun loadTile(mapTileIndex: Long): Drawable? {
                val source = tileSource.get() ?: return null
                var inputStream: InputStream? = null
                return try {
                    inputStream = getInputStream(mapTileIndex)
                    inputStream?.let { source.getDrawable(it) }
                } catch (t: Throwable) {
                    Log.w(TAG, "Error loading tile from archive", t)
                    null
                } finally {
                    inputStream?.let { StreamUtils.closeStream(it) }
                }
            }
        }
    }
}
