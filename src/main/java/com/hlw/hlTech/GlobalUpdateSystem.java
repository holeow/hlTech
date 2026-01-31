package com.hlw.hlTech;

import com.hlw.hlTech.network.CableNetworkManager;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GlobalUpdateSystem extends TickingSystem<EntityStore> {

    private final CableNetworkManager manager;
    public GlobalUpdateSystem(CableNetworkManager manager){
        this.manager = manager;
    }

    @Override
    public void tick(float dt, int index, Store<EntityStore> store) {
        World world = store.getExternalData().getWorld();
        for(var wh : manager.worldHolders.entrySet()){
            wh.getValue().tick(dt);
        }
    }
}