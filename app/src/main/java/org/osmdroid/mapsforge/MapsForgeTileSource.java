package org.osmdroid.mapsforge;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.graphics.AndroidTileBitmap;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.labels.MapDataStoreLabelStore;
import org.mapsforge.map.layer.renderer.DirectRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;
import org.osmdroid.api.IMapView;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase;
import org.osmdroid.util.MapTileIndex;

import java.io.FileInputStream;

/**
 * Adapted from code from here: <a href="https://github.com/MKergall/osmbonuspack">...</a>, which is LGPL
 * <a href="http://www.salidasoftware.com/how-to-render-mapsforge-tiles-in-osmdroid/">...</a>
 *
 * @author Salida Software
 * Adapted from code found here : <a href="http://www.sieswerda.net/2012/08/15/upping-the-developer-friendliness/">...</a>
 */
public class MapsForgeTileSource extends BitmapTileSourceBase {

    // Reasonable defaults
    public static int MIN_ZOOM = 3;
    public static int MAX_ZOOM = 29;
    public static final int TILE_SIZE_PIXELS = 256;
    private final DisplayModel model = new DisplayModel();
    private final float scale = DisplayModel.getDefaultUserScaleFactor();
    private RenderThemeFuture theme;
    private DirectRenderer renderer;

    private MultiMapDataStore mapDatabase;

    private static Application application;

    /**
     * The reason this constructor is protected is that all parameters,
     * except file should be determined from the archive file. Therefore, a
     * factory method is necessary.
     *
     * @param cacheTileSourceName name used for the osmdroid tile cache
     * @param minZoom            the minimum zoom level this tile source can render
     * @param maxZoom            the maximum zoom level this tile source can render
     * @param tileSizePixels     the tile size in pixels
     * @param fileInputStream    streams of the .map files to read from; ownership is
     *                           transferred to the map data store, and they are closed
     *                           by {@link #dispose()} via {@link MultiMapDataStore#close()}
     * @param xmlRenderTheme     the theme to render tiles with
     * @param dataPolicy         how results from multiple map files are combined
     * @param hillsRenderConfig  the hillshading setup to be used (can be null)
     * @param language           preferred language for map labels as defined in ISO 639-1 or ISO 639-2 (can be null)
     */
    @SuppressLint("Recourse") // the streams are owned by the MapFiles and closed by dispose()
    protected MapsForgeTileSource(String cacheTileSourceName, int minZoom, int maxZoom, int tileSizePixels, FileInputStream[] fileInputStream, XmlRenderTheme xmlRenderTheme, MultiMapDataStore.DataPolicy dataPolicy, HillsRenderConfig hillsRenderConfig, final String language) {
        super(cacheTileSourceName, minZoom, maxZoom, tileSizePixels, ".png", "© OpenStreetMap contributors");

        mapDatabase = new MultiMapDataStore(dataPolicy);
        for (FileInputStream inputStream : fileInputStream)
            mapDatabase.addMapDataStore(new MapFile(inputStream, language), false, false);

        if (AndroidGraphicFactory.INSTANCE == null) {
            throw new RuntimeException("Must call MapsForgeTileSource.createInstance(context.getApplication()); once before MapsForgeTileSource.createFromFileInputStream().");
        }

        if (xmlRenderTheme == null)
            xmlRenderTheme = MapsforgeThemes.OSMARENDER;
        //we the passed in theme is different that the existing one, or the theme is currently null, create it
        theme = new RenderThemeFuture(AndroidGraphicFactory.INSTANCE, xmlRenderTheme, model);
        //super important!! without the following line, all rendering activities will block until the theme is created.
        new Thread(theme).start();
        // mapsforge 0.30.0: DirectRenderer requires a MapDataStoreLabelStore for deterministic label rendering
        renderer = new DirectRenderer(mapDatabase, AndroidGraphicFactory.INSTANCE,
                new MapDataStoreLabelStore(mapDatabase, theme, scale, model, AndroidGraphicFactory.INSTANCE),
                true, false, hillsRenderConfig);

        Log.d(IMapView.LOGTAG, "min=" + minZoom + " max=" + maxZoom + " tilesize=" + tileSizePixels);
    }

    /**
     * Creates a new MapsForgeTileSource from FileInputStream[].
     * <p></p>
     * Parameters minZoom and maxZoom are obtained from the
     * database. If they cannot be obtained from the DB, the default values as
     * defined by this class are used, which is zoom = 3-20
     *
     * @param fileInputStream   streams of the .map files to read from
     * @param theme              this can be null, in which case the default theme will be used
     * @param themeName          when using a custom theme, this sets up the osmdroid caching correctly
     * @param dataPolicy         use this to override the default, which is "RETURN_ALL"
     * @param hillsRenderConfig  the hillshading setup to be used (can be null)
     * @return the tile source
     */
    public static MapsForgeTileSource createFromFileInputStream(FileInputStream[] fileInputStream, XmlRenderTheme theme, String themeName, MultiMapDataStore.DataPolicy dataPolicy, HillsRenderConfig hillsRenderConfig) {
        return new MapsForgeTileSource(themeName, MIN_ZOOM, MAX_ZOOM, TILE_SIZE_PIXELS, fileInputStream, theme, dataPolicy, hillsRenderConfig, null);
    }

    //The synchronized here is VERY important.  If missing, the mapDatabase read gets corrupted by multiple threads reading the file at once.
    public synchronized Drawable renderTile(final long pMapTileIndex) {

        Tile tile = new Tile(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), (byte) MapTileIndex.getZoom(pMapTileIndex), 256);
        model.setFixedTileSize(256);

        if (mapDatabase == null)
            return null;
        try {
            //Draw the tile
            RendererJob mapGeneratorJob = new RendererJob(tile, mapDatabase, theme, model, scale, false, false);
            AndroidTileBitmap bmp = (AndroidTileBitmap) renderer.executeJob(mapGeneratorJob);
            if (bmp != null && application != null)
                return new BitmapDrawable(application.getResources(), AndroidGraphicFactory.getBitmap(bmp));
        } catch (Exception ex) {
            Log.d(IMapView.LOGTAG, "###################### Mapsforge tile generation failed", ex);
        }
        return null;
    }

    public static void createInstance(Application app) {
        AndroidGraphicFactory.createInstance(app);
        application = app;
    }


    public void dispose() {
        // Idempotent: MapView.onDetach() invokes MapsForgeTileProvider.detach() twice
        // (via the overlay manager and directly), so dispose() may be called again
        // after the fields have already been nulled.
        if (theme != null) {
            theme.decrementRefCount();
            theme = null;
        }
        renderer = null;
        if (mapDatabase != null)
            mapDatabase.close();
        mapDatabase = null;
    }

    /**
     * @since 6.0.3
     */
    public void addTileRefresher(DirectRenderer.TileRefresher pDirectTileRefresher) {
        if (pDirectTileRefresher != null) {
            renderer.addTileRefresher(pDirectTileRefresher);
        }
    }

    // for example a scaleFactor of .6F
    public void setUserScaleFactor(float scaleFactor){
        model.setUserScaleFactor(scaleFactor);
    }
}
