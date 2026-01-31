/*
 * PipeSystem.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Renamed some variables fields, and functions to fit the new system
 * - Added the controller system in the code.
 * - Changed the state finding method of the cables
 * - Added delay on inventory breaking to ensure the inventory is removed on cheching the new state of the cables.
 * - Added the use of the locks and of the worldHolders instead of the networkmanager.
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */



package com.hlw.hlTech;

import com.hlw.hlTech.network.CableNetworkManager;
import com.hlw.hlTech.network.WorldHolder;
import com.hlw.hlTech.util.BlockPos;
import com.hlw.hlTech.util.ChunkBlockHelper;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class CableSystem {

    private static void updateNeighbors(World world, Vector3i pos, CableNetworkManager manager, Vector3i ignorePos) {
        applyStateForced(world, pos, manager,null);
        applyState(world, new Vector3i(pos.x + 1, pos.y, pos.z), manager,null);
        applyState(world, new Vector3i(pos.x - 1, pos.y, pos.z), manager,null);
        applyState(world, new Vector3i(pos.x, pos.y, pos.z + 1), manager,null);
        applyState(world, new Vector3i(pos.x, pos.y, pos.z - 1), manager,null);
        applyState(world, new Vector3i(pos.x, pos.y + 1, pos.z), manager,null);
        applyState(world, new Vector3i(pos.x, pos.y - 1, pos.z), manager,null);
    }
    private static void applyStateForced(World world, Vector3i pos, CableNetworkManager manager, Vector3i ignorePos) {

        applyState(world,pos,manager,ignorePos);
    }

    public static void applyState(World world, Vector3i pos, CableNetworkManager manager, Vector3i ignorePos) {
        long chunkId = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkId);
        if (chunk != null) {
            BlockType type = world.getBlockType(pos.x, pos.y, pos.z);
            if (type != null && type.getId().toLowerCase().contains("cable")) {
                boolean n = isConnectable(manager, world, pos.x, pos.y, pos.z - 1,ignorePos);
                boolean s = isConnectable(manager, world, pos.x, pos.y, pos.z + 1,ignorePos);
                boolean e = isConnectable(manager, world, pos.x + 1, pos.y, pos.z,ignorePos);
                boolean w = isConnectable(manager, world, pos.x - 1, pos.y, pos.z,ignorePos);
                boolean u = isConnectable(manager, world, pos.x, pos.y + 1, pos.z,ignorePos);
                boolean d = isConnectable(manager, world, pos.x, pos.y - 1, pos.z,ignorePos);
                String stateName = findState(u,d,n,s,e,w);

                try {
                    chunk.setBlockInteractionState(pos.x & 31, pos.y, pos.z & 31, type, stateName, true);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }

            }
        }
    }

    private static boolean isConnectable(CableNetworkManager manager, World world, int x, int y, int z, Vector3i ignorePos) {
        if (ignorePos != null && ignorePos.x == x && ignorePos.y == y && ignorePos.z == z) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        WorldHolder holder = manager.worldHolders.get(world.getWorldConfig().getUuid());
        if(holder == null) return false;
        return holder.getNodeAt(pos) != null || ChunkBlockHelper.hasInventoryAt(world, pos);
    }




    private static String findState(boolean u, boolean d, boolean n, boolean s, boolean e, boolean w) {
        if(!u&&!d&&!n&&!s&&!e&&!w){
            return "Empty";
        }
        else {
            return (u? "U" : "") + (d? "D" : "") + (n? "N" : "") +(s? "S" : "") + (e? "E" : "") + (w? "W" : "");
        }
    }


    public static class PlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
        private final CableNetworkManager networkManager;

        public PlaceSystem(CableNetworkManager networkManager) {
            super(PlaceBlockEvent.class);
            this.networkManager = networkManager;
        }

        public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer, PlaceBlockEvent event) {
            Vector3i pos = event.getTargetBlock();
            World world = ((EntityStore)store.getExternalData()).getWorld();
            String itemId = event.getItemInHand() != null ? event.getItemInHand().getItemId() : "";
            boolean isCable = itemId.equalsIgnoreCase("cable");
            boolean isController = itemId.equalsIgnoreCase("controller");
            this.networkManager.ensureWorldRegistered(world);
            WorldHolder holder = this.networkManager.worldHolders.get(world.getWorldConfig().getUuid());
            try {
                holder.acquireLock();
                if (isCable) {
                    holder.onCablePlaced(new BlockPos(pos.x, pos.y, pos.z));
                } if(isController){
                    holder.onControllerPlaced(new BlockPos(pos.x, pos.y, pos.z));
                }
                else {
                   holder.onPotentialInventoryPlaced( new BlockPos(pos.x, pos.y, pos.z));
                }

                CableSystem.updateNeighbors(world, pos, this.networkManager,null);
            }
            finally {
                holder.releaseLock();
            }


            world.execute(()->
                    {
                        holder.acquireLock();
                        Vector3i finalPos = new Vector3i(pos.x, pos.y, pos.z);
                        CableNetworkManager finalManager = this.networkManager;

                        try {
                            CableSystem.applyState(world, finalPos, finalManager,null);
                            CableSystem.applyState(world, new Vector3i(finalPos.x + 1, finalPos.y, finalPos.z), finalManager,null);
                            CableSystem.applyState(world, new Vector3i(finalPos.x - 1, finalPos.y, finalPos.z), finalManager,null);
                            CableSystem.applyState(world, new Vector3i(finalPos.x, finalPos.y, finalPos.z + 1), finalManager,null);
                            CableSystem.applyState(world, new Vector3i(finalPos.x, finalPos.y, finalPos.z - 1), finalManager,null);
                            CableSystem.applyState(world, new Vector3i(finalPos.x, finalPos.y + 1, finalPos.z), finalManager,null);
                            CableSystem.applyState(world, new Vector3i(finalPos.x, finalPos.y - 1, finalPos.z), finalManager,null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        finally {
                            holder.releaseLock();
                        }

                    });

        }


        @Nonnull
        public Query<EntityStore> getQuery() {
            return Query.and(new Query[]{Player.getComponentType()});
        }
    }

    public static class BreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        private final CableNetworkManager networkManager;

        public BreakSystem(CableNetworkManager networkManager) {
            super(BreakBlockEvent.class);
            this.networkManager = networkManager;
        }

        public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer, BreakBlockEvent event) {
            Vector3i pos = event.getTargetBlock();
            World world = store.getExternalData().getWorld();
            boolean isCable = event.getBlockType() != null && event.getBlockType().getId().toLowerCase().contains("cable_state_definitions");
            boolean isController = event.getBlockType() != null && event.getBlockType().getId().toLowerCase().contains("controller");
            this.networkManager.ensureWorldRegistered(world);
            var wh = this.networkManager.worldHolders.get(world.getWorldConfig().getUuid());
            boolean mightBeInventory = false;
            try{
                wh.acquireLock();
                if (isCable) {
                    wh.onCableRemoved( new BlockPos(pos.x, pos.y, pos.z));
                    CableSystem.updateNeighbors(world, pos, this.networkManager,pos);
                } else if (isController){
                    wh.onControllerRemoved( new BlockPos(pos.x, pos.y, pos.z));
                    CableSystem.updateNeighbors(world, pos, this.networkManager,pos);
                }
                else mightBeInventory = true;
            }
            finally {
                wh.releaseLock();
            }


            if(mightBeInventory)
             {

                world.execute(() -> {
                    try {
                        wh.acquireLock();
                        wh.onInventoryRemoved( new BlockPos(pos.x, pos.y, pos.z));
                        CableSystem.updateNeighbors(world, pos, this.networkManager,pos);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        wh.releaseLock();
                    }

                });

            }


        }

        @Nonnull
        public Query<EntityStore> getQuery() {
            ;return Query.and(new Query[]{Player.getComponentType()});
        }
    }



}