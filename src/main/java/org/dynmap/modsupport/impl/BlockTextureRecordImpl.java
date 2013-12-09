package org.dynmap.modsupport.impl;

import java.util.ArrayList;
import java.util.Arrays;

import org.dynmap.hdmap.TexturePack;
import org.dynmap.modsupport.BlockSide;
import org.dynmap.modsupport.BlockTextureRecord;
import org.dynmap.modsupport.TextureFile;
import org.dynmap.modsupport.TextureModifier;
import org.dynmap.modsupport.TransparencyMode;

public class BlockTextureRecordImpl implements BlockTextureRecord {
    private int[] ids = new int[0];
    private int metaMask = -1;
    private TransparencyMode transmode = TransparencyMode.OPAQUE;
    
    private static class TexturePatch {
        private String txtFileID;
        private int txtIndex;
        private TextureModifier txtMod;
    }
    
    private ArrayList<TexturePatch> txtPatches = new ArrayList<TexturePatch>();
    
    private static final int[] patchBySideOrdinal = {
        1<<1,     // FACE_0
        1<<4,     // FACE_1
        1<<2,     // FACE_2
        1<<5,     // FACE_3
        1<<0,     // FACE_4
        1<<3,     // FACE_5
        1<<1,     // BOTTOM
        1<<4,     // TOP
        1<<2,     // NORTH
        1<<5,     // SOUTH
        1<<0,     // WEST
        1<<3,     // EAST
        1<<1,     // Y_MINUS
        1<<4,     // Y_PLUS
        1<<2,     // Z_MINUS
        1<<5,     // Z_PLUS
        1<<0,     // X_MINUS
        1<<3,     // X_PLUS
        (1<<2) | (1<<5) | (1<<0) | (1<<3),   // ALLSIDES
        (1<<0) | (1<<1) | (1<<2) | (1<<3) | (1<<4) | (1<<5) // ALLFACES
    };
    
    public static final int COLORMOD_GRASSTONED = 1;
    public static final int COLORMOD_FOLIAGETONED = 2;
    public static final int COLORMOD_WATERTONED = 3;
    public static final int COLORMOD_ROT90 = 4;
    public static final int COLORMOD_ROT180 = 5;
    public static final int COLORMOD_ROT270 = 6;
    public static final int COLORMOD_FLIPHORIZ = 7;
    public static final int COLORMOD_SHIFTDOWNHALF = 8;
    public static final int COLORMOD_SHIFTDOWNHALFANDFLIPHORIZ = 9;
    public static final int COLORMOD_INCLINEDTORCH = 10;
    public static final int COLORMOD_GRASSSIDE = 11;
    public static final int COLORMOD_CLEARINSIDE = 12;
    public static final int COLORMOD_PINETONED = 13;
    public static final int COLORMOD_BIRCHTONED = 14;
    public static final int COLORMOD_LILYTONED = 15;
    //private static final int COLORMOD_OLD_WATERSHADED = 16;
    public static final int COLORMOD_MULTTONED = 17;   /* Toned with colorMult or custColorMult - not biome-style */
    public static final int COLORMOD_GRASSTONED270 = 18; // GRASSTONED + ROT270
    public static final int COLORMOD_FOLIAGETONED270 = 19; // FOLIAGETONED + ROT270
    public static final int COLORMOD_WATERTONED270 = 20; // WATERTONED + ROT270 
    public static final int COLORMOD_MULTTONED_CLEARINSIDE = 21; // MULTTONED + CLEARINSIDE
    public static final int COLORMOD_FOLIAGEMULTTONED = 22; // FOLIAGETONED + colorMult or custColorMult

    private static final int modValueByModifierOrd[] = {
        0,                                  // NONE
        TexturePack.COLORMOD_GRASSTONED,    // GRASSTONED
        TexturePack.COLORMOD_FOLIAGETONED,  // FOLIAGETONED
        TexturePack.COLORMOD_WATERTONED,    // WATERTONED
        TexturePack.COLORMOD_ROT90,         // ROT90
        TexturePack.COLORMOD_ROT180,        // ROT180
        TexturePack.COLORMOD_ROT270,        // ROT270
        TexturePack.COLORMOD_FLIPHORIZ,     // FLIPHORIZ
        TexturePack.COLORMOD_SHIFTDOWNHALF, // SHIFTDOWNHALF
        TexturePack.COLORMOD_SHIFTDOWNHALFANDFLIPHORIZ, // SHIFTDOWNHALFANDFLIPHORIZ
        TexturePack.COLORMOD_INCLINEDTORCH, // INCLINEDTORCH
        TexturePack.COLORMOD_GRASSSIDE,     // GRASSSIDE
        TexturePack.COLORMOD_CLEARINSIDE,   // CLEARINSIDE
        TexturePack.COLORMOD_PINETONED,     // PINETONED
        TexturePack.COLORMOD_BIRCHTONED,    // BIRCHTONED
        TexturePack.COLORMOD_LILYTONED,     // LILYTONED
        TexturePack.COLORMOD_MULTTONED,     // MULTTONED
        TexturePack.COLORMOD_GRASSTONED270, // GRASSTONED270
        TexturePack.COLORMOD_FOLIAGETONED270,   // FOLIAGETONED270
        TexturePack.COLORMOD_WATERTONED270, // WATERTONED270
        TexturePack.COLORMOD_MULTTONED_CLEARINSIDE, // MULTTONED_CLEARINSIDE
        TexturePack.COLORMOD_FOLIAGEMULTTONED   // FOLIAGEMULTTONED
    };

    public BlockTextureRecordImpl(int blkid) {
        addBlockID(blkid);
        for (int i = 0; i < 6; i++) {
            txtPatches.add(null);
        }
    }
    
    /**
     * Add block ID to mapping (in case multiple block IDs use same texture mapping)
     * @param blockID - block ID
     */
    @Override
    public void addBlockID(int blockID) {
        if ((blockID > 0) && (blockID < 4096)) {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == blockID) {
                    return;
                }
            }
            ids = Arrays.copyOf(ids, ids.length+1);
            ids[ids.length-1] = blockID;
        }
    }

    /**
     * Get block IDs
     * @return configured IDs
     */
    @Override
    public int[] getBlockIDs() {
        return ids;
    }

    /**
     * Set metadata value : default is for all values (data=*).  Setting other values will match only the values that are set
     * @param data - value to match (-1 = all, 0-15 is meta value to match)
     */
    @Override
    public void setMetaValue(int data) {
        if (data < 0) { // Setting to all
            metaMask = METAMASK_ALL;
        }
        else if (data < 16) {
            if (metaMask == METAMASK_ALL) {
                metaMask = 0;
            }
            metaMask |= (1 << data);
        }
    }

    /**
     * Get matching metadata value mask
     * @return matching metadata mask: bit N is set if given metadata value matches
     */
    @Override
    public int getMetaValueMask() {
        return metaMask;
    }

    /**
     * Set transparency mode for block
     * @param mode - transparency mode
     */
    @Override
    public void setTransparencyMode(TransparencyMode mode) {
        transmode = mode;
    }

    /**
     * Get transparency mode for block
     * @return transparency mode
     */
    @Override
    public TransparencyMode getTransparencyMode() {
        return transmode;
    }
    /**
     * Set side texture (standard cube block model)
     * @param txtFileID - texture file ID (first texture in file used)
     * @param side - side to apply texture to
     */
    @Override
    public void setSideTexture(String txtFileID, BlockSide side) {
        setSideTexture(txtFileID, 0, TextureModifier.NONE, side);
    }
    /**
     * Set side texture (standard cube block model)
     * @param txtFile - texture file (first texture in file used)
     * @param side - side to apply texture to
     */
    @Override
    public void setSideTexture(TextureFile txtFile, BlockSide side) {
        setSideTexture(txtFile.getTextureID(), 0, TextureModifier.NONE, side);
    }
    /**
     * Set side texture (standard cube block model) with given texture index
     * @param txtFileID - texture file ID
     * @param txtIndex - texture index
     * @param side - side to apply texture to
     */
    @Override
    public void setSideTexture(String txtFileID, int txtIndex, BlockSide side) {
        setSideTexture(txtFileID, txtIndex, TextureModifier.NONE, side);
    }
    /**
     * Set side texture (standard cube block model) with givcen texture index
     * @param txtFile - texture file
     * @param txtIndex - texture index
     * @param side - side to apply texture to
     */
    @Override
    public void setSideTexture(TextureFile txtFile, int txtIndex, BlockSide side) {
        setSideTexture(txtFile.getTextureID(), txtIndex, TextureModifier.NONE, side);
    }
    /**
     * Set side texture (standard cube block model) with texture modifier
     * @param txtFileID - texture file ID (first texture in file used)
     * @param modifier - texture modifier
     * @param side - side to apply texture to
     */
    @Override
    public void setSideTexture(String txtFileID, TextureModifier modifier, BlockSide side) {
        setSideTexture(txtFileID, 0, modifier, side);
    }
    /**
     * Set side texture (standard cube block model) with modifier
     * @param txtFile - texture file (first texture in file used)
     * @param modifier - texture modifier
     * @param side - side to apply texture to
     */
    @Override
    public void setSideTexture(TextureFile txtFile, TextureModifier modifier, BlockSide side) {
        setSideTexture(txtFile.getTextureID(), 0, modifier, side);
    }
    /**
     * Set side texture (standard cube block model) with texture modifier and texture index
     * @param txtFileID - texture file ID
     * @param txtIndex - texture index
     * @param modifier - texture modifier
     * @param side - side to apply texture to
     */
    @Override
    public void setSideTexture(String txtFileID, int txtIndex, TextureModifier modifier, BlockSide side) {
        int patchflags = patchBySideOrdinal[side.ordinal()];    // Look up patches to apply to
        for (int i = 0; i < 6; i++) {
            if ((patchflags & (1 << i)) != 0) {
                setPatchTexture(txtFileID, txtIndex, modifier, i);
            }
        }
    }
    /**
     * Set side texture (standard cube block model) with texture modifier and texture index
     * @param txtFile - texture file
     * @param txtIndex - texture index
     * @param modifier - texture modifier
     * @param side - side to apply texture to
     */
    @Override
    public void setSideTexture(TextureFile txtFile, int txtIndex, TextureModifier modifier, BlockSide side) {
        setSideTexture(txtFile.getTextureID(), txtIndex, modifier, side);
    }
    /**
     * Set patch texture
     * @param txtFileID - texture file ID (first texture in file used)
     * @param patchIndex - patch index to apply texture to
     */
    @Override
    public void setPatchTexture(String txtFileID, int patchIndex) {
        setPatchTexture(txtFileID, 0, TextureModifier.NONE, patchIndex);
    }
    /**
     * Set patch texture
     * @param txtFile - texture file (first texture in file used)
     * @param patchIndex - patch index to apply texture to
     */
    @Override
    public void setPatchTexture(TextureFile txtFile, int patchIndex) {
        setPatchTexture(txtFile.getTextureID(), 0, TextureModifier.NONE, patchIndex);
    }
    /**
     * Set patch texture with given texture index
     * @param txtFileID - texture file ID
     * @param txtIndex - texture index
     * @param patchIndex - patch index to apply texture to
     */
    @Override
    public void setPatchTexture(String txtFileID, int txtIndex, int patchIndex) {
        setPatchTexture(txtFileID, txtIndex, TextureModifier.NONE, patchIndex);
    }
    /**
     * Set patch texture with givcen texture index
     * @param txtFile - texture file
     * @param txtIndex - texture index
     * @param patchIndex - patch index to apply texture to
     */
    @Override
    public void setPatchTexture(TextureFile txtFile, int txtIndex, int patchIndex) {
        setPatchTexture(txtFile.getTextureID(), txtIndex, TextureModifier.NONE, patchIndex);
    }
    /**
     * Set patch texture with texture modifier
     * @param txtFileID - texture file ID (first texture in file used)
     * @param modifier - texture modifier
     * @param patchIndex - patch index to apply texture to
     */
    @Override
    public void setPatchTexture(String txtFileID, TextureModifier modifier, int patchIndex) {
        setPatchTexture(txtFileID, 0, modifier, patchIndex);
    }
    /**
     * Set patch texture with modifier
     * @param txtFile - texture file (first texture in file used)
     * @param modifier - texture modifier
     * @param patchIndex - patch index to apply texture to
     */
    @Override
    public void setPatchTexture(TextureFile txtFile, TextureModifier modifier, int patchIndex) {
        setPatchTexture(txtFile.getTextureID(), 0, modifier, patchIndex);
    }
    /**
     * Set patch texture with texture modifier and texture index
     * @param txtFileID - texture file ID
     * @param txtIndex - texture index
     * @param modifier - texture modifier
     * @param patchIndex - patch index to apply texture to
     */
    @Override
    public void setPatchTexture(String txtFileID, int txtIndex, TextureModifier modifier, int patchIndex) {
        while (txtPatches.size() <= patchIndex) {
            txtPatches.add(null);
        }
        TexturePatch tp = new TexturePatch();
        tp.txtFileID = txtFileID;
        tp.txtIndex = txtIndex;
        tp.txtMod = modifier;
        txtPatches.set(patchIndex, tp);
    }
    /**
     * Set patch texture with texture modifier and texture index
     * @param txtFile - texture file
     * @param txtIndex - texture index
     * @param modifier - texture modifier
     * @param patchIndex - patch index to apply texture to
     */
    @Override
    public void setPatchTexture(TextureFile txtFile, int txtIndex, TextureModifier modifier, int patchIndex) {
        setPatchTexture(txtFile.getTextureID(), txtIndex, modifier, patchIndex);
    }
    
    private TexturePatch getPatchBySide(BlockSide side) {
        int patchflags = patchBySideOrdinal[side.ordinal()];    // Look up patches to apply to
        for (int i = 0; i < 6; i++) {
            if ((patchflags & (1 << i)) != 0) {
                return this.txtPatches.get(i);
            }
        }
        return null;
    }
    /**
     * Get texture ID for given side
     * @param side - side
     * @return texture ID
     */
    @Override
    public String getSideTextureID(BlockSide side) {
        TexturePatch tp = getPatchBySide(side);
        if (tp != null)
            return tp.txtFileID;
        return null;
    }
    /**
     * Get texture modifier for given side
     * @param side - side
     * @return texture modifier
     */
    @Override
    public TextureModifier getSideTextureModifier(BlockSide side) {
        TexturePatch tp = getPatchBySide(side);
        if (tp != null)
            return tp.txtMod;
        return TextureModifier.NONE;
    }
    /**
     * Get texture index for given side
     * @param side - side
     * @return texture index
     */
    @Override
    public int getSideTextureIndex(BlockSide side) {
        TexturePatch tp = getPatchBySide(side);
        if (tp != null)
            return tp.txtIndex;
        return 0;
    }
    /**
     * Get texture ID for given patch index
     * @param patchIndex - patch index
     * @return texture ID
     */
    @Override
    public String getPatchTextureID(int patchIndex) {
        TexturePatch tp = txtPatches.get(patchIndex);
        if (tp != null)
            return tp.txtFileID;
        return null;
    }
    /**
     * Get texture modifier for given patch index
     * @param patchIndex - patch index
     * @return texture modifier
     */
    @Override
    public TextureModifier getPatchTextureModifier(int patchIndex) {
        TexturePatch tp = txtPatches.get(patchIndex);
        if (tp != null)
            return tp.txtMod;
        return TextureModifier.NONE;
    }
    /**
     * Get texture index for given patch index
     * @param patchIndex - patch index
     * @return texture index
     */
    @Override
    public int getPatchTextureIndex(int patchIndex) {
        TexturePatch tp = txtPatches.get(patchIndex);
        if (tp != null)
            return tp.txtIndex;
        return 0;
    }
    
    public String getLine() {
        if (ids.length == 0) {
            return null;
        }
        String s = "block:";
        // Add ids
        for (int i = 0; i < ids.length; i++) {
            if (i == 0) {
                s += "id=" + ids[i];
            }
            else {
                s += ",id=" + ids[i];
            }
        }
        // Add meta
        if (this.metaMask == METAMASK_ALL) {
            s += ",data=*";
        }
        else {
            for (int i = 0; i < 16; i++) {
                if ((metaMask & (1 << i)) != 0) {
                    s += ",data=" + i;
                }
            }
        }
        for (int i = 0; i < txtPatches.size(); i++) {
            TexturePatch tp = txtPatches.get(i);
            if (tp == null) continue;
            int idx = (modValueByModifierOrd[tp.txtMod.ordinal()] * 1000) + tp.txtIndex;
            s += ",patch" + i + "=" + idx + ":" + tp.txtFileID;
        }
        switch (this.transmode) {
            case TRANSPARENT:
                s += ",transparency=TRANSPARENT";
                break;
            case SEMITRANSPARENT:
                s += ",transparency=SEMITRANSPARENT";
                break;
            default:
                break;
        }
        
        return s;
    }
}
