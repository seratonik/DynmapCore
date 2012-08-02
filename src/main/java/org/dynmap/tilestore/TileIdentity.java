package org.dynmap.tilestore;

/**
 * Tile identifier for tile storage
 */
public interface TileIdentity {
    /**
     * Get tile URL for given tile identity
     * @return URL for tile (beyond base tile URL)
     * @throws TileStoreException on error
     */
    public String getURL();
}
