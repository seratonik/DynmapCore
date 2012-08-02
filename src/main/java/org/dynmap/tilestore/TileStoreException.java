package org.dynmap.tilestore;

public class TileStoreException extends Exception {
    private static final long serialVersionUID = 1L;
    public TileStoreException(String msg) {
        super(msg);
    }
    public TileStoreException(String msg, Exception x) {
        super(msg, x);
    }
}
