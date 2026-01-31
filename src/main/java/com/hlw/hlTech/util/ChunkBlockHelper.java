/*
 * ChunkBlockHelper.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Renamed some variables fields, and functions to fit the new system
 * - Added the controller system in the code.
 * - Added the ability to get an ItemContainer using a blockPos
 * - Added GetblockTypeat function.
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */

package com.hlw.hlTech.util;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

import javax.annotation.Nullable;

public class ChunkBlockHelper {

    private static final int CHUNK_SIZE = 32;
    private static final int CHUNK_MASK = 31;

    private ChunkBlockHelper() {
    }

    public static int toLocalX(int worldX) {
        return worldX & 31;
    }

    public static int toLocalZ(int worldZ) {
        return worldZ & 31;
    }

    public static int getChunkX(int worldX) {
        return worldX >> 5;
    }

    public static int getChunkZ(int worldZ) {
        return worldZ >> 5;
    }

    public static boolean isValidY(int y) {
        return y >= 0 && y <= 255;
    }

    @Nullable
    public static BlockState getBlockStateFollowingFiller(WorldChunk chunk, int x, int y, int z) {
        int localX = toLocalX(x);
        int localZ = toLocalZ(z);
        int filler = chunk.getFiller(localX, y, localZ);
        BlockType dt= (chunk.getBlockType(localX, y, localZ));

       // String id = "dt is null";
       // if(dt!= null){
        //    id = dt.toString();
       // }
        //DebugLog.log("getBlockStateFollowingFiller filler:"+ filler +" / getblock:" + chunk.getBlock(localX,y,localZ)+" / getblocktype:id:" + dt.getId());
        if (filler == 0) {
            return chunk.getState(localX, y, localZ);
        } else {
            int fillerX = FillerBlockUtil.unpackX(filler);
            int fillerY = FillerBlockUtil.unpackY(filler);
            int fillerZ = FillerBlockUtil.unpackZ(filler);
            int masterX = x - fillerX;
            int masterY = y - fillerY;
            int masterZ = z - fillerZ;
            int currentChunkX = chunk.getX();
            int currentChunkZ = chunk.getZ();
            int masterChunkX = getChunkX(masterX);
            int masterChunkZ = getChunkZ(masterZ);
            WorldChunk masterChunk;
            if (masterChunkX == currentChunkX && masterChunkZ == currentChunkZ) {
                masterChunk = chunk;
            } else {
                masterChunk = chunk.getWorld().getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(masterX, masterZ));
                if (masterChunk == null) {
                    return null;
                }
            }

            return masterChunk.getState(toLocalX(masterX), masterY, toLocalZ(masterZ));
        }
    }

    @Nullable
    public static String getBlockTypeIdAt(WorldChunk chunk, int x, int y, int z){
        int localX = toLocalX(x);
        int localZ = toLocalZ(z);
        BlockType dt= (chunk.getBlockType(localX, y, localZ));
        if(dt == null) return null;
        else return dt.getId();
    }
    @Nullable
    public static BlockType getBlockTypeAt(WorldChunk chunk, int x, int y, int z){
        int localX = toLocalX(x);
        int localZ = toLocalZ(z);
        return (chunk.getBlockType(localX, y, localZ));

    }

    public static boolean hasControllerAt(World world, BlockPos pos){
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ()));
        if (chunk == null) {
            return false;
        } else {
            String id = getBlockTypeIdAt(chunk, pos.getX(), pos.getY(), pos.getZ());
            return id.equalsIgnoreCase("controller");
        }
    }
    public static long getChunkIdAt(BlockPos pos){
        return ChunkUtil.indexChunkFromBlock(pos.getX(),pos.getZ());
    }
    public static boolean isChunkLoaded(World world, long chunkId){
        return world.getChunkIfLoaded(chunkId)!=null;
    }

    public static ItemContainer getItemContainer ( World world, BlockPos pos){
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ()));
        if (chunk == null) {
            return null;
        }
        else{
            BlockState state = getBlockStateFollowingFiller(chunk, pos.getX(), pos.getY(), pos.getZ());
            return state instanceof ItemContainerBlockState ? ((ItemContainerBlockState)state).getItemContainer() : null;
        }
    }
    @Nullable
    public static ItemContainer getItemContainer(WorldChunk chunk, int x, int y, int z) {
        BlockState state = getBlockStateFollowingFiller(chunk, x, y, z);
        return state instanceof ItemContainerBlockState ? ((ItemContainerBlockState)state).getItemContainer() : null;
    }

    public static boolean hasInventoryAt(World world, BlockPos pos) {
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ()));
        if (chunk == null) {
            return false;
        } else {
            BlockState blockState = getBlockStateFollowingFiller(chunk, pos.getX(), pos.getY(), pos.getZ());
            return blockState instanceof ItemContainerBlockState;
        }
    }

    public static int getFiller(WorldChunk chunk, int x, int y, int z) {
        return chunk.getFiller(toLocalX(x), y, toLocalZ(z));
    }
}
