/*
 * PipeUseBlockEventSystem.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Renamed some variables fields, and functions to fit the new system
 * - Changed the opening of GUI from the cable (pipe) to the controller.
 * - Used the NodeHolder to hold both cables (pipes) and controllers.
 * - Added the use of the locks and the usage of the worldHolder instead of networkmanager.
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */

package com.hlw.hlTech;

import com.hlw.hlTech.network.CableNetworkManager;
import com.hlw.hlTech.network.NodeHolder;
import com.hlw.hlTech.util.BlockPos;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ControllerUseBlockEventSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
    public ControllerUseBlockEventSystem() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public void handle(int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer,  UseBlockEvent.Pre pre) {
        String id = pre.getBlockType().getId();

    if(id.equalsIgnoreCase("controller")){
        try{
            if(pre.getContext().getChain() == null || pre.getContext().getChain().getCallDepth() != 0)
                return;
        }
        catch(Exception e){
            DebugLog.log("GetContext.getchain error in ui" +  e.getMessage());
            return;
        }
        if(pre.getInteractionType() == InteractionType.Use){
            World w = commandBuffer.getExternalData().getWorld();
            Vector3i target = pre.getTargetBlock();
            int x = target.x;
            int y = target.y;
            int z = target.z;
            CableNetworkManager manager = CablePlugin.getInstance().getPipeNetworkManager();
            NodeHolder node = null;
            if(manager!=null)
            {
                BlockPos pos = new BlockPos(x,y,z);
                manager.ensureWorldRegistered(w);
                var wh = manager.worldHolders.get(w.getWorldConfig().getUuid());
                try{
                    wh.acquireLock();
                    node = wh.getNodeAt(pos);
                    if(node == null || node.controllerNode == null){
                        wh.onControllerPlaced(pos);
                        node = wh.getNodeAt(pos);
                    }


                }
                finally {
                    wh.releaseLock();
                }
                if(node != null && node.controllerNode != null){


                    Ref<EntityStore> entityStoreRef = archetypeChunk.getReferenceTo(i);
                    Player player = store.getComponent(entityStoreRef,Player.getComponentType());
                    if(player == null) return;
                    PlayerRef playerRef = store.getComponent(entityStoreRef,PlayerRef.getComponentType());
                    if(playerRef == null)return;

                    try{
                        ControllerConfigGui configGUI = new ControllerConfigGui(playerRef,w,x,y,z);
                        player.getPageManager().openCustomPage(entityStoreRef,store,configGUI);
                    }
                    catch(Exception e){
                        DebugLog.log("[Cable Controller GUI] Error opening GUI: " + e.getMessage());
                    }
                    pre.setCancelled(true);
                }
            }

        }
    }





    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
