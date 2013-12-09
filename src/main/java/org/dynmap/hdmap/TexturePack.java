package org.dynmap.hdmap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.dynmap.Color;
import org.dynmap.ConfigurationNode;
import org.dynmap.DynmapCore;
import org.dynmap.Log;
import org.dynmap.MapManager;
import org.dynmap.common.BiomeMap;
import org.dynmap.renderer.CustomColorMultiplier;
import org.dynmap.utils.BlockStep;
import org.dynmap.utils.DynIntHashMap;
import org.dynmap.utils.ForgeConfigFile;
import org.dynmap.utils.MapIterator;

/**
 * Loader and processor class for minecraft texture packs
 *  Texture packs are found in dynmap/texturepacks directory, and either are either ZIP files
 *  or are directories whose content matches the structure of a zipped texture pack:
 *    ./terrain.png - main color data (required)
 *    misc/grasscolor.png - tone for grass color, biome sensitive (required)
 *    misc/foliagecolor.png - tone for leaf color, biome sensitive (required)
 *    custom_lava_still.png - custom still lava animation (optional)
 *    custom_lava_flowing.png - custom flowing lava animation (optional)
 *    custom_water_still.png - custom still water animation (optional)
 *    custom_water_flowing.png - custom flowing water animation (optional)
 *    misc/watercolorX.png - custom water color multiplier (optional)
 *    misc/swampgrasscolor.png - tone for grass color in swamps (optional)
 *    misc/swampfoliagecolor.png - tone for leaf color in swamps (optional)
 */

public class TexturePack {
    /* Loaded texture packs */
    private static HashMap<String, TexturePack> packs = new HashMap<String, TexturePack>();
    private static Object packlock = new Object();
    
    private static final String TERRAIN_PNG = "terrain.png";
    private static final String GRASSCOLOR_PNG = "misc/grasscolor.png";
    private static final String GRASSCOLOR_RP_PNG = "assets/minecraft/textures/colormap/grass.png";
    private static final String FOLIAGECOLOR_PNG = "misc/foliagecolor.png";
    private static final String FOLIAGECOLOR_RP_PNG = "assets/minecraft/textures/colormap/foliage.png";
    private static final String WATERCOLORX_PNG = "misc/watercolorX.png";
    private static final String WATERCOLORX_RP_PNG = "assets/minecraft/mcpatcher/colormap/watercolorX.png";
    private static final String WATERCOLORX2_RP_PNG = "assets/minecraft/mcpatcher/colormap/water.png";
    private static final String CUSTOMLAVASTILL_PNG = "custom_lava_still.png";
    private static final String CUSTOMLAVAFLOWING_PNG = "custom_lava_flowing.png";
    private static final String CUSTOMWATERSTILL_PNG = "custom_water_still.png";
    private static final String CUSTOMWATERFLOWING_PNG = "custom_water_flowing.png";
    private static final String SWAMPGRASSCOLOR_PNG = "misc/swampgrasscolor.png";
    private static final String SWAMPGRASSCOLOR_RP_PNG = "assets/minecraft/mcpatcher/colormap/swampgrass.png";
    private static final String SWAMPFOLIAGECOLOR_PNG = "misc/swampfoliagecolor.png";
    private static final String SWAMPFOLIAGECOLOR_RP_PNG = "assets/minecraft/mcpatcher/colormap/swampfoliage.png";
    private static final String PINECOLOR_PNG = "misc/pinecolor.png";
    private static final String PINECOLOR_RP_PNG = "assets/minecraft/mcpatcher/colormap/pine.png";
    private static final String BIRCHCOLOR_PNG = "misc/birchcolor.png";
    private static final String BIRCHCOLOR_RP_PNG = "assets/minecraft/mcpatcher/colormap/birch.png";

    /* Color modifier codes (x1000 for value in definition file, x1000000 for internal value) */
    //private static final int COLORMOD_NONE = 0;
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
    
    private static final int COLORMOD_MULT_FILE = 1000;
    private static final int COLORMOD_MULT_INTERNAL = 1000000;
    /* Special tile index values */
    private static final int TILEINDEX_BLANK = -1;
    private static final int TILEINDEX_GRASS = 0;
    private static final int TILEINDEX_GRASSMASK = 38;
    private static final int TILEINDEX_SNOW = 66;
    private static final int TILEINDEX_SNOWSIDE = 68;
    private static final int TILEINDEX_PISTONSIDE = 108;
    private static final int TILEINDEX_GLASSPANETOP = 148;
    private static final int TILEINDEX_AIRFRAME = 158;
    private static final int TILEINDEX_REDSTONE_NSEW_TONE = 164;
    private static final int TILEINDEX_REDSTONE_EW_TONE = 165;
    private static final int TILEINDEX_EYEOFENDER = 174;
    private static final int TILEINDEX_REDSTONE_NSEW = 180;
    private static final int TILEINDEX_REDSTONE_EW = 181;
    private static final int TILEINDEX_STATIONARYWATER = 257;
    private static final int TILEINDEX_MOVINGWATER = 258;
    private static final int TILEINDEX_STATIONARYLAVA = 259;
    private static final int TILEINDEX_MOVINGLAVA = 260;
    private static final int TILEINDEX_PISTONEXTSIDE = 261;
    private static final int TILEINDEX_PISTONSIDE_EXT = 262;
    private static final int TILEINDEX_PANETOP_X = 263;
    private static final int TILEINDEX_AIRFRAME_EYE = 264;
    private static final int TILEINDEX_WHITE = 267; // Pure white tile
    private static final int MAX_TILEINDEX = 267;  /* Index of last static tile definition */

    /* Indexes of faces in a CHEST format tile file */
    private static final int TILEINDEX_CHEST_TOP = 0;
    private static final int TILEINDEX_CHEST_LEFT = 1;
    private static final int TILEINDEX_CHEST_RIGHT = 2;
    private static final int TILEINDEX_CHEST_FRONT = 3;
    private static final int TILEINDEX_CHEST_BACK = 4;
    private static final int TILEINDEX_CHEST_BOTTOM = 5;
    private static final int TILEINDEX_CHEST_COUNT = 6;

    /* Indexes of faces in a BIGCHEST format tile file */
    private static final int TILEINDEX_BIGCHEST_TOPLEFT = 0;
    private static final int TILEINDEX_BIGCHEST_TOPRIGHT = 1;
    private static final int TILEINDEX_BIGCHEST_FRONTLEFT = 2;
    private static final int TILEINDEX_BIGCHEST_FRONTRIGHT = 3;
    private static final int TILEINDEX_BIGCHEST_LEFT = 4;
    private static final int TILEINDEX_BIGCHEST_RIGHT = 5;
    private static final int TILEINDEX_BIGCHEST_BACKLEFT = 6;
    private static final int TILEINDEX_BIGCHEST_BACKRIGHT = 7;
    private static final int TILEINDEX_BIGCHEST_BOTTOMLEFT = 8;
    private static final int TILEINDEX_BIGCHEST_BOTTOMRIGHT = 9;
    private static final int TILEINDEX_BIGCHEST_COUNT = 10;

    /* Indexes of faces in the SIGN format tile file */
    private static final int TILEINDEX_SIGN_FRONT = 0;
    private static final int TILEINDEX_SIGN_BACK = 1;
    private static final int TILEINDEX_SIGN_TOP = 2;
    private static final int TILEINDEX_SIGN_BOTTOM = 3;
    private static final int TILEINDEX_SIGN_LEFTSIDE = 4;
    private static final int TILEINDEX_SIGN_RIGHTSIDE = 5;
    private static final int TILEINDEX_SIGN_POSTFRONT = 6;
    private static final int TILEINDEX_SIGN_POSTBACK = 7;
    private static final int TILEINDEX_SIGN_POSTLEFT = 8;
    private static final int TILEINDEX_SIGN_POSTRIGHT = 9;
    private static final int TILEINDEX_SIGN_COUNT = 10;

    /* Indexes of faces in the SKIN format tile file */
    private static final int TILEINDEX_SKIN_FACEFRONT = 0;
    private static final int TILEINDEX_SKIN_FACELEFT = 1;
    private static final int TILEINDEX_SKIN_FACERIGHT = 2;
    private static final int TILEINDEX_SKIN_FACEBACK = 3;
    private static final int TILEINDEX_SKIN_FACETOP = 4;
    private static final int TILEINDEX_SKIN_FACEBOTTOM = 5;
    private static final int TILEINDEX_SKIN_COUNT = 6;

    private static final int BLOCKTABLELEN = 4096; // Max block ID range
    
    public static enum TileFileFormat {
        GRID,
        CHEST,
        BIGCHEST,
        SIGN,
        SKIN,
        CUSTOM,
        TILESET,
        BIOME
    };
    
    /* Map of 1.5 texture files to 0-255 texture indices */
    private static final String[] terrain_map = {
        "grass_top", "stone", "dirt", "grass_side", "wood", "stoneslab_side", "stoneslab_top", "brick", 
        "tnt_side", "tnt_top", "tnt_bottom", "web", "rose", "flower", "portal", "sapling",
        "stonebrick", "bedrock", "sand", "gravel", "tree_side", "tree_top", "blockIron", "blockGold",
        "blockDiamond", "blockEmerald", null, null, "mushroom_red", "mushroom_brown", "sapling_jungle", null,
        "oreGold", "oreIron", "oreCoal", "bookshelf", "stoneMoss", "obsidian", "grass_side_overlay", "tallgrass",
        null, "beacon", null, "workbench_top", "furnace_front", "furnace_side", "dispenser_front", null,
        "sponge", "glass", "oreDiamond", "oreRedstone", "leaves", "leaves_opaque", "stonebricksmooth", "deadbush",
        "fern", null, null, "workbench_side", "workbench_front", "furnace_front_lit", "furnace_top", "sapling_spruce",
        "cloth_0", "mobSpawner", "snow", "ice", "snow_side", "cactus_top", "cactus_side", "cactus_bottom",
        "clay", "reeds", "musicBlock", "jukebox_top", "waterlily", "mycel_side", "mycel_top", "sapling_birch",
        "torch", "doorWood_upper", "doorIron_upper", "ladder", "trapdoor", "fenceIron", "farmland_wet", "farmland_dry",
        "crops_0", "crops_1", "crops_2", "crops_3", "crops_4", "crops_5", "crops_6", "crops_7",
        "lever", "doorWood_lower", "doorIron_lower", "redtorch_lit", "stonebricksmooth_mossy", "stonebricksmooth_cracked", "pumpkin_top", "hellrock",
        "hellsand", "lightgem", "piston_top_sticky", "piston_top", "piston_side", "piston_bottom", "piston_inner_top", "stem_straight",
        "rail_turn", "cloth_15", "cloth_7", "redtorch", "tree_spruce", "tree_birch", "pumpkin_side", "pumpkin_face",
        "pumpkin_jack", "cake_top", "cake_side", "cake_inner", "cake_bottom", "mushroom_skin_red", "mushroom_skin_brown", "stem_bent",
        "rail", "cloth_14", "cloth_6", "repeater", "leaves_spruce", "leaves_spruce_opaque", "bed_feet_top", "bed_head_top",
        "melon_side", "melon_top", "cauldron_top", "cauldron_inner", null, "mushroom_skin_stem", "mushroom_inside", "vine",
        "blockLapis", "cloth_13", "cloth_5", "repeater_lit", "thinglass_top", "bed_feet_end", "bed_feet_side", "bed_head_side",
        "bed_head_end", "tree_jungle", "cauldron_side", "cauldron_bottom", "brewingStand_base", "brewingStand", "endframe_top", "endframe_side",
        "oreLapis", "cloth_12", "cloth_4", "goldenRail", "redstoneDust_cross", "redstoneDust_line", "enchantment_top", "dragonEgg",
        "cocoa_2", "cocoa_1", "cocoa_0", "oreEmerald", "tripWireSource", "tripWire", "endframe_eye", "whiteStone",
        "sandstone_top", "cloth_11", "cloth_3", "goldenRail_powered", "redstoneDust_cross_overlay", "redstoneDust_line_overlay", "enchantment_side", "enchantment_bottom",
        "commandBlock", "itemframe_back", "flowerPot", null, null, null, null, null,
        "sandstone_side", "cloth_10", "cloth_2", "detectorRail", "leaves_jungle", "leaves_jungle_opaque", "wood_spruce", "wood_jungle",
        "carrots_0", "carrots_1", "carrots_2", "carrots_3", "potatoes_3", null, null, null,
        "sandstone_bottom", "cloth_9", "cloth_1", "redstoneLight", "redstoneLight_lit", "stonebricksmooth_carved", "wood_birch", "anvil_base",
        "anvil_top_damaged_1", null, null, null, null, null, null, null,
        "netherBrick", "cloth_8", "netherStalk_0", "netherStalk_1", "netherStalk_2", "sandstone_carved", "sandstone_smooth", "anvil_top",
        "anvil_top_damaged_2", null, null, null, null, null, null, null,
        "destroy_0", "destroy_1", "destroy_2", "destroy_3", "destroy_4", "destroy_5", "destroy_6", "destroy_7",
        "destroy_8", "destroy_9", null, null, null, null, null, null,
        /* Extra 1.5-based textures: starting at 256 (corresponds to TILEINDEX_ values) */
        null, "water", "water_flow", "lava", "lava_flow", null, null, null, 
        null, "fire_0", "portal"
    };

    /* Map of 1.6 resource files to 0-255 texture indices */
    private static final String[] terrain_rp_map = {
        "grass_top", "stone", "dirt", "grass_side", "planks_oak", "stone_slab_side", "stone_slab_top", "brick", 
        "tnt_side", "tnt_top", "tnt_bottom", "web", "flower_rose", "flower_dandelion", "portal", "sapling_oak",
        "cobblestone", "bedrock", "sand", "gravel", "log_oak", "log_oak_top", "iron_block", "gold_block",
        "diamond_block", "emerald_block", null, null, "mushroom_red", "mushroom_brown", "sapling_jungle", null,
        "gold_ore", "iron_ore", "coal_ore", "bookshelf", "cobblestone_mossy", "obsidian", "grass_side_overlay", "tallgrass",
        null, "beacon", null, "crafting_table_top", "furnace_front_off", "furnace_side", "dispenser_front_horizontal", null,
        "sponge", "glass", "diamond_ore", "redstone_ore", "leaves_oak", "leaves_oak_opaque", "stonebrick", "deadbush",
        "fern", null, null, "crafting_table_side", "crafting_table_front", "furnace_front_on", "furnace_top", "sapling_spruce",
        "wool_colored_white", "mob_spawner", "snow", "ice", "grass_side_snowed", "cactus_top", "cactus_side", "cactus_bottom",
        "clay", "reeds", "jukebox_side", "jukebox_top", "waterlily", "mycelium_side", "mycelium_top", "sapling_birch",
        "torch_on", "door_wood_upper", "door_iron_upper", "ladder", "trapdoor", "iron_bars", "farmland_wet", "farmland_dry",
        "wheat_stage_0", "wheat_stage_1", "wheat_stage_2", "wheat_stage_3", "wheat_stage_4", "wheat_stage_5", "wheat_stage_6", "wheat_stage_7",
        "lever", "door_wood_lower", "door_iron_lower", "redstone_torch_on", "stonebrick_mossy", "stonebrick_cracked", "pumpkin_top", "netherrack",
        "soul_sand", "glowstone", "piston_top_sticky", "piston_top_normal", "piston_side", "piston_bottom", "piston_inner", "pumpkin_stem_disconnected",
        "rail_normal_turned", "wool_colored_black", "wool_colored_gray", "redstone_torch_off", "log_spruce", "log_birch", "pumpkin_side", "pumpkin_face_off",
        "pumpkin_face_on", "cake_top", "cake_side", "cake_inner", "cake_bottom", "mushroom_block_skin_red", "mushroom_block_skin_brown", "pumpkin_stem_connected",
        "rail_normal", "wool_colored_red", "wool_colored_pink", "repeater_off", "leaves_spruce", "leaves_spruce_opaque", "bed_feet_top", "bed_head_top",
        "melon_side", "melon_top", "cauldron_top", "cauldron_inner", null, "mushroom_block_skin_stem", "mushroom_block_inside", "vine",
        "lapis_block", "wool_colored_green", "wool_colored_lime", "repeater_on", "glass_pane_top", "bed_feet_end", "bed_feet_side", "bed_head_side",
        "bed_head_end", "log_jungle", "cauldron_side", "cauldron_bottom", "brewibrewing_stand_base", "brewing_stand", "endframe_top", "endframe_side",
        "lapis_ore", "wool_colored_brown", "wool_colored_yellow", "rail_golden", "redstone_dust_cross", "redstone_dust_line", "enchanting_table_top", "dragon_egg",
        "cocoa_stage_2", "cocoa_stage_1", "cocoa_stage_0", "emerald_ore", "trip_wire_source", "trip_wire", "endframe_eye", "end_stone",
        "sandstone_top", "wool_colored_blue", "wool_colored_light_blue", "rail_golden_powered", "redstone_dust_cross_overlay", "redstone_dust_line_overlay", "enchanting_table_side", "enchanting_table_bottom",
        "command_block", "itemframe_background", "flower_pot", null, null, null, null, null,
        "sandstone_normal", "wool_colored_purple", "wool_colored_magenta", "rail_detector", "leaves_jungle", "leaves_jungle_opaque", "planks_spruce", "planks_jungle",
        "carrots_stage_0", "carrots_stage_1", "carrots_stage_2", "carrots_stage_3", "potatoes_stage_3", null, null, null,
        "sandstone_bottom", "wool_colored_cyan", "wool_colored_orange", "redstone_lamp_off", "redstone_lamp_on", "stonebrick_carved", "planks_birch", "anvil_base",
        "anvil_top_damaged_1", null, null, null, null, null, null, null,
        "nether_brick", "wool_colored_silver", "nether_wart_stage_0", "nether_wart_stage_1", "nether_wart_stage_2", "sandstone_carved", "sandstone_smooth", "anvil_top_damaged_0",
        "anvil_top_damaged_2", null, null, null, null, null, null, null,
        "destroy_stage_0", "destroy_stage_1", "destroy_stage_2", "destroy_stage_3", "destroy_stage_4", "destroy_stage_5", "destroy_stage_6", "destroy_stage_7",
        "destroy_stage_8", "destroy_stage_9", null, null, null, null, null, null,
        /* Extra 1.5-based textures: starting at 256 (corresponds to TILEINDEX_ values) */
        null, "water_still", "water_flow", "lava_still", "lava_flow", null, null, null, 
        null, "fire_layer_0", "portal"
    };

    private static class CustomTileRec {
        int srcx, srcy, width, height, targetx, targety;
    }
    
    private static int next_dynamic_tile = MAX_TILEINDEX+1;
    
    private static class DynamicTileFile {
        int idx;                    /* Index of tile in addonfiles */
        String filename;
        String modname;             /* Modname associated with file, if any */
        int tilecnt_x, tilecnt_y;   /* Number of tiles horizontally and vertically */
        int tile_to_dyntile[];      /* Mapping from tile index in tile file to dynamic ID in global tile table (terrain_argb): 0=unassigned */
        TileFileFormat format;
        List<CustomTileRec> cust;
        String[] tilenames;         /* For TILESET, array of tilenames, indexed by tile index */
    }
    private static ArrayList<DynamicTileFile> addonfiles = new ArrayList<DynamicTileFile>();
    private static Map<String, DynamicTileFile> addonfilesbyname = new HashMap<String, DynamicTileFile>();

    private static String getBlockFileName(int idx) {
        if ((idx >= 0) && (idx < terrain_map.length) && (terrain_map[idx] != null)) {
            return "textures/blocks/" + terrain_map[idx] + ".png";
        }
        return null;
    }

    private static String getRPFileName(int idx) {
        if ((idx >= 0) && (idx < terrain_rp_map.length) && (terrain_rp_map[idx] != null)) {
            return "assets/minecraft/textures/blocks/" + terrain_rp_map[idx] + ".png";
        }
        return null;
    }

    /* Reset add-on tile data */
    private static void resetFiles(DynmapCore core) {
        synchronized(packlock) {
            packs.clear();
        }
        addonfiles.clear();
        addonfilesbyname.clear();
        next_dynamic_tile = MAX_TILEINDEX+1;
        
        /* Now, load entries for vanilla v1.6.x RP files */
        for(int i = 0; i < terrain_rp_map.length; i++) {
            String fn = getRPFileName(i);
            if (fn != null) {
                int idx = findOrAddDynamicTileFile(fn, null, 1, 1, TileFileFormat.GRID, new String[0]);
                DynamicTileFile dtf = addonfiles.get(idx);
                if (dtf != null) {  // Fix mapping of tile ID to global table index
                    dtf.tile_to_dyntile[0] = i;
                }
            }
        }
        /* Now, load entries for vanilla v1.5.x files (put second so that add-on TP overrides built in RP) */
        for(int i = 0; i < terrain_map.length; i++) {
            String fn = getBlockFileName(i);
            if (fn != null) {
                int idx = findOrAddDynamicTileFile(fn, null, 1, 1, TileFileFormat.GRID, new String[0]);
                DynamicTileFile dtf = addonfiles.get(idx);
                if (dtf != null) {  // Fix mapping of tile ID to global table index
                    dtf.tile_to_dyntile[0] = i;
                }
            }
        }
    }
    
    private static class LoadedImage {
        int[] argb;
        int width, height;
        int trivial_color;
    }    
    
    private int[][]   tile_argb;
    private int[] blank;
    private int native_scale;
    private CTMTexturePack ctm;
    private BitSet hasBlockColoring = new BitSet(); // Quick lookup - (blockID << 4) + blockMeta - set if custom colorizer
    private DynIntHashMap blockColoring = new DynIntHashMap();  // Map - index by (blockID << 4) + blockMeta - Index of image for color map

    private int colorMultBirch = 0x80a755;  /* From ColorizerFoliage.java in MCP */
    private int colorMultPine = 0x619961;   /* From ColorizerFoliage.java in MCP */
    private int colorMultLily = 0x208030;   /* from BlockLilyPad.java in MCP */
    private int colorMultWater = 0xFFFFFF; 
    
    private static final int IMG_GRASSCOLOR = 0;
    private static final int IMG_FOLIAGECOLOR = 1;
    private static final int IMG_CUSTOMWATERMOVING = 2;
    private static final int IMG_CUSTOMWATERSTILL = 3;
    private static final int IMG_CUSTOMLAVAMOVING = 4;
    private static final int IMG_CUSTOMLAVASTILL = 5;
    private static final int IMG_WATERCOLORX = 6;
    private static final int IMG_SWAMPGRASSCOLOR = 7;
    private static final int IMG_SWAMPFOLIAGECOLOR = 8;
    private static final int IMG_PINECOLOR = 9;
    private static final int IMG_BIRCHCOLOR = 10;
    
    private static final int IMG_CNT = 11;
    /* 0-(IMG_CNT-1) are fixed, IMG_CNT+x is dynamic file x */
    private LoadedImage[] imgs;

    private HashMap<Integer, TexturePack> scaled_textures;
    private Object scaledlock = new Object();
    
    public enum BlockTransparency {
        OPAQUE, /* Block is opaque - blocks light - lit by light from adjacent blocks */
        TRANSPARENT,    /* Block is transparent - passes light - lit by light level in own block */ 
        SEMITRANSPARENT, /* Opaque block that doesn't block all rays (steps, slabs) - use light above for face lighting on opaque blocks */
        LEAVES /* Special case of transparent, to work around lighting errors in SpoutPlugin */
    }
    public static class HDTextureMap {
        private int faces[];  /* index in terrain.png of image for each face (indexed by BlockStep.ordinal() OR patch index) */
        private byte[] layers;  /* If layered, each index corresponds to faces index, and value is index of next layer */
        private List<Integer> blockids;
        private int databits;
        private BlockTransparency bt;
        private boolean userender;
        private String blockset;
        private int colorMult;
        private CustomColorMultiplier custColorMult;
        private boolean stdrotate; // Marked for corrected to proper : stdrot=true
        private static HDTextureMap[] texmaps;
        private static BlockTransparency transp[];
        private static boolean userenderdata[];
        private static HDTextureMap blank;
                
        public int getIndexForFace(int face) {
            if ((faces != null) && (faces.length > face))
                return faces[face];
            return TILEINDEX_BLANK;
        }
        
        private static void initializeTable() {
            texmaps = new HDTextureMap[16*BLOCKTABLELEN];
            transp = new BlockTransparency[BLOCKTABLELEN];
            userenderdata = new boolean[BLOCKTABLELEN];
            blank = new HDTextureMap();
            for(int i = 0; i < texmaps.length; i++)
                texmaps[i] = blank;
            for(int i = 0; i < transp.length; i++)
                transp[i] = BlockTransparency.OPAQUE;
        }
        
        private HDTextureMap() {
            blockids = Collections.singletonList(Integer.valueOf(0));
            databits = 0xFFFF;
            userender = false;
            blockset = null;
            colorMult = 0;
            custColorMult = null;
            faces = new int[] { TILEINDEX_BLANK, TILEINDEX_BLANK, TILEINDEX_BLANK, TILEINDEX_BLANK, TILEINDEX_BLANK, TILEINDEX_BLANK };
            layers = null;
            stdrotate = true;
        }
        
        public HDTextureMap(List<Integer> blockids, int databits, int[] faces, byte[] layers, BlockTransparency trans, boolean userender, int colorMult, CustomColorMultiplier custColorMult, String blockset, boolean stdrot) {
            this.faces = faces;
            this.layers = layers;
            this.blockids = blockids;
            this.databits = databits;
            this.bt = trans;
            this.colorMult = colorMult;
            this.custColorMult = custColorMult;
            this.userender = userender;
            this.blockset = blockset;
            this.stdrotate = stdrot;
        }
        
        public void addToTable() {
            /* Add entries to lookup table */
            for(Integer blkid : blockids) {
                if(blkid > 0) {
                    for(int i = 0; i < 16; i++) {
                        if((databits & (1 << i)) != 0) {
                            int idx = 16*blkid + i;
                            
                            if((this.blockset != null) && (this.blockset.equals("core") == false)) {
                                HDBlockModels.resetIfNotBlockSet(blkid, i, this.blockset);
                            }
                            
                            texmaps[idx] = this;
                        }
                    }
                    transp[blkid] = bt; /* Transparency is only blocktype based right now */
                    userenderdata[blkid] = userender;	/* Ditto for using render data */
                }
            }
        }
        
        public static HDTextureMap getMap(int blkid, int blkdata, int blkrenderdata) {
            try {
                if(userenderdata[blkid])
                    return texmaps[(blkid<<4) + blkrenderdata];
                else
                    return texmaps[(blkid<<4) + blkdata];
            } catch (Exception x) {
                return blank;
            }
        }
        
        public static BlockTransparency getTransparency(int blkid) {
            try {
                return transp[blkid];
            } catch (Exception x) {
                return BlockTransparency.OPAQUE;
            }
        }
        
        private static void remapTexture(int id, int srcid) {
            for(int i = 0; i < 16; i++) {
                texmaps[(id<<4)+i] = texmaps[(srcid<<4)+i];
            }
            transp[id] = transp[srcid];
            userenderdata[id] = userenderdata[srcid];
        }

    }
    
    /**
     * Texture map - used for accumulation of textures from different sources, keyed by lookup value
     */
    public static class TextureMap {
        private Map<Integer, Integer> key_to_index = new HashMap<Integer, Integer>();
        private List<Integer> texture_ids = new ArrayList<Integer>();
        private List<Integer> blockids = new ArrayList<Integer>();
        private int databits = 0;
        private BlockTransparency trans = BlockTransparency.OPAQUE;
        private boolean userender = false;
        private int colorMult = 0;
        private CustomColorMultiplier custColorMult = null;
        private String blockset;

        public int addTextureByKey(int key, int textureid) {
            int off = texture_ids.size();   /* Next index in array is texture index */
            texture_ids.add(textureid); /* Add texture ID to list */
            key_to_index.put(key, off);   /* Add texture index to lookup by key */
            return off;
        }
    }
    private static HashMap<String, TextureMap> textmap_by_id = new HashMap<String, TextureMap>();
    
    /**
     * Set tile ARGB buffer at index
     */
    public final void setTileARGB(int idx, int[] buf) {
        if (idx >= tile_argb.length) {
            tile_argb = Arrays.copyOf(tile_argb, 3*idx/2);
        }
        tile_argb[idx] = buf;
    }
    /**
     * Get tile ARGB buffer at index
     */
    public final int[] getTileARGB(int idx) {
        int[] rslt = blank;
        if (idx < tile_argb.length) {
            rslt = tile_argb[idx];
            if (rslt == null) {
                rslt = tile_argb[idx] = blank;
            }
        }
        return rslt;
    }
    /**
     * Add texture to texture map
     */
    private static int addTextureByKey(String id, int key, int textureid) {
        TextureMap idx = textmap_by_id.get(id);
        if(idx == null) {   /* Add empty one, if not found */
            idx = new TextureMap();
            textmap_by_id.put(id,  idx);
        }
        return idx.addTextureByKey(key, textureid);
    }
    /**
     * Add settings for texture map
     */
    private static void addTextureIndex(String id, List<Integer> blockids, int databits, BlockTransparency trans, boolean userender, int colorMult, CustomColorMultiplier custColorMult, String blockset) {
        TextureMap idx = textmap_by_id.get(id);
        if(idx == null) {   /* Add empty one, if not found */
            idx = new TextureMap();
            textmap_by_id.put(id,  idx);
        }
        idx.blockids = blockids;
        idx.databits = databits;
        idx.trans = trans;
        idx.userender = userender;
        idx.colorMult = colorMult;
        idx.custColorMult = custColorMult;
    }
    /**
     * Finish processing of texture indexes - add to texture maps
     */
    private static void processTextureMaps() {
        for(TextureMap ti : textmap_by_id.values()) {
            if(ti.blockids.isEmpty()) continue;
            int[] txtids = new int[ti.texture_ids.size()];
            for(int i = 0; i < txtids.length; i++) {
                txtids[i] = ti.texture_ids.get(i).intValue();
            }
            HDTextureMap map = new HDTextureMap(ti.blockids, ti.databits, txtids, null, ti.trans, ti.userender, ti.colorMult, ti.custColorMult, ti.blockset, true);
            map.addToTable();
        }
    }
    /**
     * Get index of texture in texture map
     */
    public static int getTextureIndexFromTextureMap(String id, int key) {
        int idx = -1;
        TextureMap map = textmap_by_id.get(id);
        if(map != null) {
            Integer txtidx = map.key_to_index.get(key);
            if(txtidx != null) {
                idx = txtidx.intValue();
            }
        }
        return idx;
    }
    /*
     * Get count of textures in given texture map
     */
    public static int getTextureMapLength(String id) {
        TextureMap map = textmap_by_id.get(id);
        if(map != null) {
            return map.texture_ids.size();
        }
        return -1;
    }
    /** Get or load texture pack */
    public static TexturePack getTexturePack(DynmapCore core, String tpname) {
        synchronized(packlock) {
            TexturePack tp = packs.get(tpname);
            if(tp != null)
                return tp;
            try {
                tp = new TexturePack(core, tpname);   /* Attempt to load pack */
                packs.put(tpname, tp);
                return tp;
            } catch (FileNotFoundException fnfx) {
                Log.severe("Error loading texture pack '" + tpname + "' - not found");
            }
            return null;
        }
    }
    /**
     * Constructor for texture pack, by name
     */
    private TexturePack(DynmapCore core, String tpname) throws FileNotFoundException {
        File texturedir = getTexturePackDirectory(core);

        /* Set up for enough files */
        imgs = new LoadedImage[IMG_CNT + addonfiles.size()];

        // Get texture pack
        File f = new File(texturedir, tpname);
        // Build loader
        TexturePackLoader tpl = new TexturePackLoader(f, core);
        InputStream is = null;
        try {
            boolean is_rp = false;
            /* Check if resource pack */
            is = tpl.openTPResource("pack.mcmeta");
            if (is != null) {
                tpl.closeResource(is);
                is_rp = true;
                Log.info("Loading resource pack " + f.getName());
            }
            else if(tpname.equals("standard")) { // Built in is RP
                is_rp = true;
                Log.info("Loading default resource pack");
            }
            else {
                Log.info("Loading texture pack " + f.getName());
            }
            /* Load CTM support, if enabled */
            if(core.isCTMSupportEnabled()) {
                ctm = new CTMTexturePack(tpl, this, core, is_rp);
                if(ctm.isValid() == false) {
                    ctm = null;
                }
            }
            String fn;
            /* Load custom colors support, if enabled */
            if(core.isCustomColorsSupportEnabled()) {
                fn = (is_rp?"assets/minecraft/mcpatcher/color.properties":"color.properties");
                is = tpl.openTPResource(fn);
                Properties p;
                if (is != null) {
                    p = new Properties();
                    try {
                        p.load(is);
                    } finally {
                        tpl.closeResource(is);
                    }
                    processCustomColors(p);
                }
            }
            /* Loop through dynamic files */
            for(int i = 0; i < addonfiles.size(); i++) {
                DynamicTileFile dtf = addonfiles.get(i);
                is = tpl.openModTPResource(dtf.filename, dtf.modname);
                try {
                    if(dtf.format == TileFileFormat.BIOME)
                        loadBiomeShadingImage(is, i+IMG_CNT); /* Load image file */
                    else
                        loadImage(is, i+IMG_CNT); /* Load image file */
                } finally {
                    tpl.closeResource(is);
                }
            }
            /* Find and load terrain.png */
            is = tpl.openTPResource(TERRAIN_PNG); /* Try to find terrain.png */
            if (is != null) {
                loadTerrainPNG(is, is_rp);
                tpl.closeResource(is);
            }
            /* Try to find and load misc/grasscolor.png */
            is = tpl.openTPResource(GRASSCOLOR_PNG, GRASSCOLOR_RP_PNG);
            if (is != null) {
                loadBiomeShadingImage(is, IMG_GRASSCOLOR);
                tpl.closeResource(is);
            }
            /* Try to find and load misc/foliagecolor.png */
            is = tpl.openTPResource(FOLIAGECOLOR_PNG, FOLIAGECOLOR_RP_PNG);
            if (is != null) {
                loadBiomeShadingImage(is, IMG_FOLIAGECOLOR);
                tpl.closeResource(is);
            }
            /* Try to find and load misc/swampgrasscolor.png */
            is = tpl.openTPResource(SWAMPGRASSCOLOR_PNG, SWAMPGRASSCOLOR_RP_PNG);
            if (is != null) {
                loadBiomeShadingImage(is, IMG_SWAMPGRASSCOLOR);
                tpl.closeResource(is);
            }
            /* Try to find and load misc/swampfoliagecolor.png */
            is = tpl.openTPResource(SWAMPFOLIAGECOLOR_PNG, SWAMPFOLIAGECOLOR_RP_PNG);
            if (is != null) {
                loadBiomeShadingImage(is, IMG_SWAMPFOLIAGECOLOR);
                tpl.closeResource(is);
            }
            /* Try to find and load misc/watercolor.png */
            is = tpl.openTPResource(WATERCOLORX_PNG, WATERCOLORX_RP_PNG);
            if (is == null) {
                /* Try to find and load colormap/water.png */
                is = tpl.openTPResource(WATERCOLORX_PNG, WATERCOLORX2_RP_PNG);
            }
            if (is != null) {
                loadBiomeShadingImage(is, IMG_WATERCOLORX);
                tpl.closeResource(is);
            }
            /* Try to find pine.png */
            is = tpl.openTPResource(PINECOLOR_PNG, PINECOLOR_RP_PNG);
            if (is != null) {
                loadBiomeShadingImage(is, IMG_PINECOLOR);
                tpl.closeResource(is);
            }
            /* Try to find birch.png */
            is = tpl.openTPResource(BIRCHCOLOR_PNG, BIRCHCOLOR_RP_PNG);
            if (is != null) {
                loadBiomeShadingImage(is, IMG_BIRCHCOLOR);
                tpl.closeResource(is);
            }
            /* Optional files - process if they exist */
            is = tpl.openTPResource(CUSTOMLAVASTILL_PNG);
            if (is == null) {
                is = tpl.openTPResource("anim/" + CUSTOMLAVASTILL_PNG);
            }
            if (is != null) {
                loadImage(is, IMG_CUSTOMLAVASTILL);
                tpl.closeResource(is);
                patchTextureWithImage(IMG_CUSTOMLAVASTILL, TILEINDEX_STATIONARYLAVA);
                patchTextureWithImage(IMG_CUSTOMLAVASTILL, TILEINDEX_MOVINGLAVA);
            }
            is = tpl.openTPResource(CUSTOMLAVAFLOWING_PNG);
            if (is == null) {
                is = tpl.openTPResource("anim/" + CUSTOMLAVAFLOWING_PNG);
            }
            if (is != null) {
                loadImage(is, IMG_CUSTOMLAVAMOVING);
                tpl.closeResource(is);
                patchTextureWithImage(IMG_CUSTOMLAVAMOVING, TILEINDEX_MOVINGLAVA);
            }
            is = tpl.openTPResource(CUSTOMWATERSTILL_PNG);
            if (is == null) {
                is = tpl.openTPResource("anim/" + CUSTOMWATERSTILL_PNG);
            }
            if (is != null) {
                loadImage(is, IMG_CUSTOMWATERSTILL);
                tpl.closeResource(is);
                patchTextureWithImage(IMG_CUSTOMWATERSTILL, TILEINDEX_STATIONARYWATER);
                patchTextureWithImage(IMG_CUSTOMWATERSTILL, TILEINDEX_MOVINGWATER);
            }
            is = tpl.openTPResource(CUSTOMWATERFLOWING_PNG);
            if (is == null) {
                is = tpl.openTPResource("anim/" + CUSTOMWATERFLOWING_PNG);
            }
            if (is != null) {
                loadImage(is, IMG_CUSTOMWATERMOVING);
                tpl.closeResource(is);
                patchTextureWithImage(IMG_CUSTOMWATERMOVING, TILEINDEX_MOVINGWATER);
            }
            /* Loop through dynamic files */
            for(int i = 0; i < addonfiles.size(); i++) {
                DynamicTileFile dtf = addonfiles.get(i);
                processDynamicImage(i, dtf.format);
            }
        } catch (IOException iox) {
            Log.severe("Error loadling texture pack", iox);
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException iox) {}
                is = null;
            }
            tpl.close();
        }
    }
    /**
     * Copy subimage from portions of given image
     * @param img_id - image ID of raw image
     * @param from_x - top-left X
     * @param from_y - top-left Y
     * @param to_x - dest topleft
     * @param to_y - dest topleft
     * @param width - width to copy
     * @param height - height to copy
     * @param dest_argb - destination tile buffer
     * @param dest_width - width of destination tile buffer
     */
    private void copySubimageFromImage(int img_id, int from_x, int from_y, int to_x, int to_y, int width, int height, int[] dest_argb, int dest_width) {
        for(int h = 0; h < height; h++) {
            System.arraycopy(imgs[img_id].argb, (h+from_y)*imgs[img_id].width + from_x, dest_argb, dest_width*(h+to_y) + to_x, width);
        }
    }
    private enum HandlePos { CENTER, LEFT, RIGHT, NONE, LEFTFRONT, RIGHTFRONT };
    /**
     * Make chest side image (based on chest and largechest layouts)
     * @param img_id - source image ID
     * @param dest_idx - destination tile index
     * @param src_x - starting X of source (scaled based on 64 high)
     * @param width - width to copy (scaled based on 64 high)
     * @param dest_x - destination X (scaled based on 64 high)
     * @param handlepos - 0=middle,1=leftedge,2=rightedge
     */
    private void makeChestSideImage(int img_id, int dest_idx, int src_x, int width, int dest_x, HandlePos handlepos) {
        if(dest_idx <= 0) return;
        int mult = imgs[img_id].height / 64; /* Nominal height for chest images is 64 */
        int[] tile = new int[16 * 16 * mult * mult];    /* Make image */
        /* Copy top part */
        copySubimageFromImage(img_id, src_x * mult, 14 * mult, dest_x * mult, 2 * mult, width * mult, 5 * mult, tile, 16 * mult);
        /* Copy bottom part */
        copySubimageFromImage(img_id, src_x * mult, 34 * mult, dest_x * mult, 7 * mult, width * mult, 9 * mult, tile, 16 * mult);
        /* Handle the handle image */
        if(handlepos == HandlePos.CENTER) {    /* Middle */
            copySubimageFromImage(img_id, 1 * mult, 1 * mult, 7 * mult, 4 * mult, 2 * mult, 4 * mult, tile, 16 * mult);
        }
        else if(handlepos == HandlePos.LEFT) {   /* left edge */
            copySubimageFromImage(img_id, 3 * mult, 1 * mult, 0 * mult, 4 * mult, 1 * mult, 4 * mult, tile, 16 * mult);
        }
        else if(handlepos == HandlePos.LEFTFRONT) {   /* left edge - front of handle */
            copySubimageFromImage(img_id, 2 * mult, 1 * mult, 0 * mult, 4 * mult, 1 * mult, 4 * mult, tile, 16 * mult);
        }
        else if(handlepos == HandlePos.RIGHT) {  /* Right */
            copySubimageFromImage(img_id, 0 * mult, 1 * mult, 15 * mult, 4 * mult, 1 * mult, 4 * mult, tile, 16 * mult);
        }
        else if(handlepos == HandlePos.RIGHTFRONT) {  /* Right - front of handle */
            copySubimageFromImage(img_id, 1 * mult, 1 * mult, 15 * mult, 4 * mult, 1 * mult, 4 * mult, tile, 16 * mult);
        }
        /* Put scaled result into tile buffer */
        int new_argb[] = new int[native_scale*native_scale];
        scaleTerrainPNGSubImage(16*mult, native_scale, tile, new_argb);
        setTileARGB(dest_idx, new_argb);
    }
    /**
     * Make chest top/bottom image (based on chest and largechest layouts)
     * @param img_id - source image ID
     * @param dest_idx - destination tile index
     * @param src_x - starting X of source (scaled based on 64 high)
     * @param src_y - starting Y of source (scaled based on 64 high)
     * @param width - width to copy (scaled based on 64 high)
     * @param dest_x - destination X (scaled based on 64 high)
     * @param handlepos - 0=middle,1=left-edge (righttop),2=right-edge (lefttop)
     */
    private void makeChestTopBottomImage(int img_id, int dest_idx, int src_x, int src_y, int width, int dest_x, HandlePos handlepos) {
        if(dest_idx <= 0) return;
        
        int mult = imgs[img_id].height / 64; /* Nominal height for chest images is 64 */
        int[] tile = new int[16 * 16 * mult * mult];    /* Make image */
        copySubimageFromImage(img_id, src_x * mult, src_y * mult, dest_x * mult, 1 * mult, width * mult, 14 * mult, tile, 16 * mult);
        /* Handle the handle image */
        if(handlepos == HandlePos.CENTER) {    /* Middle */
            copySubimageFromImage(img_id, 1 * mult, 0, 7 * mult, 15 * mult, 2 * mult, 1 * mult, tile, 16 * mult);
        }
        else if(handlepos == HandlePos.LEFT) {   /* left edge */
            copySubimageFromImage(img_id, 2 * mult, 0, 0 * mult, 15 * mult, 1 * mult, 1 * mult, tile, 16 * mult);
        }
        else if(handlepos == HandlePos.RIGHT) {  /* Right */
            copySubimageFromImage(img_id, 1 * mult, 0, 15 * mult, 15 * mult, 1 * mult, 1 * mult, tile, 16 * mult);
        }
        /* Put scaled result into tile buffer */
        int new_argb[] = new int[native_scale*native_scale];
        scaleTerrainPNGSubImage(16*mult, native_scale, tile, new_argb);
        setTileARGB(dest_idx, new_argb);
    }
    /**
     * Patch tiles based on image with chest-style layout
     */
    private void patchChestImages(int img_id, int tile_top, int tile_bottom, int tile_front, int tile_back, int tile_left, int tile_right) {
        makeChestSideImage(img_id, tile_front, 14, 14, 1, HandlePos.CENTER);
        makeChestSideImage(img_id, tile_back, 42, 14, 1, HandlePos.NONE);
        makeChestSideImage(img_id, tile_left, 0, 14, 1, HandlePos.RIGHT);
        makeChestSideImage(img_id, tile_right, 28, 14, 1, HandlePos.LEFT);
        makeChestTopBottomImage(img_id, tile_top, 14, 0, 14, 1, HandlePos.CENTER);
        makeChestTopBottomImage(img_id, tile_bottom, 28, 19, 14, 1, HandlePos.CENTER);
    }
    /**
     * Patch tiles based on image with large-chest-style layout
     */
    private void patchLargeChestImages(int img_id, int tile_topright, int tile_topleft, int tile_bottomright, int tile_bottomleft, int tile_right, int tile_left, int tile_frontright, int tile_frontleft, int tile_backright, int tile_backleft) {
        makeChestSideImage(img_id, tile_frontleft, 14, 15, 1, HandlePos.RIGHTFRONT);
        makeChestSideImage(img_id, tile_frontright, 29, 15, 0, HandlePos.LEFTFRONT);
        makeChestSideImage(img_id, tile_left, 0, 14, 1, HandlePos.RIGHT);
        makeChestSideImage(img_id, tile_right, 44, 14, 1, HandlePos.LEFT);
        makeChestSideImage(img_id, tile_backright, 58, 15, 1, HandlePos.NONE);
        makeChestSideImage(img_id, tile_backleft, 73, 15, 0, HandlePos.NONE);
        makeChestTopBottomImage(img_id, tile_topleft, 14, 0, 15, 1, HandlePos.RIGHT);
        makeChestTopBottomImage(img_id, tile_topright, 29, 0, 15, 0, HandlePos.LEFT);
        makeChestTopBottomImage(img_id, tile_bottomleft, 34, 19, 15, 1, HandlePos.RIGHT);
        makeChestTopBottomImage(img_id, tile_bottomright, 49, 19, 15, 0, HandlePos.LEFT);
    }

    /**
     * Make sign image (based on sign layouts)
     * @param img_id - source image ID
     * @param dest_idx - destination tile index
     * @param src_x - starting X of source (scaled based on 32 high)
     * @param src_y - starting Y of source (scaled based on 32 high)
     * @param width - width to copy (scaled based on 32 high)
     * @param height - height to copy (scaled based on 32 high)
     */
    private void makeSignImage(int img_id, int dest_idx, int src_x, int src_y, int width, int height) {
        int mult = imgs[img_id].height / 32; /* Nominal height for sign images is 32 */
        int[] tile = new int[24 * 24 * mult * mult];    /* Make image (all are 24x24) */
        copySubimageFromImage(img_id, src_x * mult, src_y * mult, 0, (24-height)*mult, width * mult, height * mult, tile, 24 * mult);
        /* Put scaled result into tile buffer */
        int new_argb[] = new int[native_scale*native_scale];
        scaleTerrainPNGSubImage(24*mult, native_scale, tile, new_argb);
        setTileARGB(dest_idx, new_argb);
    }

    private void patchSignImages(int img, int sign_front, int sign_back, int sign_top, int sign_bottom, int sign_left, int sign_right, int post_front, int post_back, int post_left, int post_right)
    {
        /* Load images at lower left corner of each tile */
        makeSignImage(img, sign_front, 2, 2, 24, 12);
        makeSignImage(img, sign_back, 28, 2, 24, 12);
        makeSignImage(img, sign_top, 2, 0, 24, 2);
        makeSignImage(img, sign_left, 0, 2, 2, 12);
        makeSignImage(img, sign_right, 26, 2, 2, 12);
        makeSignImage(img, sign_bottom, 26, 0, 24, 2);
        makeSignImage(img, post_front, 0, 16, 2, 14);
        makeSignImage(img, post_right, 2, 16, 2, 14);
        makeSignImage(img, post_back, 4, 16, 2, 14);
        makeSignImage(img, post_left, 6, 16, 2, 14);
    }

    /**
     * Make face image (based on skin layouts)
     * @param img_id - source image ID
     * @param dest_idx - destination tile index
     * @param src_x - starting X of source (scaled based on 32 high)
     * @param src_y - starting Y of source (scaled based on 32 high)
     */
    private void makeFaceImage(int img_id, int dest_idx, int src_x, int src_y) {
        int mult = imgs[img_id].width / 64; /* Nominal height for skin images is 32 */
        int[] tile = new int[8 * 8 * mult * mult];    /* Make image (all are 8x8) */
        copySubimageFromImage(img_id, src_x * mult, src_y * mult, 0, 0, 8 * mult, 8 * mult, tile, 8 * mult);
        /* Put scaled result into tile buffer */
        int new_argb[] = new int[native_scale*native_scale];
        scaleTerrainPNGSubImage(8 * mult, native_scale, tile, new_argb);
        setTileARGB(dest_idx, new_argb);
    }
    
    private void patchSkinImages(int img, int face_front, int face_left, int face_right, int face_back, int face_top, int face_bottom)
    {
        makeFaceImage(img, face_front, 8, 8);
        makeFaceImage(img, face_left, 16, 8);
        makeFaceImage(img, face_right, 0, 8);
        makeFaceImage(img, face_back, 24, 8);
        makeFaceImage(img, face_top, 8, 0);
        makeFaceImage(img, face_bottom, 16, 0);
    }

    private void patchCustomImages(int img_id, int[] imgids, List<CustomTileRec> recs, int xcnt, int ycnt)
    {
        int mult = imgs[img_id].height / (ycnt * 16); /* Compute scale based on nominal tile count vertically (ycnt * 16) */
        for(int i = 0; i < imgids.length; i++) {
            if(imgids[i] <= 0) continue;
            CustomTileRec ctr = recs.get(i);
            if(ctr == null) continue;
            int[] tile = new int[16 * 16 * mult * mult];    /* Make image */
            copySubimageFromImage(img_id, ctr.srcx * mult, ctr.srcy * mult, ctr.targetx * mult, ctr.targety * mult, 
                    ctr.width * mult, ctr.height * mult, tile, 16 * mult);
            /* Put scaled result into tile buffer */
            int new_argb[] = new int[native_scale*native_scale];
            scaleTerrainPNGSubImage(16*mult, native_scale, tile, new_argb);
            setTileARGB(imgids[i], new_argb);
        }
    }

    /* Copy texture pack */
    private TexturePack(TexturePack tp) {
        this.tile_argb = Arrays.copyOf(tp.tile_argb, tp.tile_argb.length);
        this.native_scale = tp.native_scale;
        this.ctm = tp.ctm;
        this.imgs = tp.imgs;
        this.hasBlockColoring = tp.hasBlockColoring;
        this.blockColoring = tp.blockColoring;
    }
    
    /* Load terrain.png */
    private void loadTerrainPNG(InputStream is, boolean is_rp) throws IOException {
        int i, j;
        /* Load image */
        ImageIO.setUseCache(false);
        BufferedImage img = ImageIO.read(is);
        if(img == null) { throw new FileNotFoundException(); }
        tile_argb = new int[MAX_TILEINDEX][];
        /* If we're using pre 1.5 terrain.png */
        if(img.getWidth() >= 256) {
            native_scale = img.getWidth() / 16;
            blank = new int[native_scale*native_scale];
            for(i = 0; i < 256; i++) {
                int[] buf = new int[native_scale*native_scale];
                img.getRGB((i & 0xF)*native_scale, (i>>4)*native_scale, native_scale, native_scale, buf, 0, native_scale);
                setTileARGB(i, buf);
            }
            /* Now, load extra scaled images */
            for(i = 256; i < terrain_map.length; i++) {
                String fn = getBlockFileName(i);
                if (fn == null) continue;
                DynamicTileFile dtf = addonfilesbyname.get(fn);
                if (dtf == null) continue;
                LoadedImage li = imgs[dtf.idx + IMG_CNT];
                if(li != null) {
                    int[] buf = new int[native_scale * native_scale];
                    scaleTerrainPNGSubImage(li.width, native_scale, li.argb, buf);
                    setTileARGB(i, buf);
                }
            }
        }
        else if (is_rp) {   // If resource pack (1.6+)
            native_scale = 16;
            /* Loop through textures - find biggest one */
            for(i = 0; i < terrain_rp_map.length; i++) {
                String fn = getRPFileName(i);
                if (fn == null) continue;
                DynamicTileFile dtf = addonfilesbyname.get(fn);
                if (dtf == null) continue;
                LoadedImage li = imgs[dtf.idx+IMG_CNT];
                if(li != null) {
                    if(native_scale < li.width) native_scale = li.width;
                }
            }
            blank = new int[native_scale*native_scale];
            /* Now, load scaled images */
            for(i = 0; i < terrain_rp_map.length; i++) {
                String fn = getRPFileName(i);
                if (fn == null) continue;
                DynamicTileFile dtf = addonfilesbyname.get(fn);
                if (dtf == null) continue;
                LoadedImage li = imgs[dtf.idx + IMG_CNT];
                if(li != null) {
                    int[] buf =  new int[native_scale * native_scale];
                    scaleTerrainPNGSubImage(li.width, native_scale, li.argb, buf);
                    setTileARGB(i, buf);
                }
            }
        }
        else {  /* Else, use v1.5 tile files */
            native_scale = 16;
            /* Loop through textures - find biggest one */
            for(i = 0; i < terrain_map.length; i++) {
                String fn = getBlockFileName(i);
                if (fn == null) continue;
                DynamicTileFile dtf = addonfilesbyname.get(fn);
                if (dtf == null) continue;
                LoadedImage li = imgs[dtf.idx+IMG_CNT];
                if(li != null) {
                    if(native_scale < li.width) native_scale = li.width;
                }
            }
            blank = new int[native_scale*native_scale];
            /* Now, load scaled images */
            for(i = 0; i < terrain_map.length; i++) {
                String fn = getBlockFileName(i);
                if (fn == null) continue;
                DynamicTileFile dtf = addonfilesbyname.get(fn);
                if (dtf == null) continue;
                LoadedImage li = imgs[dtf.idx + IMG_CNT];
                if(li != null) {
                    int[] buf = new int[native_scale * native_scale];
                    scaleTerrainPNGSubImage(li.width, native_scale, li.argb, buf);
                    setTileARGB(i, buf);
                }
            }
        }
        /* Now, build redstone textures with active wire color (since we're not messing with that) */
        Color tc = new Color();
        int[] red_nsew_tone = getTileARGB(TILEINDEX_REDSTONE_NSEW_TONE);
        int[] red_nsew = getTileARGB(TILEINDEX_REDSTONE_NSEW);
        int[] red_ew_tone = getTileARGB(TILEINDEX_REDSTONE_EW_TONE);
        int[] red_ew = getTileARGB(TILEINDEX_REDSTONE_EW);
        
        for(i = 0; i < native_scale*native_scale; i++) {
            if(red_nsew_tone[i] != 0) {
                /* Overlay NSEW redstone texture with toned wire color */
                tc.setARGB(red_nsew_tone[i]);
                tc.blendColor(0xFFC00000);  /* Blend in red */
                red_nsew[i] = tc.getARGB();
            }
            if(red_ew_tone[i] != 0) {
                /* Overlay NSEW redstone texture with toned wire color */
                tc.setARGB(red_ew_tone[i]);
                tc.blendColor(0xFFC00000);  /* Blend in red */
                red_ew[i] = tc.getARGB();
            }
        }
        /* Build extended piston side texture - take top 1/4 of piston side, use to make piston extension */
        int[] buf = new int[native_scale*native_scale];
        setTileARGB(TILEINDEX_PISTONEXTSIDE, buf);
        int[] piston_side = getTileARGB(TILEINDEX_PISTONSIDE);
        System.arraycopy(piston_side, 0, buf, 0, native_scale * native_scale / 4);
        for(i = 0; i < native_scale/4; i++) {
            for(j = 0; j < (3*native_scale/4); j++) {
                buf[native_scale*(native_scale/4 + j) + (3*native_scale/8 + i)] = piston_side[native_scale*i + j];
            }
        }
        /* Build piston side while extended (cut off top 1/4, replace with rotated top for extension */
        buf = new int[native_scale*native_scale];
        setTileARGB(TILEINDEX_PISTONSIDE_EXT, buf); 
        System.arraycopy(piston_side, native_scale*native_scale/4, buf, native_scale*native_scale/4,
             3 * native_scale * native_scale / 4);  /* Copy bottom 3/4 */
        for(i = 0; i < native_scale/4; i++) {
            for(j = 3*native_scale/4; j < native_scale; j++) {
                buf[native_scale*(j - 3*native_scale/4) + (3*native_scale/8 + i)] =
                    piston_side[native_scale*i + j];
            }
        }
        /* Build glass pane top in NSEW config (we use model to clip it) */
        buf = new int[native_scale*native_scale];
        setTileARGB(TILEINDEX_PANETOP_X, buf); 
        int[] glasspanetop = getTileARGB(TILEINDEX_GLASSPANETOP);
        System.arraycopy(glasspanetop, 0, buf, 0, native_scale*native_scale);
        for(i = native_scale*7/16; i < native_scale*9/16; i++) {
            for(j = 0; j < native_scale; j++) {
                buf[native_scale*i + j] = buf[native_scale*j + i];
            }
        }
        /* Build air frame with eye overlay */
        buf = new int[native_scale*native_scale];
        setTileARGB(TILEINDEX_AIRFRAME_EYE, buf);
        int[] airframe = getTileARGB(TILEINDEX_AIRFRAME);
        int[] eyeofender = getTileARGB(TILEINDEX_EYEOFENDER);
        System.arraycopy(airframe, 0, buf, 0, native_scale*native_scale);
        for(i = native_scale/4; i < native_scale*3/4; i++) {
            for(j = native_scale/4; j < native_scale*3/4; j++) {
                buf[native_scale*i + j] = eyeofender[native_scale*i + j];
            }
        }
        /* Build white tile */
        buf = new int[native_scale*native_scale];
        setTileARGB(TILEINDEX_WHITE, buf);
        Arrays.fill(buf, 0xFFFFFFFF);
        
        img.flush();
    }
    
    /* Load image into image array */
    private void loadImage(InputStream is, int idx) throws IOException {
        BufferedImage img = null;
        /* Load image */
        if(is != null) {
            ImageIO.setUseCache(false);
            img = ImageIO.read(is);
            if(img == null) { throw new FileNotFoundException(); }
        }
        if(idx >= imgs.length) {
            LoadedImage[] newimgs = new LoadedImage[idx+1];
            System.arraycopy(imgs, 0, newimgs, 0, imgs.length);
            imgs = newimgs;
        }
        imgs[idx] = new LoadedImage();
        if (img != null) {
            imgs[idx].width = img.getWidth();
            imgs[idx].height = img.getHeight();
            imgs[idx].argb = new int[imgs[idx].width * imgs[idx].height];
            img.getRGB(0, 0, imgs[idx].width, imgs[idx].height, imgs[idx].argb, 0, imgs[idx].width);
            img.flush();
        }
        else {
            imgs[idx].width = 16;
            imgs[idx].height = 16;
            imgs[idx].argb = new int[imgs[idx].width * imgs[idx].height];
        }
    }
        

    /* Process dynamic texture files, and patch into terrain_argb */
    private void processDynamicImage(int idx, TileFileFormat format) {
        DynamicTileFile dtf = addonfiles.get(idx);  /* Get tile file definition */
        LoadedImage li = imgs[idx+IMG_CNT];
        if (li == null) return;
        
        switch(format) {
            case GRID:  /* If grid format tile file */
                int dim = li.width / dtf.tilecnt_x; /* Dimension of each tile */
                int old_argb[] = new int[dim*dim];
                for(int x = 0; x < dtf.tilecnt_x; x++) {
                    for(int y = 0; y < dtf.tilecnt_y; y++) {
                        int tileidx = dtf.tile_to_dyntile[y*dtf.tilecnt_x + x];
                        if (tileidx < 0) continue;
                        if((tileidx >= terrain_map.length) || (terrain_map[tileidx] == null)) {    /* dynamic ID? */
                            /* Copy source tile */
                            for(int j = 0; j < dim; j++) {
                                System.arraycopy(li.argb, (y*dim+j)*li.width + (x*dim), old_argb, j*dim, dim); 
                            }
                            /* Rescale to match rest of terrain PNG */
                            int new_argb[] = new int[native_scale*native_scale];
                            scaleTerrainPNGSubImage(dim, native_scale, old_argb, new_argb);
                            setTileARGB(tileidx, new_argb);
                        }
                    }
                }
                break;
            case CHEST:
                patchChestImages(idx+IMG_CNT, dtf.tile_to_dyntile[TILEINDEX_CHEST_TOP], dtf.tile_to_dyntile[TILEINDEX_CHEST_BOTTOM], dtf.tile_to_dyntile[TILEINDEX_CHEST_FRONT], dtf.tile_to_dyntile[TILEINDEX_CHEST_BACK], dtf.tile_to_dyntile[TILEINDEX_CHEST_LEFT], dtf.tile_to_dyntile[TILEINDEX_CHEST_RIGHT]);
                break;
            case BIGCHEST:
                patchLargeChestImages(idx+IMG_CNT, dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_TOPRIGHT], dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_TOPLEFT], dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_BOTTOMRIGHT], dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_BOTTOMLEFT], dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_RIGHT], dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_LEFT], dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_FRONTRIGHT], dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_FRONTLEFT], dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_BACKRIGHT], dtf.tile_to_dyntile[TILEINDEX_BIGCHEST_BACKLEFT]);
                break;
            case SIGN:
                patchSignImages(idx+IMG_CNT, dtf.tile_to_dyntile[TILEINDEX_SIGN_FRONT], dtf.tile_to_dyntile[TILEINDEX_SIGN_BACK], dtf.tile_to_dyntile[TILEINDEX_SIGN_TOP], dtf.tile_to_dyntile[TILEINDEX_SIGN_BOTTOM], dtf.tile_to_dyntile[TILEINDEX_SIGN_LEFTSIDE], dtf.tile_to_dyntile[TILEINDEX_SIGN_RIGHTSIDE], dtf.tile_to_dyntile[TILEINDEX_SIGN_POSTFRONT], dtf.tile_to_dyntile[TILEINDEX_SIGN_POSTBACK], dtf.tile_to_dyntile[TILEINDEX_SIGN_POSTLEFT], dtf.tile_to_dyntile[TILEINDEX_SIGN_POSTRIGHT]);
                break;
            case SKIN:
                patchSkinImages(idx+IMG_CNT, dtf.tile_to_dyntile[TILEINDEX_SKIN_FACEFRONT], dtf.tile_to_dyntile[TILEINDEX_SKIN_FACELEFT], dtf.tile_to_dyntile[TILEINDEX_SKIN_FACERIGHT], dtf.tile_to_dyntile[TILEINDEX_SKIN_FACEBACK], dtf.tile_to_dyntile[TILEINDEX_SKIN_FACETOP], dtf.tile_to_dyntile[TILEINDEX_SKIN_FACEBOTTOM]);
                break;
            case CUSTOM:
                patchCustomImages(idx+IMG_CNT, dtf.tile_to_dyntile, dtf.cust, dtf.tilecnt_x, dtf.tilecnt_y);
                break;
            case TILESET:
                break;
            default:
                break;
        }
    }
    /* Load biome shading image into image array */
    private void loadBiomeShadingImage(InputStream is, int idx) throws IOException {
        loadImage(is, idx); /* Get image */
        LoadedImage li = imgs[idx];
        if (li.width != 256) {  /* Required to be 256 x 256 */
            int[] scaled = new int[256*256];
            scaleTerrainPNGSubImage(li.width, 256, li.argb, scaled);
            li.argb = scaled;
            li.width = 256;
            li.height = 256;
        }
        /* Get trivial color for biome-shading image */
        int clr = li.argb[li.height*li.width*3/4 + li.width/2];
        boolean same = true;
        for(int j = 0; same && (j < li.height); j++) {
            for(int i = 0; same && (i <= j); i++) {
                if(li.argb[li.width*j+i] != clr)
                    same = false;
            }
        }
        /* All the same - no biome lookup needed */
        if(same) {
            li.trivial_color = clr;
        }
        else {  /* Else, calculate color average for lower left quadrant */
            int[] clr_scale = new int[16];
            scaleTerrainPNGSubImage(li.width, 4, li.argb, clr_scale);
            li.trivial_color = clr_scale[9];
        }
    }
    
    /* Patch image into texture table */
    private void patchTextureWithImage(int image_idx, int block_idx) {
        /* Now, patch in to block table */
        int new_argb[] = new int[native_scale*native_scale];
        scaleTerrainPNGSubImage(imgs[image_idx].width, native_scale, imgs[image_idx].argb, new_argb);
        setTileARGB(block_idx, new_argb);
        
    }

    /* Get texture pack directory */
    private static File getTexturePackDirectory(DynmapCore core) {
        return new File(core.getDataFolder(), "texturepacks");
    }

    /**
     * Resample terrain pack for given scale, and return copy using that scale
     */
    public TexturePack resampleTexturePack(int scale) {
        synchronized(scaledlock) {
            if(scaled_textures == null) scaled_textures = new HashMap<Integer, TexturePack>();
            TexturePack stp = scaled_textures.get(scale);
            if(stp != null)
                return stp;
            stp = new TexturePack(this);    /* Make copy */
            /* Scale terrain.png, if needed */
            if(stp.native_scale != scale) {
                stp.native_scale = scale;
                scaleTerrainPNG(stp);
            }
            /* Remember it */
            scaled_textures.put(scale, stp);
            return stp;
        }
    }
    /**
     * Scale our terrain_argb into the terrain_argb of the provided destination, matching the scale of that destination
     * @param tp
     */
    private void scaleTerrainPNG(TexturePack tp) {
        tp.tile_argb = new int[tile_argb.length][];
        /* Terrain.png is 16x16 array of images : process one at a time */
        for(int idx = 0; idx < tile_argb.length; idx++) {
            tp.tile_argb[idx] = new int[tp.native_scale*tp.native_scale];
            scaleTerrainPNGSubImage(native_scale, tp.native_scale, getTileARGB(idx),  tp.tile_argb[idx]);
        }
        /* Special case - some textures are used as masks - need pure alpha (00 or FF) */
        makeAlphaPure(tp.tile_argb[TILEINDEX_GRASSMASK]); /* Grass side mask */
    }
    public static void scaleTerrainPNGSubImage(int srcscale, int destscale, int[] src_argb, int[] dest_argb) {
        int nativeres = srcscale;
        int res = destscale;
        Color c = new Color();
        /* Same size, so just copy */
        if(res == nativeres) {
            System.arraycopy(src_argb, 0, dest_argb, 0, dest_argb.length);
        }
        /* If we're scaling larger source pixels into smaller pixels, each destination pixel
         * receives input from 1 or 2 source pixels on each axis
         */
        else if(res > nativeres) {
            int weights[] = new int[res];
            int offsets[] = new int[res];
            /* LCM of resolutions is used as length of line (res * nativeres)
             * Each native block is (res) long, each scaled block is (nativeres) long
             * Each scaled block overlaps 1 or 2 native blocks: starting with native block 'offsets[]' with
             * 'weights[]' of its (res) width in the first, and the rest in the second
             */
            for(int v = 0, idx = 0; v < res*nativeres; v += nativeres, idx++) {
                offsets[idx] = (v/res); /* Get index of the first native block we draw from */
                if((v+nativeres-1)/res == offsets[idx]) {   /* If scaled block ends in same native block */
                    weights[idx] = nativeres;
                }
                else {  /* Else, see how much is in first one */
                    weights[idx] = (offsets[idx]*res + res) - v;
                }
            }
            /* Now, use weights and indices to fill in scaled map */
            for(int y = 0; y < res; y++) {
                int ind_y = offsets[y];
                int wgt_y = weights[y];
                for(int x = 0; x < res; x++) {
                    int ind_x = offsets[x];
                    int wgt_x = weights[x];
                    double accum_red = 0;
                    double accum_green = 0;
                    double accum_blue = 0;
                    double accum_alpha = 0;
                    for(int xx = 0; xx < 2; xx++) {
                        int wx = (xx==0)?wgt_x:(nativeres-wgt_x);
                        if(wx == 0) continue;
                        for(int yy = 0; yy < 2; yy++) {
                            int wy = (yy==0)?wgt_y:(nativeres-wgt_y);
                            if(wy == 0) continue;
                            /* Accumulate */
                            c.setARGB(src_argb[(ind_y+yy)*nativeres + ind_x + xx]);
                            int w = wx * wy;
                            double a = (double)w * (double)c.getAlpha();
                            accum_red += c.getRed() * a;
                            accum_green += c.getGreen() * a;
                            accum_blue += c.getBlue() * a;
                            accum_alpha += a;
                        }
                    }
                    double newalpha = accum_alpha;
                    if(newalpha == 0.0) newalpha = 1.0;
                    /* Generate weighted compnents into color */
                    c.setRGBA((int)(accum_red / newalpha), (int)(accum_green / newalpha), 
                              (int)(accum_blue / newalpha), (int)(accum_alpha / (nativeres*nativeres)));
                    dest_argb[(y*res) + x] = c.getARGB();
                }
            }
        }
        else {  /* nativeres > res */
            int weights[] = new int[nativeres];
            int offsets[] = new int[nativeres];
            /* LCM of resolutions is used as length of line (res * nativeres)
             * Each native block is (res) long, each scaled block is (nativeres) long
             * Each native block overlaps 1 or 2 scaled blocks: starting with scaled block 'offsets[]' with
             * 'weights[]' of its (res) width in the first, and the rest in the second
             */
            for(int v = 0, idx = 0; v < res*nativeres; v += res, idx++) {
                offsets[idx] = (v/nativeres); /* Get index of the first scaled block we draw to */
                if((v+res-1)/nativeres == offsets[idx]) {   /* If native block ends in same scaled block */
                    weights[idx] = res;
                }
                else {  /* Else, see how much is in first one */
                    weights[idx] = (offsets[idx]*nativeres + nativeres) - v;
                }
            }
            double accum_red[] = new double[res*res];
            double accum_green[] = new double[res*res];
            double accum_blue[] = new double[res*res];
            double accum_alpha[] = new double[res*res];
            
            /* Now, use weights and indices to fill in scaled map */
            for(int y = 0; y < nativeres; y++) {
                int ind_y = offsets[y];
                int wgt_y = weights[y];
                for(int x = 0; x < nativeres; x++) {
                    int ind_x = offsets[x];
                    int wgt_x = weights[x];
                    c.setARGB(src_argb[(y*nativeres) + x]);
                    for(int xx = 0; xx < 2; xx++) {
                        int wx = (xx==0)?wgt_x:(res-wgt_x);
                        if(wx == 0) continue;
                        for(int yy = 0; yy < 2; yy++) {
                            int wy = (yy==0)?wgt_y:(res-wgt_y);
                            if(wy == 0) continue;
                            double w = wx * wy;
                            double a = w * c.getAlpha();
                            accum_red[(ind_y+yy)*res + (ind_x+xx)] += c.getRed() * a;
                            accum_green[(ind_y+yy)*res + (ind_x+xx)] += c.getGreen() * a;
                            accum_blue[(ind_y+yy)*res + (ind_x+xx)] += c.getBlue() * a;
                            accum_alpha[(ind_y+yy)*res + (ind_x+xx)] += a;
                        }
                    }
                }
            }
            /* Produce normalized scaled values */
            for(int y = 0; y < res; y++) {
                for(int x = 0; x < res; x++) {
                    int off = (y*res) + x;
                    double aa = accum_alpha[off];
                    if(aa == 0.0) aa = 1.0;
                    c.setRGBA((int)(accum_red[off]/aa), (int)(accum_green[off]/aa),
                          (int)(accum_blue[off]/aa), (int)(accum_alpha[off] / (nativeres*nativeres)));
                    dest_argb[y*res + x] = c.getARGB();
                }
            }
        }
    }
    private static void addFiles(List<String> tsfiles, List<String> txfiles, File dir, String path) {
        File[] listfiles = dir.listFiles();
        if(listfiles == null) return;
        for(File f : listfiles) {
            String fn = f.getName();
            if(fn.equals(".") || (fn.equals(".."))) continue;
            if(f.isFile()) {
                if(fn.endsWith("-texture.txt")) {
                    txfiles.add(path + fn);
                }
                if(fn.endsWith("-tilesets.txt")) {
                    tsfiles.add(path + fn);
                }
            }
            else if(f.isDirectory()) {
                addFiles(tsfiles, txfiles, f, path + f.getName() + "/");
            }
        }
    }
    /**
     * Load texture pack mappings
     */
    public static void loadTextureMapping(DynmapCore core, ConfigurationNode config) {
        File datadir = core.getDataFolder();
        /* Start clean with texture packs - need to be loaded after mapping */
        resetFiles(core);
        /* Initialize map with blank map for all entries */
        HDTextureMap.initializeTable();
        /* Load block textures (0-N) */
        int i = 0;
        boolean done = false;
        InputStream in = null;
        while (!done) {
            in = TexturePack.class.getResourceAsStream("/texture_" + i + ".txt");
            if(in != null) {
                loadTextureFile(in, "texture_" + i + ".txt", config, core, "core");
                if(in != null) { try { in.close(); } catch (IOException x) {} in = null; }
            }
            else {
                done = true;
            }
            i++;
        }
        File renderdir = new File(datadir, "renderdata");
        ArrayList<String> tsfiles = new ArrayList<String>();
        ArrayList<String> txfiles = new ArrayList<String>();
        addFiles(tsfiles, txfiles, renderdir, "");
        for(String fname : tsfiles) {
            File custom = new File(renderdir, fname);
            if(custom.canRead()) {
                try {
                    in = new FileInputStream(custom);
                    loadTileSetsFile(in, custom.getPath(), config, core, fname.substring(0,  fname.indexOf("-tilesets.txt")));
                } catch (IOException iox) {
                    Log.severe("Error loading " + custom.getPath() + " - " + iox);
                } finally {
                    if(in != null) { try { in.close(); } catch (IOException x) {} in = null; }
                }
            }
        }
        for(String fname : txfiles) {
            File custom = new File(renderdir, fname);
            if(custom.canRead()) {
                try {
                    in = new FileInputStream(custom);
                    loadTextureFile(in, custom.getPath(), config, core, fname.substring(0,  fname.indexOf("-texture.txt")));
                } catch (IOException iox) {
                    Log.severe("Error loading " + custom.getPath() + " - " + iox);
                } finally {
                    if(in != null) { try { in.close(); } catch (IOException x) {} in = null; }
                }
            }
        }
        /* Finish processing of texture maps */
        processTextureMaps();
        /* Check integrity of texture mappings versus models */
        for(int blkiddata = 0; blkiddata < HDTextureMap.texmaps.length; blkiddata++) {
            int blkid = (blkiddata >> 4);
            int blkdata = blkiddata & 0xF;
            HDTextureMap tm = HDTextureMap.texmaps[blkiddata];
            int cnt = HDBlockModels.getNeededTextureCount(blkid, blkdata);
            if(cnt > tm.faces.length){
                Log.severe("Block ID " + blkid + ":" + blkdata + " - not enough textures for faces (" + cnt + " > " + tm.faces.length + ")");
                int[] newfaces = new int[cnt];
                System.arraycopy(tm.faces, 0, newfaces, 0, tm.faces.length);
                for(i = tm.faces.length; i < cnt; i++) {
                    newfaces[i] = TILEINDEX_BLANK;
                }
            }
        }
    }

    private static Integer getIntValue(Map<String,Integer> vars, String val) throws NumberFormatException {
        if(Character.isLetter(val.charAt(0))) {
            int off = val.indexOf('+');
            int offset = 0;
            if (off > 0) {
                offset = Integer.valueOf(val.substring(off+1));
                val = val.substring(0,  off);
            }
            Integer v = vars.get(val);
            if(v == null)
                throw new NumberFormatException("invalid ID - " + val);
            if((offset != 0) && (v.intValue() > 0))
                v = v.intValue() + offset;
            return v;
        }
        else {
            return Integer.valueOf(val);
        }
    }

    private static int parseTextureIndex(HashMap<String,Integer> filetoidx, int srctxtid, String val) throws NumberFormatException {
        int off = val.indexOf(':');
        int txtid = -1;
        if(off > 0) {
            String txt = val.substring(off+1);
            if(filetoidx.containsKey(txt)) {
                srctxtid = filetoidx.get(txt);
            }
            else {
                throw new NumberFormatException("Unknown attribute: " + txt);
            }
            txtid = Integer.valueOf(val.substring(0, off));
        }
        else {
            txtid = Integer.valueOf(val);
        }
        /* Shift function code from x1000 to x1000000 for internal processing */
        int funcid = (txtid / COLORMOD_MULT_FILE);
        txtid = txtid - (COLORMOD_MULT_FILE * funcid);
        /* If we have source texture, need to map values to dynamic ids */
        if((srctxtid >= 0) && (txtid >= 0)) {
            /* Map to assigned ID in global tile table: preserve modifier */
            txtid =findOrAddDynamicTile(srctxtid, txtid); 
        }
        if(srctxtid == TXTID_INVALID) {
            throw new NumberFormatException("Invalid texture ID: no default terrain.png: " + val);
        }
        return txtid + (COLORMOD_MULT_INTERNAL * funcid);
    }
    /**
     * Load texture pack mappings from tilesets.txt file
     */
    private static void loadTileSetsFile(InputStream txtfile, String txtname, ConfigurationNode config, DynmapCore core, String blockset) {
        LineNumberReader rdr = null;
        DynamicTileFile tfile = null;
        
        try {
            String line;
            rdr = new LineNumberReader(new InputStreamReader(txtfile));
            while((line = rdr.readLine()) != null) {
                if(line.startsWith("#")) {
                }
                else if(line.startsWith("tileset:")) { /* Start of tileset definition */
                    line = line.substring(line.indexOf(':')+1);
                    int xdim = 16, ydim = 16;
                    String fname = null;
                    String setdir = null;
                    String[] toks = line.split(",");
                    for(String tok : toks) {
                        String[] v = tok.split("=");
                        if(v.length < 2) continue;
                        if(v[0].equals("xcount")) {
                            xdim = Integer.parseInt(v[1]);
                        }
                        else if(v[0].equals("ycount")) {
                            ydim = Integer.parseInt(v[1]);
                        }
                        else if(v[0].equals("setdir")) {
                            setdir = v[1];
                        }
                        else if(v[0].equals("filename")) {
                            fname = v[1];
                        }
                    }
                    if ((fname != null) && (setdir != null)) {
                        /* Register tile file */
                        int fid = findOrAddDynamicTileFile(fname, null, xdim, ydim, TileFileFormat.TILESET, new String[0]);
                        tfile = addonfiles.get(fid);
                        if (tfile == null) {
                            Log.severe("Error registering tile set " + fname + " at " + rdr.getLineNumber() + " of " + txtname);
                            return;
                        }
                        /* Initialize tile name map and set directory path */
                        tfile.tilenames = new String[tfile.tile_to_dyntile.length];
                    }
                    else {
                        Log.severe("Error defining tile set at " + rdr.getLineNumber() + " of " + txtname);
                        return;
                    }
                }
                else if(Character.isDigit(line.charAt(0))) {    /* Starts with digit?  tile mapping */
                    int split = line.indexOf('-');  /* Find first dash */
                    if(split < 0) continue;
                    String id = line.substring(0, split).trim();
                    String name = line.substring(split+1).trim();
                    String[] coord = id.split(",");
                    int idx = -1;
                    if(coord.length == 2) { /* If x,y */
                        idx = (Integer.parseInt(coord[1]) * tfile.tilecnt_x) + Integer.parseInt(coord[0]);
                    }
                    else if(coord.length == 1) { /* Just index */
                        idx = Integer.parseInt(coord[0]);
                    }
                    if((idx >= 0) && (idx < tfile.tilenames.length)) {
                        tfile.tilenames[idx] = name;
                    }
                    else {
                        Log.severe("Bad tile index - line " + rdr.getLineNumber() + " of " + txtname);
                    }
                }
            }
        } catch (IOException iox) {
            Log.severe("Error reading " + txtname + " - " + iox.toString());
        } catch (NumberFormatException nfx) {
            Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname + ": " + nfx.getMessage());
        } finally {
            if(rdr != null) {
                try {
                    rdr.close();
                    rdr = null;
                } catch (IOException e) {
                }
            }
        }
    }
    private static final int TXTID_INVALID = -2;
    private static final int TXTID_TERRAINPNG = -1;
    /**
     * Load texture pack mappings from texture.txt file
     */
    private static void loadTextureFile(InputStream txtfile, String txtname, ConfigurationNode config, DynmapCore core, String blockset) {
        LineNumberReader rdr = null;
        int cnt = 0;
        HashMap<String,Integer> filetoidx = new HashMap<String,Integer>();
        HashMap<String,Integer> varvals = new HashMap<String,Integer>();
        final String mcver = core.getDynmapPluginPlatformVersion();
        boolean mod_cfg_needed = false;
        String modname = null;
        String modversion = null;
        String texturemod = null;
        String texturepath = null;
        boolean terrain_ok = true;
        try {
            String line;
            rdr = new LineNumberReader(new InputStreamReader(txtfile));
            while((line = rdr.readLine()) != null) {
                boolean skip = false;
                if ((line.length() > 0) && (line.charAt(0) == '[')) {    // If version constrained like
                    int end = line.indexOf(']');    // Find end
                    if (end < 0) {
                        Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname + ": bad version limit");
                        return;
                    }
                    String vertst = line.substring(1, end);
                    String tver = mcver;
                    if (vertst.startsWith("mod:")) {    // If mod version ranged
                        tver = modversion;
                        vertst = vertst.substring(4);
                    }
                    if (!HDBlockModels.checkVersionRange(tver, vertst)) {
                        skip = true;
                    }
                    line = line.substring(end+1);
                }
                // If we're skipping due to version restriction
                if (skip) {
                }
                else if(line.startsWith("block:")) {
                    ArrayList<Integer> blkids = new ArrayList<Integer>();
                    int databits = -1;
                    int srctxtid = TXTID_TERRAINPNG;
                    if (!terrain_ok)
                        srctxtid = TXTID_INVALID;  // Mark as not usable
                    int faces[] = new int[] { TILEINDEX_BLANK, TILEINDEX_BLANK, TILEINDEX_BLANK, TILEINDEX_BLANK, TILEINDEX_BLANK, TILEINDEX_BLANK };
                    int txtidx[] = new int[] { -1, -1, -1, -1, -1, -1 };
                    byte layers[] = null;
                    line = line.substring(6);
                    BlockTransparency trans = BlockTransparency.OPAQUE;
                    int colorMult = 0;
                    boolean stdrot = false; // Legacy top/bottom rotation
                    CustomColorMultiplier custColorMult = null;
                    String[] args = line.split(",");
                    for(String a : args) {
                        String[] av = a.split("=");
                        if(av.length < 2) continue;
                        else if(av[0].equals("txtid")) {
                            if(filetoidx.containsKey(av[1]))
                                srctxtid = filetoidx.get(av[1]);
                            else
                                Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname + ": bad texture " + av[1]);
                        }
                    }
                    boolean userenderdata = false;
                    for(String a : args) {
                        String[] av = a.split("=");
                        if(av.length < 2) continue;
                        if(av[0].equals("id")) {
                            blkids.add(getIntValue(varvals, av[1]));
                        }
                        else if(av[0].equals("data")) {
                            if(databits < 0) databits = 0;
                            if(av[1].equals("*"))
                                databits = 0xFFFF;
                            else
                                databits |= (1 << getIntValue(varvals,av[1]));
                        }

                        else if(av[0].equals("top") || av[0].equals("y-") || av[0].equals("face1")) {
                            faces[BlockStep.Y_MINUS.ordinal()] = parseTextureIndex(filetoidx, srctxtid, av[1]);
                        }
                        else if(av[0].equals("bottom") || av[0].equals("y+") || av[0].equals("face0")) {
                            faces[BlockStep.Y_PLUS.ordinal()] = parseTextureIndex(filetoidx, srctxtid, av[1]);
                        }
                        else if(av[0].equals("north") || av[0].equals("x+") || av[0].equals("face4")) {
                            faces[BlockStep.X_PLUS.ordinal()] = parseTextureIndex(filetoidx, srctxtid, av[1]);
                        }
                        else if(av[0].equals("south") || av[0].equals("x-") || av[0].equals("face5")) {
                            faces[BlockStep.X_MINUS.ordinal()] = parseTextureIndex(filetoidx, srctxtid, av[1]);
                        }
                        else if(av[0].equals("west") || av[0].equals("z-") || av[0].equals("face3")) {
                            faces[BlockStep.Z_MINUS.ordinal()] = parseTextureIndex(filetoidx, srctxtid, av[1]);
                        }
                        else if(av[0].equals("east") || av[0].equals("z+") || av[0].equals("face2")) {
                            faces[BlockStep.Z_PLUS.ordinal()] = parseTextureIndex(filetoidx, srctxtid, av[1]);
                        }
                        else if(av[0].equals("allfaces")) {
                            int id = parseTextureIndex(filetoidx, srctxtid, av[1]);
                            for(int i = 0; i < 6; i++) {
                                faces[i] = id;
                            }
                        }
                        else if(av[0].equals("allsides")) {
                            int id = parseTextureIndex(filetoidx, srctxtid, av[1]);
                            faces[BlockStep.X_PLUS.ordinal()] = id;
                            faces[BlockStep.X_MINUS.ordinal()] = id;
                            faces[BlockStep.Z_PLUS.ordinal()] = id;
                            faces[BlockStep.Z_MINUS.ordinal()] = id;
                        }
                        else if(av[0].equals("topbottom")) {
                            faces[BlockStep.Y_MINUS.ordinal()] = 
                                faces[BlockStep.Y_PLUS.ordinal()] = parseTextureIndex(filetoidx, srctxtid, av[1]);
                        }
                        else if(av[0].startsWith("patch")) {
                            int patchid0, patchid1;
                            String idrange = av[0].substring(5);
                            String[] ids = idrange.split("-");
                            if(ids.length > 1) {
                                patchid0 = Integer.parseInt(ids[0]);
                                patchid1 = Integer.parseInt(ids[1]);
                            }
                            else {
                                patchid0 = patchid1 = Integer.parseInt(ids[0]);
                            }
                            if((patchid0 < 0) || (patchid1 < patchid0)) {
                                Log.severe("Texture mapping has invalid patch index - " + av[1] + " - line " + rdr.getLineNumber() + " of " + txtname);
                                return;
                            }
                            if(faces.length <= patchid1) {
                                int[] newfaces = new int[patchid1+1];
                                Arrays.fill(newfaces, TILEINDEX_BLANK);
                                System.arraycopy(faces, 0, newfaces, 0, faces.length);
                                faces = newfaces;
                                int[] newtxtidx = new int[patchid1+1];
                                Arrays.fill(newtxtidx, -1);
                                System.arraycopy(txtidx, 0, newtxtidx, 0, txtidx.length);
                                txtidx = newtxtidx;
                            }
                            int txtid = parseTextureIndex(filetoidx, srctxtid, av[1]);
                            for(int i = patchid0; i <= patchid1; i++) {
                                faces[i] = txtid;
                            }
                        }
                        else if(av[0].equals("transparency")) {
                            trans = BlockTransparency.valueOf(av[1]);
                            if(trans == null) {
                                trans = BlockTransparency.OPAQUE;
                                Log.severe("Texture mapping has invalid transparency setting - " + av[1] + " - line " + rdr.getLineNumber() + " of " + txtname);
                            }
                            /* For leaves, base on leaf transparency setting */
                            if(trans == BlockTransparency.LEAVES) {
                                if(core.getLeafTransparency())
                                    trans = BlockTransparency.TRANSPARENT;
                                else
                                    trans = BlockTransparency.OPAQUE;
                            }
                            /* If no water lighting fix */
                            if((blkids.contains(8) || blkids.contains(9)) && (HDMapManager.waterlightingfix == false)) {
                                trans = BlockTransparency.TRANSPARENT;  /* Treat water as transparent if no fix */
                            }
                        }
                        else if(av[0].equals("userenderdata")) {
                    		userenderdata = av[1].equals("true");
                        }
                        else if(av[0].equals("colorMult")) {
                            colorMult = (int)Long.parseLong(av[1], 16);
                        }
                        else if(av[0].equals("custColorMult")) {
                            try {
                                Class<?> cls = Class.forName(av[1]);
                                custColorMult = (CustomColorMultiplier)cls.newInstance();
                            } catch (Exception x) {
                                Log.severe("Error loading custom color multiplier - " + av[1] + ": " + x.getMessage());
                            }
                        }
                        else if(av[0].equals("stdrot")) {
                            stdrot = av[1].equals("true");
                        }
                    }
                    for(String a : args) {
                        String[] av = a.split("=");
                        if(av.length < 2) continue;
                        if(av[0].startsWith("layer")) {
                            if(layers == null) {
                                layers = new byte[faces.length];
                                Arrays.fill(layers, (byte)-1);
                            }
                            String v[] = av[0].substring(5).split("-");
                            int id1, id2;
                            id1 = id2 = Integer.parseInt(v[0]);
                            if(v.length > 1) {
                                id2 = Integer.parseInt(v[1]);
                            }
                            byte val = (byte)Integer.parseInt(av[1]);
                            for(; id1 <= id2; id1++) {
                                layers[id1] = val;
                            }
                        }
                    }
                    /* If no data bits, assume all */
                    if(databits < 0) databits = 0xFFFF;
                    /* If we have everything, build block */
                    if(blkids.size() > 0) {
                        HDTextureMap map = new HDTextureMap(blkids, databits, faces, layers, trans, userenderdata, colorMult, custColorMult, blockset, stdrot);
                        map.addToTable();
                        cnt++;
                    }
                    else {
                        Log.severe("Texture mapping missing required parameters = line " + rdr.getLineNumber() + " of " + txtname);
                    }
                }
                else if(line.startsWith("addtotexturemap:")) {
                    int srctxtid = -1;
                    String mapid = null;
                    line = line.substring(line.indexOf(':') + 1);
                    String[] args = line.split(",");
                    for(String a : args) {
                        String[] av = a.split("=");
                        if(av.length < 2) continue;
                        else if(av[0].equals("txtid")) {
                            if(filetoidx.containsKey(av[1]))
                                srctxtid = filetoidx.get(av[1]);
                            else
                                Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname);
                        }
                        else if(av[0].equals("mapid")) {
                            mapid = av[1];
                        }
                    }
                    if(mapid != null) {
                        for(String a : args) {
                            String[] av = a.split("=");
                            if(av.length < 2) continue;
                            if(av[0].startsWith("key:")) {
                                addTextureByKey(mapid, getIntValue(varvals, av[0].substring(4)), 
                                        parseTextureIndex(filetoidx, srctxtid, av[1]));
                            }
                        }
                    }
                    else {
                        Log.severe("Missing mapid  - line " + rdr.getLineNumber() + " of " + txtname);
                    }
                }
                else if(line.startsWith("texturemap:")) {
                    ArrayList<Integer> blkids = new ArrayList<Integer>();
                    int databits = -1;
                    String mapid = null;
                    line = line.substring(line.indexOf(':') + 1);
                    BlockTransparency trans = BlockTransparency.OPAQUE;
                    int colorMult = 0;
                    CustomColorMultiplier custColorMult = null;
                    String[] args = line.split(",");
                    boolean userenderdata = false;
                    for(String a : args) {
                        String[] av = a.split("=");
                        if(av.length < 2) continue;
                        if(av[0].equals("id")) {
                            blkids.add(getIntValue(varvals, av[1]));
                        }
                        else if(av[0].equals("mapid")) {
                            mapid = av[1];
                        }
                        else if(av[0].equals("data")) {
                            if(databits < 0) databits = 0;
                            if(av[1].equals("*"))
                                databits = 0xFFFF;
                            else
                                databits |= (1 << getIntValue(varvals,av[1]));
                        }
                        else if(av[0].equals("transparency")) {
                            trans = BlockTransparency.valueOf(av[1]);
                            if(trans == null) {
                                trans = BlockTransparency.OPAQUE;
                                Log.severe("Texture mapping has invalid transparency setting - " + av[1] + " - line " + rdr.getLineNumber() + " of " + txtname);
                            }
                            /* For leaves, base on leaf transparency setting */
                            if(trans == BlockTransparency.LEAVES) {
                                if(core.getLeafTransparency())
                                    trans = BlockTransparency.TRANSPARENT;
                                else
                                    trans = BlockTransparency.OPAQUE;
                            }
                            /* If no water lighting fix */
                            if((blkids.contains(8) || blkids.contains(9)) && (HDMapManager.waterlightingfix == false)) {
                                trans = BlockTransparency.TRANSPARENT;  /* Treat water as transparent if no fix */
                            }
                        }
                        else if(av[0].equals("userenderdata")) {
                            userenderdata = av[1].equals("true");
                        }
                        else if(av[0].equals("colorMult")) {
                            colorMult = Integer.valueOf(av[1], 16);
                        }
                        else if(av[0].equals("custColorMult")) {
                            try {
                                Class<?> cls = Class.forName(av[1]);
                                custColorMult = (CustomColorMultiplier)cls.newInstance();
                            } catch (Exception x) {
                                Log.severe("Error loading custom color multiplier - " + av[1] + ": " + x.getMessage());
                            }
                        }
                    }
                    /* If no data bits, assume all */
                    if(databits < 0) databits = 0xFFFF;
                    /* If we have everything, build texture map */
                    if((blkids.size() > 0) && (mapid != null)) {
                        addTextureIndex(mapid, blkids, databits, trans, userenderdata, colorMult, custColorMult, blockset);
                    }
                    else {
                        Log.severe("Texture map missing required parameters = line " + rdr.getLineNumber() + " of " + txtname);
                    }
                }
                else if(line.startsWith("texturefile:") || line.startsWith("texture:")) {
                    boolean istxt = line.startsWith("texture:");
                    line = line.substring(line.indexOf(':')+1);
                    String[] args = line.split(",");
                    int xdim = 16, ydim = 16;
                    String fname = null;
                    String id = null;
                    TileFileFormat fmt = TileFileFormat.GRID;
                    if(istxt) {
                        xdim = ydim = 1;
                        fmt = TileFileFormat.GRID;
                    }
                    for(String arg : args) {
                        String[] aval = arg.split("=");
                        if(aval.length < 2)
                            continue;
                        if(aval[0].equals("id")) {
                            id = aval[1];
                            if (fname == null) {
                                if (texturepath != null) {
                                    fname = texturepath + id + ".png";
                                }
                                else if (texturemod != null) {
                                    fname = "mods/" + texturemod + "/textures/blocks/" + id + ".png";
                                }
                            }
                        }
                        else if(aval[0].equals("filename"))
                            fname = aval[1];
                        else if(aval[0].equals("xcount"))
                            xdim = Integer.parseInt(aval[1]);
                        else if(aval[0].equals("ycount"))
                            ydim = Integer.parseInt(aval[1]);
                        else if(aval[0].equals("format")) {
                            fmt = TileFileFormat.valueOf(aval[1].toUpperCase());
                            if(fmt == null) {
                                Log.severe("Invalid format type " + aval[1] + " - line " + rdr.getLineNumber() + " of " + txtname);
                                return;
                            }
                        }
                    }
                    if((fname != null) && (id != null)) {
                        /* Register the file */
                        int fid = findOrAddDynamicTileFile(fname, modname, xdim, ydim, fmt, args);
                        filetoidx.put(id, fid); /* Save lookup */
                    }
                    else {
                        Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname);
                        return;
                    }
                }
                else if(line.startsWith("#") || line.startsWith(";")) {
                }
                else if(line.startsWith("enabled:")) {  /* Test if texture file is enabled */
                    line = line.substring(8).trim();
                    if(line.startsWith("true")) {   /* We're enabled? */
                        /* Nothing to do - keep processing */
                    }
                    else if(line.startsWith("false")) { /* Disabled */
                        return; /* Quit */
                    }
                    /* If setting is not defined or false, quit */
                    else if(config.getBoolean(line, false) == false) {
                        return;
                    }
                    else {
                        Log.info(line + " textures enabled");
                    }
                }
                else if(line.startsWith("var:")) {  /* Test if variable declaration */
                    line = line.substring(4).trim();
                    String args[] = line.split(",");
                    for(int i = 0; i < args.length; i++) {
                        String[] v = args[i].split("=");
                        if(v.length < 2) {
                            Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname);
                            return;
                        }
                        try {
                            int val = Integer.valueOf(v[1]);    /* Parse default value */
                            int parmval = config.getInteger(v[0], val); /* Read value, with applied default */
                            varvals.put(v[0], parmval); /* And save value */
                        } catch (NumberFormatException nfx) {
                            Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname + ": " + nfx.getMessage());
                            return;
                        }
                    }
                }
                else if(line.startsWith("cfgfile:")) { /* If config file */
                    mod_cfg_needed = true;
                    File cfgfile = new File(line.substring(8).trim());
                    ForgeConfigFile cfg = new ForgeConfigFile(cfgfile);
                    if(cfg.load()) {
                        cfg.addBlockIDs(varvals);
                        mod_cfg_needed = false;
                    }
                }
                else if(line.startsWith("modname:")) {
                    String[] names = line.substring(8).split(",");
                    boolean found = false;
                    for(String n : names) {
                        String[] ntok = n.split("[\\[\\]]");
                        String rng = null;
                        if (ntok.length > 1) {
                            n = ntok[0].trim();
                            rng = ntok[1].trim();
                        }
                        n = n.trim();
                        String modver = core.getServer().getModVersion(n);
                        if((modver != null) && ((rng == null) || HDBlockModels.checkVersionRange(modver, rng))) {
                            found = true;
                            Log.info(n + "[" + modver + "] textures enabled");
                            modname = n;
                            modversion = modver;
                            if(texturemod == null) texturemod = modname;
                            break;
                        }
                    }
                    if(!found) return;
                }
                else if(line.startsWith("texturemod:")) {
                    texturemod = line.substring(line.indexOf(':')+1).trim();
                }
                else if(line.startsWith("texturepath:")) {
                    texturepath = line.substring(line.indexOf(':')+1).trim();
                    if (texturepath.charAt(texturepath.length()-1) != '/') {
                        texturepath += "/";
                    }
                }
                else if(line.startsWith("biome:")) {
                    line = line.substring(6).trim();
                    String args[] = line.split(",");
                    int id = 0;
                    int grasscolormult = -1;
                    int foliagecolormult = -1;
                    int watercolormult = -1;
                    double rain = -1.0;
                    double tmp = -1.0;
                    for(int i = 0; i < args.length; i++) {
                        String[] v = args[i].split("=");
                        if(v.length < 2) {
                            Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname);
                            return;
                        }
                        if(v[0].equals("id")) {
                            id = getIntValue(varvals, v[1]);   
                        }
                        else if(v[0].equals("grassColorMult")) {
                            grasscolormult = Integer.valueOf(v[1], 16);
                        }
                        else if(v[0].equals("foliageColorMult")) {
                            foliagecolormult = Integer.valueOf(v[1], 16);
                        }
                        else if(v[0].equals("waterColorMult")) {
                            watercolormult = Integer.valueOf(v[1], 16);
                        }
                        else if(v[0].equals("temp")) {
                            tmp = Double.parseDouble(v[1]);
                        }
                        else if(v[0].equals("rain")) {
                            rain = Double.parseDouble(v[1]);
                        }
                    }
                    if(id > 0) {
                        BiomeMap b = BiomeMap.byBiomeID(id); /* Find biome */
                        if(b == null) {
                            Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname + ": " + id);
                        }
                        else {
                            if(foliagecolormult != -1)
                                b.setFoliageColorMultiplier(foliagecolormult);
                            if(grasscolormult != -1)
                                b.setGrassColorMultiplier(grasscolormult);
                            if(watercolormult != -1)
                                b.setWaterColorMultiplier(watercolormult);
                            if(tmp != -1.0)
                                b.setTemperature(tmp);
                            if(rain != -1.0)
                                b.setRainfall(rain);
                        }
                    }
                }
                else if(line.startsWith("version:")) {
                    line = line.substring(line.indexOf(':')+1);
                    int dash = line.indexOf('-');
                    if(dash < 0) {
                        if(!mcver.equals(line.trim())) { // If not match
                            return;
                        }
                    }
                    else {
                        String s1 = line.substring(0, dash).trim();
                        String s2 = line.substring(dash+1).trim();
                        if( (s1.equals("") || (s1.compareTo(mcver) <= 0)) &&
                                (s2.equals("") || (s2.compareTo(mcver) >= 0))) {
                        }
                        else {
                            return;
                        }
                    }
                }
                else if(line.startsWith("noterrainpng:")) {
                    line = line.substring(line.indexOf(':')+1);
                    if (line.startsWith("true")) {
                        terrain_ok = false;
                    }
                    else {
                        terrain_ok = true;
                    }
                }
            }
            if(mod_cfg_needed) {
                Log.severe("Error loading configuration file for " + modname);
            }

            Log.verboseinfo("Loaded " + cnt + " texture mappings from " + txtname);
        } catch (IOException iox) {
            Log.severe("Error reading " + txtname + " - " + iox.toString());
        } catch (NumberFormatException nfx) {
            Log.severe("Format error - line " + rdr.getLineNumber() + " of " + txtname + ": " + nfx.getMessage());
        } finally {
            if(rdr != null) {
                try {
                    rdr.close();
                    rdr = null;
                } catch (IOException e) {
                }
            }
        }

    }

    /* Process any block aliases */
    public static void handleBlockAlias() {
        for(int i = 0; i < BLOCKTABLELEN; i++) {
            int id = MapManager.mapman.getBlockIDAlias(i);
            if(id != i) {   /* New mapping? */
                HDTextureMap.remapTexture(i, id);
            }
        }
    }

    private static final int BLOCKID_GRASS = 2;
    private static final int BLOCKID_SNOW = 78;
    /**
     * Read color for given subblock coordinate, with given block id and data and face
     */
    public final void readColor(final HDPerspectiveState ps, final MapIterator mapiter, final Color rslt, final int blkid, final int lastblocktype,
            final TexturePackHDShader.ShaderState ss) {
        int blkdata = ps.getBlockData();
        HDTextureMap map = HDTextureMap.getMap(blkid, blkdata, ps.getBlockRenderData());
        BlockStep laststep = ps.getLastBlockStep();
        int patchid = ps.getTextureIndex();   /* See if patch index */
        int textid;
        int faceindex;
        if(patchid >= 0) {
            faceindex = patchid;
        }
        else {
            faceindex = laststep.ordinal();
        }
        textid = map.faces[faceindex];
        if (ctm != null) {
            int mod = 0;
            if(textid >= COLORMOD_MULT_INTERNAL) {
                mod = (textid / COLORMOD_MULT_INTERNAL) * COLORMOD_MULT_INTERNAL;
                textid -= mod;
            }
            textid = mod + ctm.mapTexture(mapiter, blkid, blkdata, laststep, textid, ss);
        }
        readColor(ps, mapiter, rslt, blkid, lastblocktype, ss, blkdata, map, laststep, patchid, textid, map.stdrotate);
        if(map.layers != null) {    /* If layered */
            /* While transparent and more layers */
            while(rslt.isTransparent() && (map.layers[faceindex] >= 0)) {
                faceindex = map.layers[faceindex];
                textid = map.faces[faceindex];
                readColor(ps, mapiter, rslt, blkid, lastblocktype, ss, blkdata, map, laststep, patchid, textid, map.stdrotate);
            }
        }
    }
    /**
     * Read color for given subblock coordinate, with given block id and data and face
     */
    private final void readColor(final HDPerspectiveState ps, final MapIterator mapiter, final Color rslt, final int blkid, final int lastblocktype,
                final TexturePackHDShader.ShaderState ss, int blkdata, HDTextureMap map, BlockStep laststep, int patchid, int textid, boolean stdrot) {
        if(textid < 0) {
            rslt.setTransparent();
            return;
        }
        int blkindex = indexByIDMeta(blkid, blkdata);
        boolean hasblockcoloring = ss.do_biome_shading && hasBlockColoring.get(blkindex);
        // Test if we have no texture modifications
        boolean simplemap = (textid < COLORMOD_MULT_INTERNAL) && (!hasblockcoloring);
        
        if (simplemap) {    /* If simple mapping */
            int[] texture = getTileARGB(textid);
            /* Get texture coordinates (U=horizontal(left=0),V=vertical(top=0)) */
            int u = 0, v = 0;
            /* If not patch, compute U and V */
            if(patchid < 0) {
                int[] xyz = ps.getSubblockCoord();

                switch(laststep) {
                    case X_MINUS: /* South face: U = East (Z-), V = Down (Y-) */
                        u = native_scale-xyz[2]-1; v = native_scale-xyz[1]-1; 
                        break;
                    case X_PLUS:    /* North face: U = West (Z+), V = Down (Y-) */
                        u = xyz[2]; v = native_scale-xyz[1]-1; 
                        break;
                    case Z_MINUS:   /* West face: U = South (X+), V = Down (Y-) */
                        u = xyz[0]; v = native_scale-xyz[1]-1;
                        break;
                    case Z_PLUS:    /* East face: U = North (X-), V = Down (Y-) */
                        u = native_scale-xyz[0]-1; v = native_scale-xyz[1]-1;
                        break;
                    case Y_MINUS:   /* U = East(Z-), V = South(X+) */
                        if(stdrot) {
                            u = xyz[0]; v = xyz[2];
                        }
                        else {
                            u = native_scale-xyz[2]-1; v = xyz[0];
                        }
                        break;
                    case Y_PLUS:
                        if(stdrot) {
                            u = native_scale-xyz[0]-1; v = xyz[2];
                        }
                        else {
                            u = xyz[2]; v = xyz[0];
                        }
                        break;
                }
            }
            else {
                u = fastFloor(ps.getPatchU() * native_scale);
                v = native_scale - fastFloor(ps.getPatchV() * native_scale) - 1;
            }
            /* Read color from texture */
            try {
                rslt.setARGB(texture[v*native_scale + u]);
            } catch(ArrayIndexOutOfBoundsException aoobx) {
                u = ((u < 0) ? 0 : ((u >= native_scale) ? (native_scale-1) : u));
                v = ((v < 0) ? 0 : ((v >= native_scale) ? (native_scale-1) : v));
                try {
                    rslt.setARGB(texture[v*native_scale + u]);
                } catch(ArrayIndexOutOfBoundsException oob2) { }
            }
            
            return;            
        }
        
        /* See if not basic block texture */
        int textop = textid / COLORMOD_MULT_INTERNAL;
        textid = textid % COLORMOD_MULT_INTERNAL;
        
        /* If clear-inside op, get out early */
        if((textop == COLORMOD_CLEARINSIDE) || (textop == COLORMOD_MULTTONED_CLEARINSIDE)) {
            /* Check if previous block is same block type as we are: surface is transparent if it is */
            if(blkid == lastblocktype) {
                rslt.setTransparent();
                return;
            }
            /* If water block, to watercolor tone op */
            if((blkid == 8) || (blkid == 9)) {
                textop = COLORMOD_WATERTONED;
            }
            else if(textop == COLORMOD_MULTTONED_CLEARINSIDE) {
                textop = COLORMOD_MULTTONED;
            }
        }

        int[] texture = getTileARGB(textid);
        /* Get texture coordinates (U=horizontal(left=0),V=vertical(top=0)) */
        int u = 0, v = 0, tmp;
        
        if(patchid < 0) {
            int[] xyz = ps.getSubblockCoord();

            switch(laststep) {
                case X_MINUS: /* South face: U = East (Z-), V = Down (Y-) */
                    u = native_scale-xyz[2]-1; v = native_scale-xyz[1]-1; 
                    break;
                case X_PLUS:    /* North face: U = West (Z+), V = Down (Y-) */
                    u = xyz[2]; v = native_scale-xyz[1]-1; 
                    break;
                case Z_MINUS:   /* West face: U = South (X+), V = Down (Y-) */
                    u = xyz[0]; v = native_scale-xyz[1]-1;
                    break;
                case Z_PLUS:    /* East face: U = North (X-), V = Down (Y-) */
                    u = native_scale-xyz[0]-1; v = native_scale-xyz[1]-1;
                    break;
                case Y_MINUS:   /* U = East(Z-), V = South(X+) */
                    if(stdrot) {
                        u = xyz[0]; v = xyz[2];
                    }
                    else {
                        u = native_scale-xyz[2]-1; v = xyz[0];
                    }
                    break;
                case Y_PLUS:
                    if(stdrot) {
                        u = native_scale-xyz[0]-1; v = xyz[2];
                    }
                    else {
                        u = xyz[2]; v = xyz[0];
                    }
                    break;
            }
        }
        else {
            u = fastFloor(ps.getPatchU() * native_scale);
            v = native_scale - fastFloor(ps.getPatchV() * native_scale) - 1;
        }
        /* Handle U-V transorms before fetching color */
        switch(textop) {
            case COLORMOD_ROT90:
                tmp = u; u = native_scale - v - 1; v = tmp;
                break;
            case COLORMOD_ROT180:
                u = native_scale - u - 1; v = native_scale - v - 1;
                break;
            case COLORMOD_ROT270:
            case COLORMOD_GRASSTONED270:
            case COLORMOD_FOLIAGETONED270:
            case COLORMOD_WATERTONED270:
                tmp = u; u = v; v = native_scale - tmp - 1;
                break;
            case COLORMOD_FLIPHORIZ:
                u = native_scale - u - 1;
                break;
            case COLORMOD_SHIFTDOWNHALF:
                if(v < native_scale/2) {
                    rslt.setTransparent();
                    return;
                }
                v -= native_scale/2;
                break;
            case COLORMOD_SHIFTDOWNHALFANDFLIPHORIZ:
                if(v < native_scale/2) {
                    rslt.setTransparent();
                    return;
                }
                v -= native_scale/2;
                u = native_scale - u - 1;
                break;
            case COLORMOD_INCLINEDTORCH:
                if(v >= (3*native_scale/4)) {
                    rslt.setTransparent();
                    return;
                }
                v += native_scale/4;
                if(u < native_scale/2) u = native_scale/2-1;
                if(u > native_scale/2) u = native_scale/2;
                break;
            case COLORMOD_GRASSSIDE:
                boolean do_grass_side = false;
                boolean do_snow_side = false;
                if(ss.do_better_grass) {
                    mapiter.unstepPosition(laststep);
                    if(mapiter.getBlockTypeID() == BLOCKID_SNOW)
                        do_snow_side = true;
                    if(mapiter.getBlockTypeIDAt(BlockStep.Y_MINUS) == BLOCKID_GRASS)
                        do_grass_side = true;
                    mapiter.stepPosition(laststep);
                }
                
                /* Check if snow above block */
                if(mapiter.getBlockTypeIDAt(BlockStep.Y_PLUS) == BLOCKID_SNOW) {
                    if(do_snow_side) {
                        texture = getTileARGB(TILEINDEX_SNOW); /* Snow full side block */
                        textid = TILEINDEX_SNOW;
                    }
                    else {
                        texture = getTileARGB(TILEINDEX_SNOWSIDE); /* Snow block */
                        textid = TILEINDEX_SNOWSIDE;
                    }
                    textop = 0;
                }
                else {  /* Else, check the grass color overlay */
                    if(do_grass_side) {
                        texture = getTileARGB(TILEINDEX_GRASS); /* Grass block */
                        textid = TILEINDEX_GRASS;
                        textop = COLORMOD_GRASSTONED;   /* Force grass toning */
                    }
                    else {
                        int ovclr = getTileARGB(TILEINDEX_GRASSMASK)[v*native_scale+u];
                        if((ovclr & 0xFF000000) != 0) { /* Hit? */
                            texture = getTileARGB(TILEINDEX_GRASSMASK); /* Use it */
                            textop = COLORMOD_GRASSTONED;   /* Force grass toning */
                        }
                    }
                }
                break;
            case COLORMOD_LILYTONED:
                /* Rotate texture based on lily orientation function (from renderBlockLilyPad in RenderBlocks.jara in MCP) */
                long l1 = (long)(mapiter.getX() * 0x2fc20f) ^ (long)mapiter.getZ() * 0x6ebfff5L ^ (long)mapiter.getY();
                l1 = l1 * l1 * 0x285b825L + l1 * 11L;
                int orientation = (int)(l1 >> 16 & 3L);
                switch(orientation) {
                    case 0:
                        tmp = u; u = native_scale - v - 1; v = tmp;
                        break;
                    case 1:
                        u = native_scale - u - 1; v = native_scale - v - 1;
                        break;
                    case 2:
                        tmp = u; u = v; v = native_scale - tmp - 1;
                        break;
                    case 3:
                        break;
                }
                break;
        }
        /* Read color from texture */
        try {
            rslt.setARGB(texture[v*native_scale + u]);
        } catch (ArrayIndexOutOfBoundsException aioobx) {
            rslt.setARGB(0);
        }

        int clrmult = -1;
        int clralpha = 0xFF000000;
        int custclrmult = -1;
        // If block has custom coloring
        if (hasblockcoloring) {
            Integer idx = (Integer) this.blockColoring.get(blkindex);
            LoadedImage img = imgs[idx];
            if (img.argb != null) {
                custclrmult = mapiter.getSmoothWaterColorMultiplier(img.argb);
            }
            else {
                hasblockcoloring = false;
            }
        }
        //if (!hasblockcoloring) {
            // Switch based on texture modifier
            switch(textop) {
                case COLORMOD_GRASSTONED:
                case COLORMOD_GRASSTONED270:
                    if(ss.do_biome_shading) {
                        if(imgs[IMG_SWAMPGRASSCOLOR] != null)
                            clrmult = mapiter.getSmoothColorMultiplier(imgs[IMG_GRASSCOLOR].argb, imgs[IMG_SWAMPGRASSCOLOR].argb);
                        else
                            clrmult = mapiter.getSmoothGrassColorMultiplier(imgs[IMG_GRASSCOLOR].argb);
                    }
                    else {
                        clrmult = imgs[IMG_GRASSCOLOR].trivial_color;
                    }
                    break;
                case COLORMOD_FOLIAGETONED:
                case COLORMOD_FOLIAGETONED270:
                    if(ss.do_biome_shading) {
                        if(imgs[IMG_SWAMPFOLIAGECOLOR] != null)
                            clrmult = mapiter.getSmoothColorMultiplier(imgs[IMG_FOLIAGECOLOR].argb, imgs[IMG_SWAMPFOLIAGECOLOR].argb);
                        else
                            clrmult = mapiter.getSmoothFoliageColorMultiplier(imgs[IMG_FOLIAGECOLOR].argb);
                    }
                    else {
                        clrmult = imgs[IMG_FOLIAGECOLOR].trivial_color;
                    }
                    break;
                case COLORMOD_FOLIAGEMULTTONED:
                    if(ss.do_biome_shading) {
                        if(imgs[IMG_SWAMPFOLIAGECOLOR] != null)
                            clrmult = mapiter.getSmoothColorMultiplier(imgs[IMG_FOLIAGECOLOR].argb, imgs[IMG_SWAMPFOLIAGECOLOR].argb);
                        else
                            clrmult = mapiter.getSmoothFoliageColorMultiplier(imgs[IMG_FOLIAGECOLOR].argb);
                    }
                    else {
                        clrmult = imgs[IMG_FOLIAGECOLOR].trivial_color;
                    }
                    if(map.custColorMult != null) {
                        clrmult = ((clrmult & 0xFEFEFE) + map.custColorMult.getColorMultiplier(mapiter)) / 2;
                    }
                    else {
                        clrmult = ((clrmult & 0xFEFEFE) + map.colorMult) / 2;
                    }
                    break;

                case COLORMOD_WATERTONED:
                case COLORMOD_WATERTONED270:
                    if(imgs[IMG_WATERCOLORX] != null) {
                        if(ss.do_biome_shading) {
                            clrmult = mapiter.getSmoothWaterColorMultiplier(imgs[IMG_WATERCOLORX].argb);
                        }
                        else {
                            clrmult = imgs[IMG_WATERCOLORX].trivial_color;
                        }
                    }
                    else {
                        if(ss.do_biome_shading) {
                            if (colorMultWater != 0xFFFFFF)
                                clrmult = colorMultWater;
                            else
                                clrmult = mapiter.getSmoothWaterColorMultiplier();
                        }
                    }
                    break;
                case COLORMOD_BIRCHTONED:
                    if(ss.do_biome_shading) {
                        if(imgs[IMG_BIRCHCOLOR] != null)
                            clrmult = mapiter.getSmoothFoliageColorMultiplier(imgs[IMG_BIRCHCOLOR].argb);
                        else
                            clrmult = colorMultBirch;
                    }
                    else {
                        clrmult = colorMultBirch;
                    }
                    break;
                case COLORMOD_PINETONED:
                    if(ss.do_biome_shading) {
                        if(imgs[IMG_PINECOLOR] != null)
                            clrmult = mapiter.getSmoothFoliageColorMultiplier(imgs[IMG_PINECOLOR].argb);
                        else
                            clrmult = colorMultPine;
                    }
                    else {
                        clrmult = colorMultPine;
                    }
                    break;
                case COLORMOD_LILYTONED:
                    clrmult = colorMultLily;
                    break;
                case COLORMOD_MULTTONED:    /* Use color multiplier */
                    if(map.custColorMult != null) {
                        clrmult = map.custColorMult.getColorMultiplier(mapiter);
                    }
                    else {
                        clrmult = map.colorMult;
                    }
                    if((clrmult & 0xFF000000) != 0) {
                        clralpha = clrmult & 0xFF000000;
                    }
                    break;
            }
        //}
        
        if((clrmult != -1) && (clrmult != 0)) {
            rslt.blendColor(clrmult | clralpha);
        }
        if (hasblockcoloring && (custclrmult != -1)) {
            rslt.blendColor(custclrmult | clralpha);
        }
    }
    
    private static final void makeAlphaPure(int[] argb) {
        for(int i = 0; i < argb.length; i++) {
            if((argb[i] & 0xFF000000) != 0)
                argb[i] |= 0xFF000000;
        }
    }

    private static final int fastFloor(double f) {
        return ((int)(f + 1000000000.0)) - 1000000000;
    }

    /**
     * Get tile index, based on tile file name and relative index within tile file
     * @param fname - filename
     * @param idx - tile index (= (y * xdim) + x)
     * @return global tile index, or -1 if not found
     */
    public static int findDynamicTile(String fname, int idx) {
        DynamicTileFile f;
        /* Find existing, if already there */
        f = addonfilesbyname.get(fname);
        if (f != null) {
            if ((idx >= 0) && (idx < f.tile_to_dyntile.length) && (f.tile_to_dyntile[idx] >= 0)) {
                return f.tile_to_dyntile[idx];
            }
        }
        return -1;
    }
    /**
     * Add new dynmaic file definition, or return existing
     * 
     * @param fname
     * @param xdim
     * @param ydim
     * @param fmt 
     * @param args
     * @return dynamic file index
     */
    public static int findOrAddDynamicTileFile(String fname, String modname, int xdim, int ydim, TileFileFormat fmt, String[] args) {
        DynamicTileFile f;
        /* Find existing, if already there */
        f = addonfilesbyname.get(fname);
        if (f != null) {
            return f.idx;
        }
        /* Add new tile file entry */
        f = new DynamicTileFile();
        f.filename = fname;
        f.modname = modname;
        f.tilecnt_x = xdim;
        f.tilecnt_y = ydim;
        f.format = fmt;
        switch(fmt) {
            case GRID:
                f.tile_to_dyntile = new int[xdim*ydim];
                break;
            case CHEST:
                f.tile_to_dyntile = new int[TILEINDEX_CHEST_COUNT]; /* 6 images for chest tile */
                break;
            case BIGCHEST:
                f.tile_to_dyntile = new int[TILEINDEX_BIGCHEST_COUNT]; /* 10 images for chest tile */
                break;
            case SIGN:
                f.tile_to_dyntile = new int[TILEINDEX_SIGN_COUNT]; /* 10 images for sign tile */
                break;
            case CUSTOM:
                {
                    List<CustomTileRec> recs = new ArrayList<CustomTileRec>();
                    for(String a : args) {
                        String[] v = a.split("=");
                        if(v.length != 2) continue;
                        if(v[0].startsWith("tile")) {
                            int id = 0;
                            try {
                                id = Integer.parseInt(v[0].substring(4));
                            } catch (NumberFormatException nfx) {
                                Log.warning("Bad tile ID: " + v[0]);
                                continue;
                            }
                            while(recs.size() <= id) {
                                recs.add(null);
                            }
                            CustomTileRec rec = new CustomTileRec();
                            try {
                                String[] coords = v[1].split("/");
                                String[] topleft = coords[0].split(":");
                                rec.srcx = Integer.parseInt(topleft[0]);
                                rec.srcy = Integer.parseInt(topleft[1]);
                                String[] size = coords[1].split(":");
                                rec.width = Integer.parseInt(size[0]);
                                rec.height = Integer.parseInt(size[1]);
                                if(coords.length >= 3) {
                                    String[] dest = coords[2].split(":");
                                    rec.targetx = Integer.parseInt(dest[0]);
                                    rec.targety = Integer.parseInt(dest[1]);
                                }
                                recs.set(id,  rec);
                            } catch (Exception x) {
                                Log.warning("Bad custom tile coordinate: " + v[1]);
                            }
                        }
                    }
                    f.tile_to_dyntile = new int[recs.size()];
                    f.cust = recs;
                }
                break;
            case SKIN:
                f.tile_to_dyntile = new int[TILEINDEX_SKIN_COUNT]; /* 6 images for skin tile */
                break;
            case TILESET:
                f.tile_to_dyntile = new int[xdim*ydim];
                break;
            default:
                f.tile_to_dyntile = new int[xdim*ydim];
                break;
        }
        Arrays.fill(f.tile_to_dyntile,  -1);
        f.idx = addonfiles.size();
        addonfiles.add(f);
        addonfilesbyname.put(f.filename, f);
        //Log.info("File " + fname + "(" + f.idx + ")=" + fmt.toString());
        return f.idx;
    }
    /**
     * Add or find dynamic tile index of given dynamic tile
     * @param dynfile_idx - index of file
     * @param tile_id - ID of tile within file
     * @return global tile ID
     */
    public static int findOrAddDynamicTile(int dynfile_idx, int tile_id) {
        DynamicTileFile f = addonfiles.get(dynfile_idx);
        if(f == null) {
            Log.warning("Invalid add-on file index: " + dynfile_idx);
            return 0;
        }
        if(f.tile_to_dyntile[tile_id] < 0) {   /* Not assigned yet? */
            f.tile_to_dyntile[tile_id] = next_dynamic_tile;
            next_dynamic_tile++;    /* Allocate next ID */
        }
        return f.tile_to_dyntile[tile_id];
    }

    private static final int[] smooth_water_mult = new int[10];
    
    public static int getTextureIDAt(MapIterator mapiter, int blkdata, int blkmeta, BlockStep face) {
        HDTextureMap map = HDTextureMap.getMap(blkdata, blkmeta, blkmeta);
        int idx = -1;
        if (map != null) {
            int sideidx = face.ordinal();
            if (map.faces != null) {
                if (sideidx < map.faces.length)
                    idx = map.faces[sideidx];
                else 
                    idx = map.faces[0];
            }
        }
        if(idx > 0)
            idx = idx % COLORMOD_MULT_INTERNAL;
        return idx;
    }
    
    private static final String PALETTE_BLOCK_KEY = "palette.block.";

    private void processCustomColorMap(String fname, String ids) {
        // Register file name
        int idx = findOrAddDynamicTileFile(fname, null, 1, 1, TileFileFormat.BIOME, new String[0]);
        if(idx < 0) {
            Log.info("Error registering custom color file: " + fname);
            return;
        }
        Integer index = idx + IMG_CNT;
        // Now, parse block ID list
        for (String id : ids.split("\\s+")) {
            String[] tok = id.split(":");
            int meta = -1;
            int blkid = -1;
            if (tok.length == 1) {  /* Only ID */
                try {
                    blkid = Integer.parseInt(tok[0]);
                } catch (NumberFormatException nfx) {
                    Log.info("Bad custom color block ID: " + tok[0]);
                }
            }
            else if (tok.length == 2) { /* ID : meta */
                try {
                    blkid = Integer.parseInt(tok[0]);
                } catch (NumberFormatException nfx) {
                    Log.info("Bad custom color block ID: " + tok[0]);
                }
                try {
                    meta = Integer.parseInt(tok[1]);
                } catch (NumberFormatException nfx) {
                    Log.info("Bad custom color meta ID: " + tok[1]);
                }
            }

            /* Add mappings for values */
            if ((blkid > 0) && (blkid < 4096)) {
                if ((meta >= 0) && (meta < 16)) {
                    int idm = indexByIDMeta(blkid, meta);
                    this.hasBlockColoring.set(idm);
                    this.blockColoring.put(idm, index);
                }
                else if (meta == -1) {  /* All meta IDs */
                    for (meta = 0; meta < 16; meta++) {
                        int idm = indexByIDMeta(blkid, meta);
                        this.hasBlockColoring.set(idm);
                        this.blockColoring.put(idm, index);
                    }
                }
            }
        }
    }
    private void processCustomColors(Properties p) {
        // Loop through keys
        for(String pname : p.stringPropertyNames()) {
            if(!pname.startsWith(PALETTE_BLOCK_KEY))
                continue;
            String v = p.getProperty(pname);
            String fname = pname.substring(PALETTE_BLOCK_KEY.length()).trim(); // Get filename of color map
            if(fname.charAt(0) == '/') fname = fname.substring(1); // Strip leading /
            if(fname.charAt(0) == '~') fname = "assets/minecraft/mcpatcher" + fname.substring(1);
            processCustomColorMap(fname, v);
        }
    }
    private static final int indexByIDMeta(int blkid, int meta) {
        return ((blkid << 4) | meta);
    }
    
    static {
        /*
         * Generate smoothed swamp multipliers (indexed by swamp biome count)
         */
        Color c = new Color();
        for(int i = 0; i < 10; i++) {
            /* Use water color multiplier base for 1.1 (E0FFAE) */
            int r = (((9-i) * 0xFF) + (i * 0xE0)) / 9;
            int g = 0xFF;
            int b = (((9-i) * 0xFF) + (i * 0xAE)) / 9;
            c.setRGBA(r & 0xFE, g & 0xFE, b & 0xFE, 0xFF);
            smooth_water_mult[i] = c.getARGB();
        }
    }
    
    public int getTrivialFoliageMultiplier() {
        return imgs[IMG_FOLIAGECOLOR].argb[BiomeMap.FOREST.biomeLookup()];
    }
    public int getTrivialGrassMultiplier() {
        return imgs[IMG_GRASSCOLOR].argb[BiomeMap.FOREST.biomeLookup()];
    }
    public int getTrivialWaterMultiplier() {
        if(imgs[IMG_WATERCOLORX] != null) {
            return imgs[IMG_WATERCOLORX].argb[BiomeMap.FOREST.biomeLookup()];
        }
        else {
            return 0xFFFFFF;
        }
    }
    public int getCustomBlockMultiplier(int blkid, int blkdata) {
        int blkindex = indexByIDMeta(blkid, blkdata);
        if (hasBlockColoring.get(blkindex)) {
            Integer idx = (Integer) this.blockColoring.get(blkindex);
            LoadedImage img = imgs[idx];
            if (img.argb != null) {
                return img.argb[BiomeMap.FOREST.biomeLookup()];
            }
        }
        return 0xFFFFFF;
    }
}
