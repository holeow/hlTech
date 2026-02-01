/*
 * PipeComponent.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Renamed some variables fields, and functions to fit the new system
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */
package com.hlw.hlTech;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class CableComponent implements Component<ChunkStore> {
    public int cableState = 0;
    public boolean isCable = false;
    public boolean isController = false;
    @Nonnull
    public static final BuilderCodec<CableComponent> CODEC;

    public CableComponent() {
    }

    public CableComponent(int state, boolean isCable, boolean isController) {
        this.cableState = state;
        this.isCable = isCable;
        this.isController = isController;
    }

    public void setIsCable(Boolean state){
        this.isCable = state;
    }
    public boolean getIsCable(){
        return isCable;
    }
    public void setIsController(Boolean state){
        this.isController = state;
    }
    public boolean getIsController(){
        return isController;
    }


    public CableComponent clone() {
        return new CableComponent(this.cableState, this.isCable,this.isController);
    }

    static {
        CODEC = BuilderCodec.builder(CableComponent.class, CableComponent::new)
                .append(new KeyedCodec<Integer>("CableState", Codec.INTEGER), (o, state) -> o.cableState = state, (o) -> o.cableState).add()
                .append(new KeyedCodec<Boolean>("IsCable", Codec.BOOLEAN), CableComponent::setIsCable,CableComponent::getIsCable).add()
                .append(new KeyedCodec<Boolean>("IsController", Codec.BOOLEAN), CableComponent::setIsController,CableComponent::getIsController).add()

                .build();
    }
}
