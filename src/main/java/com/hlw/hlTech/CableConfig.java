/*
 * PipeConfig.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Renamed some variables fields, and functions, changed some strings to fit the new system
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */

package com.hlw.hlTech;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CableConfig {
    private static final String CONFIG_FILE = "cable_config.json";
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    public long transferIntervalMs = 100L;
    public int itemsPerTransfer = 1;
    public boolean debugMode = false;
    public Map<String, List<Integer>> extractionRules = new HashMap();

    public static CableConfig load() {
        File file = new File("cable_config.json");
        if (!file.exists()) {
            CableConfig config = new CableConfig();
            config.save();
            return config;
        } else {
            try (FileReader reader = new FileReader(file)) {
                return (CableConfig)GSON.fromJson(reader, CableConfig.class);
            } catch (IOException e) {
                System.out.println("[CableConfig] Error loading config: " + e.getMessage());
                return new CableConfig();
            }
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter("cable_config.json")) {
            GSON.toJson(this, writer);
            PrintStream var10000 = System.out;
            File var10001 = new File("cable_config.json");
            var10000.println("[CableConfig] Created default config at: " + var10001.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[CableConfig] Error saving config: " + e.getMessage());
        }

    }
}
