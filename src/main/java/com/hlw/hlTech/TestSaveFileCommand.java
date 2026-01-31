package com.hlw.hlTech;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class TestSaveFileCommand extends CommandBase {
    private final String pluginName;
    private final String pluginVersion;

    public TestSaveFileCommand(String pluginName, String pluginVersion) {
        super("testsavefile", "Prints a test message from the " + pluginName + " plugin. A number is iterated and saved to a file.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
    }


    private Path getSaveFilePath() {
        return Path.of("plugins", "hltech").toAbsolutePath().resolve("hltech_debugsavefile.dat");
    }
    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {

        Path saveFile = this.getSaveFilePath();

        try
        {
            if (saveFile.getParent() != null)
            {
                Files.createDirectories(saveFile.getParent());
            }

            int current = 0;
            if (!Files.exists(saveFile, new LinkOption[0]))
            {
                ctx.sendMessage(Message.raw("No saved file"));
            }
            else
            {
                try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(saveFile)))) {
                    current = in.readInt();

                        ctx.sendMessage(Message.raw("Number = " + current +" at " + Path.of("plugins", "hltech").toAbsolutePath().toString()));
                }

            }
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(saveFile)))) {
                out.writeInt(current+1);
            }


        }
        catch (
            IOException err) {
                 ctx.sendMessage(Message.raw("Failed to use savefile " + err.getMessage()));
            }


    }
}