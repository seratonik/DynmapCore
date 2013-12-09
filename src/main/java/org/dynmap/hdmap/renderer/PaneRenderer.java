package org.dynmap.hdmap.renderer;

import java.util.Map;

import org.dynmap.hdmap.TexturePack.BlockTransparency;
import org.dynmap.hdmap.TexturePack.HDTextureMap;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;
import org.dynmap.renderer.RenderPatchFactory.SideVisible;

/*
 * Glass pane / iron fence renderer
 */
public class PaneRenderer extends CustomRenderer {
    private static final int TEXTURE_FACE = 0;
    private static final int TEXTURE_EDGE = 1;

    private static final int SIDE_XP = 0x4;
    private static final int SIDE_XN = 0x1;
    private static final int SIDE_ZP = 0x8;
    private static final int SIDE_ZN = 0x2;

    private static final int TYPEID_GLASS = 20; // Block ID of glass blocks
    
    // Meshes, indexed by connection combination (bit 0=X-, bit 1=Z-, bit 2=X+, bit 3=Z+)
    private RenderPatch[][] meshes = new RenderPatch[16][];
    
    @Override
    public boolean initializeRenderer(RenderPatchFactory rpf, int blkid, int blockdatamask, Map<String,String> custparm) {
        if(!super.initializeRenderer(rpf, blkid, blockdatamask, custparm))
            return false;
        buildPatches(rpf);
        return true;
    }

    private void buildPatches(RenderPatchFactory rpf) {
        RenderPatch VertX05 = rpf.getPatch(0.5, 0.0, 1.0, 0.5, 0.0, 0.0, 0.5, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, SideVisible.BOTH, TEXTURE_FACE);
        RenderPatch VertX05_90 = rpf.getRotatedPatch(VertX05, 0, 90, 0, TEXTURE_FACE);
        RenderPatch VertX05_180 = rpf.getRotatedPatch(VertX05, 0, 180, 0, TEXTURE_FACE);
        RenderPatch VertX05_270 = rpf.getRotatedPatch(VertX05, 0, 270, 0, TEXTURE_FACE);
        RenderPatch VertX05Left = rpf.getPatch(0.5, 0.0, 1.0, 0.5, 0.0, 0.0, 0.5, 1.0, 1.0, 0.0, 0.5, 0.0, 1.0, SideVisible.BOTH, TEXTURE_FACE);
        RenderPatch VertX05Left_90 = rpf.getRotatedPatch(VertX05Left, 0, 90, 0, TEXTURE_FACE);
        RenderPatch VertX05Left_180 = rpf.getRotatedPatch(VertX05Left, 0, 180, 0, TEXTURE_FACE);
        RenderPatch VertX05Left_270 = rpf.getRotatedPatch(VertX05Left, 0, 270, 0, TEXTURE_FACE);
        RenderPatch VertX05Strip = rpf.getPatch(0.5, 0.0, 1.0, 0.5, 0.0, 0.0, 0.5, 1.0, 1.0, 0.4375, 0.5625, 0.0, 1.0, SideVisible.BOTH, TEXTURE_EDGE);
        RenderPatch VertX05Strip_90 = rpf.getRotatedPatch(VertX05Strip, 0, 90, 0, TEXTURE_EDGE);
        RenderPatch VertX05Strip_180 = rpf.getRotatedPatch(VertX05Strip, 0, 180, 0, TEXTURE_EDGE);
        RenderPatch VertX05Strip_270 = rpf.getRotatedPatch(VertX05Strip, 0, 270, 0, TEXTURE_EDGE);
        RenderPatch HorizY100ZTopStrip = rpf.getPatch(0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.4375, 0.5625, 0.0, 1.0, SideVisible.BOTH, TEXTURE_EDGE);
        RenderPatch HorizY100ZTopStrip_90 = rpf.getRotatedPatch(HorizY100ZTopStrip, 0, 90, 0, TEXTURE_EDGE);
        RenderPatch HorizY100ZTopStrip_180 = rpf.getRotatedPatch(HorizY100ZTopStrip, 0, 180, 0, TEXTURE_EDGE);
        RenderPatch HorizY100ZTopStrip_270 = rpf.getRotatedPatch(HorizY100ZTopStrip, 0, 270, 0, TEXTURE_EDGE);
        RenderPatch HorizY100ZTopStripTop = rpf.getPatch(0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.4375, 0.5625, 0.5, 1.0, SideVisible.BOTH, TEXTURE_EDGE);
        RenderPatch HorizY100ZTopStripTop_90 = rpf.getRotatedPatch(HorizY100ZTopStripTop, 0, 90, 0, TEXTURE_EDGE);
        RenderPatch HorizY100ZTopStripTop_180 = rpf.getRotatedPatch(HorizY100ZTopStripTop, 0, 180, 0, TEXTURE_EDGE);
        RenderPatch HorizY100ZTopStripTop_270 = rpf.getRotatedPatch(HorizY100ZTopStripTop, 0, 270, 0, TEXTURE_EDGE);

        meshes[0] = meshes[15] = new RenderPatch[] { VertX05, VertX05_90, HorizY100ZTopStrip, HorizY100ZTopStrip_90 };
        meshes[1] = new RenderPatch[] { VertX05Left_90, HorizY100ZTopStripTop_90, VertX05Strip };
        meshes[2] = new RenderPatch[] { VertX05Left_180, HorizY100ZTopStripTop_180, VertX05Strip_90 };
        meshes[3] = new RenderPatch[] { VertX05Left_90, HorizY100ZTopStripTop_90, VertX05Left_180, HorizY100ZTopStripTop_180 };
        meshes[4] = new RenderPatch[] { VertX05Left_270, HorizY100ZTopStripTop_270, VertX05Strip_180 };
        meshes[5] = new RenderPatch[] { VertX05_90, HorizY100ZTopStrip_90 };
        meshes[6] = new RenderPatch[] { VertX05Left_180, HorizY100ZTopStripTop_180, VertX05Left_270, HorizY100ZTopStripTop_270 };
        meshes[7] = new RenderPatch[] { VertX05_90, HorizY100ZTopStrip_90, VertX05Left_180, HorizY100ZTopStripTop_180 };
        meshes[8] = new RenderPatch[] { VertX05Left, HorizY100ZTopStripTop, VertX05Strip_270 };
        meshes[9] = new RenderPatch[] { VertX05Left, HorizY100ZTopStripTop, VertX05Left_90, HorizY100ZTopStripTop_90 };
        meshes[10] = new RenderPatch[] { VertX05, HorizY100ZTopStrip };
        meshes[11] = new RenderPatch[] { VertX05, HorizY100ZTopStrip, VertX05Left_90, HorizY100ZTopStripTop_90 };
        meshes[12] = new RenderPatch[] { VertX05Left_270, HorizY100ZTopStripTop_270, VertX05Left, HorizY100ZTopStripTop };
        meshes[13] = new RenderPatch[] { VertX05_270, HorizY100ZTopStrip_270, VertX05Left, HorizY100ZTopStripTop };
        meshes[14] = new RenderPatch[] { VertX05_180, HorizY100ZTopStrip_180, VertX05Left_270, HorizY100ZTopStripTop_270 };
    }
    @Override
    public int getMaximumTextureCount() {
        return 2;
    }
    
    @Override
    public RenderPatch[] getRenderPatchList(MapDataContext ctx) {
        /* Build connection map - check each axis */
        int blockdata = 0;
        int id;
        int typeid = ctx.getBlockTypeID();
        /* Check north */
        id = ctx.getBlockTypeIDAt(-1,  0,  0);
        if ((id == typeid) || (id == TYPEID_GLASS) || ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {
            blockdata |= SIDE_XN;
        }
        /* Look east */
        id = ctx.getBlockTypeIDAt(0,  0,  -1);
        if ((id == typeid) || (id == TYPEID_GLASS) || ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {
            blockdata |= SIDE_ZN;
        }
        /* Look south */
        id = ctx.getBlockTypeIDAt(1,  0,  0);
        if ((id == typeid) || (id == TYPEID_GLASS) || ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {
            blockdata |= SIDE_XP;
        }
        /* Look west */
        id = ctx.getBlockTypeIDAt(0,  0,  1);
        if ((id == typeid) || (id == TYPEID_GLASS) || ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {
            blockdata |= SIDE_ZP;
        }
        return meshes[blockdata];
    }    
}
