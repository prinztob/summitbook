package org.osmdroid.mapsforge;


import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.IFilesystemCache;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.MapTileIndex;

import java.util.Objects;

/**
 * This lets you hook up multiple MapsForge files, it will render to the screen the first
 * image that's available.
 * <p>
 * Adapted from code from here: <a href="https://github.com/MKergall/osmbonuspack">...</a>, which is LGPL
 * <a href="http://www.salidasoftware.com/how-to-render-mapsforge-tiles-in-osmdroid/">...</a>
 *
 * @author Salida Software
 * Adapted from code found here : <a href="http://www.sieswerda.net/2012/08/15/upping-the-developer-friendliness/">...</a>
 */
public class MapsForgeTileProvider extends MapTileProviderArray {
    IFilesystemCache tileWriter;

    public MapsForgeTileProvider(IRegisterReceiver pRegisterReceiver, MapsForgeTileSource pTileSource, IFilesystemCache cacheWriter) {
        super(pTileSource, pRegisterReceiver);

        final MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(
                pRegisterReceiver, pTileSource);
        mTileProviderList.add(fileSystemProvider);

        final MapTileFileArchiveProvider archiveProvider = new MapTileFileArchiveProvider(
                pRegisterReceiver, pTileSource);
        mTileProviderList.add(archiveProvider);


        tileWriter = Objects.requireNonNullElseGet(cacheWriter, SqlTileWriter::new);

        // Create the module provider; this class provides a TileLoader that
        // actually loads the tile from the map file.
        MapsForgeTileModuleProvider moduleProvider = new MapsForgeTileModuleProvider(pRegisterReceiver, (MapsForgeTileSource) getTileSource(), tileWriter);
        //this is detached by super


        // Add the module provider to the array of providers; mTileProviderList
        // is defined by the superclass.
        mTileProviderList.add(moduleProvider);

        // In mapsforge the tiles bitmap may need to be refreshed according to neighboring tiles' labels
        pTileSource.addTileRefresher(pTile -> {
            final long index = MapTileIndex.getTileIndex(pTile.zoomLevel, pTile.tileX, pTile.tileY);
            expireInMemoryCache(index);
        });
    }


    @Override
    public void detach() {
        if (tileWriter != null)
            tileWriter.onDetach();
        tileWriter = null;
        super.detach();
        // Dispose the tile source after the module providers are detached.
        // This closes the MapsForge map database, which closes the underlying
        // file input streams; otherwise they are only closed by the finalizer
        // (StrictMode LeakedClosableViolation).
        final ITileSource tileSource = getTileSource();
        if (tileSource instanceof MapsForgeTileSource)
            ((MapsForgeTileSource) tileSource).dispose();
    }

}
