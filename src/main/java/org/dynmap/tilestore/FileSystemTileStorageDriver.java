package org.dynmap.tilestore;

import org.dynmap.DynmapCore;
import org.dynmap.DynmapWorld;
import org.dynmap.TileHashManager;
import org.dynmap.MapType.ImageFormat;

/**
 * Standard file system backed storage driver
 */
public class FileSystemTileStorageDriver implements TileStorageDriver {
    /* Tile hash manager */
    private TileHashManager hashman;

    private static class FileSystemTileIdentity implements TileIdentity {
        private DynmapWorld world;
        private String mapprefix;
        private int tx;
        private int ty;
        private boolean daytile;
        private int zoomout;
        private String url;
        
        private FileSystemTileIdentity(DynmapWorld w, String m, int x, int y, boolean day, int zoom) {
            world = w;
            mapprefix = m;
            tx = x;
            ty = y;
            daytile = day;
            zoomout = zoom;
            /* Build URL based on other settings */
            
        }
        
        public String getURL() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    /**
     * Constructor for storage driver
     */
    public FileSystemTileStorageDriver(final DynmapCore core) {
        hashman = new TileHashManager(core.getTilesFolder(), core.configuration.getBoolean("enabletilehash", true));
    }
    @Override
    public void initialize() throws TileStoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void shutdown() throws TileStoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void initializeMap(DynmapWorld world, String mapprefix,
            boolean bigworld, ImageFormat fmt) throws TileStoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean tileExists(TileIdentity tileid) throws TileStoreException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getTileHashCode(TileIdentity tileid) throws TileStoreException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean testTileHashCode(TileIdentity tileid, long hashcode)
            throws TileStoreException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public byte[] getTileData(TileIdentity tileid) throws TileStoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeTileData(TileIdentity tileid, byte[] data, long hashcode)
            throws TileStoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startTileTransaction(TileIdentity tileid, boolean writelock)
            throws TileStoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void completeTileTransaction(TileIdentity tileid, boolean writelock)
            throws TileStoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public TileIdentity parseTileURL(String url) throws TileStoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TileIdentity getTileIdentity(DynmapWorld world, String mapprefix,
            int tx, int ty, int zoomlevel, boolean daytile) {
        // TODO Auto-generated method stub
        return null;
    }

}
