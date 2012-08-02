package org.dynmap.tilestore;

import org.dynmap.DynmapWorld;
import org.dynmap.MapType;

/**
 * Generic interface to tile storage system
 */
public interface TileStorageDriver {
    public static long HASHCODE_NO_TILE = -1L;
    /**
     * Initialize overall tile storage system
     * @throws TileStoreException on error
     */
    public void initialize() throws TileStoreException;
    /**
     * Shutdown overall tile storage system
     * @throws TileStoreException on error
     */
    public void shutdown() throws TileStoreException;
    /**
     * Initialize tile storage for specific world and map
     * @param world - world
     * @param mapprefix - map prefix
     * @param bigworld - big world format map
     * @param fmt - format of tiles in map
     * @throws TileStoreException on error
     */
    public void initializeMap(DynmapWorld world, String mapprefix, boolean bigworld, MapType.ImageFormat fmt) throws TileStoreException;
    /**
     * Test if tile exists
     * @param tileid - tile identity
     * @return true if exists, false if does not
     * @throws TileStoreException on error
     */
    public boolean tileExists(TileIdentity tileid) throws TileStoreException;
    /**
     * Get hash data for tile
     * @param tileid - tile identity
     * @return hashcode if exists, HASHCODE_NO_TILE if does not
     * @throws TileStoreException on error
     */
    public long getTileHashCode(TileIdentity tileid) throws TileStoreException;
    /**
     * Test for hash code matc for tile
     * @param tileid - tile identity
     * @param hashcode - hashcode to test
     * @return true if tile exists and matches given hash, false otherwise
     * @throws TileStoreException on error
     */
    public boolean testTileHashCode(TileIdentity tileid, long hashcode) throws TileStoreException;
    /**
     * Get data for tile
     * @param tileid - tile identity
     * @return byte array of image, or null if no tile
     * @throws TileStoreException on error
     */
    public byte[] getTileData(TileIdentity tileid) throws TileStoreException;
    /**
     * Write data and hashcode for tile
     * @param tileid - tile identity
     * @param data - data for tile (null=delete tile)
     * @param hashcode - hashcode for tile (HASHCODE_NO_TILE for delete)
     * @throws TileStoreException on error
     */
    public void writeTileData(TileIdentity tileid, byte[] data, long hashcode) throws TileStoreException;
    /**
     * Start tile transaction (for read or write)
     * @param tileid - tile identity
     * @param writelock - if true, write lock; if false, read lock
     */
    public void startTileTransaction(TileIdentity tileid, boolean writelock) throws TileStoreException;
    /**
     * Complete tile transaction (for read or write)
     * @param tileid - tile identity
     * @param writelock - if true, write lock; if false, read lock
     * @throws TileStoreException on error
     */
    public void completeTileTransaction(TileIdentity tileid, boolean writelock) throws TileStoreException;
    /**
     * Parse tile URL to tile identity
     * @param url - URL starting after base tile URL
     * @return tile identity
     * @throws TileStoreException on error
     */
    public TileIdentity parseTileURL(String url) throws TileStoreException;
    /**
     * Get tile identity from parameters
     * @param world - world
     * @param mapprefix - map prefix
     * @param tx - tile X coordinate
     * @param ty - tile Y coordinate
     * @param zoomlevel - zoom level
     * @param daytile - if true, day tile in day/night map
     */
    public TileIdentity getTileIdentity(DynmapWorld world, String mapprefix, int tx, int ty, int zoomlevel, boolean daytile);
}
