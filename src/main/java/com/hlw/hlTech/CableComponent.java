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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class CableComponent implements Component<EntityStore> {
    public int cableState = 0;
    @Nonnull
    public static final BuilderCodec<CableComponent> CODEC;

    public CableComponent() {
    }

    public CableComponent(int state) {
        this.cableState = state;
    }

    public CableComponent clone() {
        return new CableComponent(this.cableState);
    }

    static {
        CODEC = ((BuilderCodec.Builder)BuilderCodec.builder(CableComponent.class, CableComponent::new).append(new KeyedCodec("CableState", Codec.INTEGER), (o, state) -> o.cableState = state, (o) -> o.cableState).add()).build();
    }
}
