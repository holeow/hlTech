/*
 * BlockPos.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Added the FromString function to deserialize from a string to a BlockPos.
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */

package com.hlw.hlTech.util;

import com.hypixel.hytale.math.vector.Vector3i;

import javax.annotation.Nonnull;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class BlockPos {
    private final int x;
    private final int y;
    private final int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public BlockPos(@Nonnull Vector3i vec) {
        this.x = vec.getX();
        this.y = vec.getY();
        this.z = vec.getZ();
    }
    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    @Nonnull
    public Vector3i toVector3i() {
        return new Vector3i(this.x, this.y, this.z);
    }
    public BlockPos offset(Direction direction) {
        return new BlockPos(this.x + direction.getOffsetX(), this.y + direction.getOffsetY(), this.z + direction.getOffsetZ());
    }

    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(this.x + dx, this.y + dy, this.z + dz);
    }


    public int manhattanDistance(BlockPos other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y) + Math.abs(this.z - other.z);
    }

    @Nonnull
    public static BlockPos fromVector3i(@Nonnull Vector3i vec) {
        return new BlockPos(vec.getX(), vec.getY(), vec.getZ());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            BlockPos blockPos = (BlockPos)o;
            return this.x == blockPos.x && this.y == blockPos.y && this.z == blockPos.z;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.x, this.y, this.z});
    }

    public String toString() {
        return "BlockPos{" + this.x + "," + this.y + "," + this.z + "}";
    }
    public static BlockPos FromString(String BlockPosString){
        String cut = BlockPosString.substring(9,BlockPosString.length()-1);
        String[] posStrings = cut.split(",");
        return new BlockPos(Integer.parseInt(posStrings[0]),Integer.parseInt(posStrings[1]),Integer.parseInt(posStrings[2]));
    }
    public void writeToDataStream (DataOutputStream stream) throws IOException {
        stream.writeInt(getX());
        stream.writeInt(getY());
        stream.writeInt(getZ());
    }
    public static BlockPos readFromDataStream(DataInputStream stream) throws IOException {
        return new BlockPos(stream.readInt(),stream.readInt(),stream.readInt());
    }
}
