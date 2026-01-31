/*
 * PipeNode.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Changed Pipes to Cables
 * - Removed some properties, getters and setters that became unused with the controller system added.
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */


package com.hlw.hlTech.network;

import com.hlw.hlTech.util.BlockPos;

import com.hlw.hlTech.util.Direction;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;


public class CableNode {

    private final UUID worldId;
    private final BlockPos position;
    private final Set<Direction> connections;
    private CableNetwork network;

    public CableNode(UUID worldId, BlockPos position ) {
        this.worldId = worldId;
        this.position = position;
        this.connections = EnumSet.noneOf(Direction.class);
    }
    public UUID getWorldId() {
        return this.worldId;
    }

    public BlockPos getPosition() {
        return this.position;
    }


    public Set<Direction> getConnections() {
        return this.connections;
    }

    public void addConnection(Direction direction) {
        this.connections.add(direction);
    }

    public void removeConnection(Direction direction) {
        this.connections.remove(direction);
    }

    public boolean hasConnection(Direction direction) {
        return this.connections.contains(direction);
    }

    public CableNetwork getNetwork() {
        return this.network;
    }

    public void setNetwork(CableNetwork network) {
        this.network = network;
    }


    public void tick() {

    }

    public void onTransfer() {

    }

    public BlockPos[] getConnectedNeighbors() {
        Stream<Direction> var10000 = this.connections.stream();
        BlockPos var10001 = this.position;
        Objects.requireNonNull(var10001);
        return (BlockPos[])var10000.map(var10001::offset).toArray((a) -> new BlockPos[a]);
    }

    public String toString() {
        String var10000 = String.valueOf(this.position);
        return "PipeNode{pos=" + var10000 +  ", connections=" + this.connections.size() + "}";
    }




    public static enum CableMode {
        INPUT,
        OUTPUT;
    }
    public static enum DistributionStrategy {
        ROUND_ROBIN,
        NEAREST,
        FARTHEST;
    }




}
