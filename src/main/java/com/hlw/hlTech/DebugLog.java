package com.hlw.hlTech;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;

public class DebugLog {
    public static void log(String msg) {
        System.out.println("HLTEST: " + msg);

        try {
            Universe.get().getPlayers().forEach((p) -> p.sendMessage(Message.raw("[HLTEST]: " + msg)));
        } catch (Exception ex) {
        }

    }
}
