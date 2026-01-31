/*
 * Direction.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */

package com.hlw.hlTech.util;

public enum Direction {

    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    EAST(1, 0, 0),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0);
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    private Direction(int offsetX, int offsetY, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public int getOffsetX() {
        return this.offsetX;
    }

    public int getOffsetY() {
        return this.offsetY;
    }

    public int getOffsetZ() {
        return this.offsetZ;
    }
    public Direction getOpposite() {
        switch (this.ordinal()) {
            case 0 -> {
                return SOUTH;
            }
            case 1 -> {
                return NORTH;
            }
            case 2 -> {
                return WEST;
            }
            case 3 -> {
                return EAST;
            }
            case 4 -> {
                return DOWN;
            }
            case 5 -> {
                return UP;
            }
            default -> throw new IllegalStateException("Unknown direction: " + String.valueOf(this));
        }
    }
}
