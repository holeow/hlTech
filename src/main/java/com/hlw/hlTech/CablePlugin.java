/*
 * PipePlugin.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Renamed some variables fields, and functions to fit the new system
 * - Added the controller system in the code.
 * - Added the StartWorld event to load the system on world start.
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */

package com.hlw.hlTech;

import com.hlw.hlTech.network.CableNetworkManager;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.ScheduledExecutorService;

public class CablePlugin extends JavaPlugin {
    private final CableNetworkManager cableNetworkManager;
    private ScheduledExecutorService tickExecutor;
    public static ComponentType<ChunkStore, CableComponent> PIPE_COMPONENT_TYPE;
    private static CablePlugin instance;

    public CablePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        this.cableNetworkManager = new CableNetworkManager();
    }

    public static CablePlugin getInstance() {
        return instance;
    }

    public CableNetworkManager getPipeNetworkManager() {
        return this.cableNetworkManager;
    }

    protected void setup() {
        super.setup();

        try {
            PIPE_COMPONENT_TYPE = this.getChunkStoreRegistry().registerComponent(CableComponent.class, "cable_data", CableComponent.CODEC);
        } catch (Exception e) {
            DebugLog.log("!! Failed to register CableComponent: " + e.getMessage());
        }



        CableConfig config = CableConfig.load();
        //this.cableNetworkManager.setConfig(config);
        System.out.println("[CablePlugin] Config loaded: Interval=" + config.transferIntervalMs + "ms, Items=" + config.itemsPerTransfer);

        try {
            this.getEntityStoreRegistry().registerSystem(new CableSystem.PlaceSystem(this.cableNetworkManager));
            this.getEntityStoreRegistry().registerSystem(new CableSystem.BreakSystem(this.cableNetworkManager));
            this.getEntityStoreRegistry().registerSystem(new ControllerUseBlockEventSystem());
            this.getEntityStoreRegistry().registerSystem(new GlobalUpdateSystem(this.cableNetworkManager));
            this.getEventRegistry().registerGlobal(StartWorldEvent.class, event -> {
                this.cableNetworkManager.ensureWorldRegistered(event.getWorld());
                var holder = this.cableNetworkManager.worldHolders.get(event.getWorld().getWorldConfig().getUuid());
                try{
                    holder.acquireLock();
                    holder.loadNetworks();
                    holder.refreshInventoryLinks();
                }
                finally {
                    holder.releaseLock();
                }
                });
        } catch (Exception e) {
            DebugLog.log("!! Failed to register Cable Systems: " + e.getMessage());
        }
        /*
        try {
            this.getCommandRegistry().registerCommand(new PipeCommand(this.pipeNetworkManager));
        } catch (Exception e) {
            Broadcaster.log("!! Failed to register PipeCommand: " + e.getMessage());
        }
        */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            System.out.println("[CablePlugin] Saving networks on shutdown...");
            for(var w : this.cableNetworkManager.worldHolders.entrySet()){
               try{
                   w.getValue().acquireLock();
                   w.getValue().saveNetworks();
               }
               finally {
                   w.getValue().releaseLock();
               }
            }
        }));
        System.out.println("CablePlugin setup complete with CableNetworkManager!");
    }
}
