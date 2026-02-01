/*
 * PipeConfigGui.java - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Renamed some variables fields, and functions to fit the new system
 * - Added the controller system in the code.
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */

package com.hlw.hlTech;

import com.hlw.hlTech.network.CableNetworkManager;
import com.hlw.hlTech.network.NodeHolder;
import com.hlw.hlTech.network.OutputData;
import com.hlw.hlTech.util.BlockPos;
import com.hlw.hlTech.util.ChunkBlockHelper;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.UUID;

public class ControllerConfigGui extends InteractiveCustomUIPage<ControllerConfigGui.ControllerEventData> {
    private static final Value<String> MODE_BUTTON_STYLE = Value.ref("Common.ui", "SecondaryTextButtonStyle");
    private static final Value<String> MODE_BUTTON_SELECTED_STYLE = Value.ref("Common.ui", "DefaultTextButtonStyle");
    private final World world;
    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;
    private final PlayerRef playerRef;
    private BlockPos selectedInput = null;
    private BlockPos selectedOutput = null;

    public ControllerConfigGui(@Nonnull PlayerRef playerRef, @Nonnull World world, int x, int y, int z) {
        super(playerRef, CustomPageLifetime.CanDismiss, ControllerConfigGui.ControllerEventData.CODEC);
        this.playerRef = playerRef;
        this.world = world;
        this.worldId = world.getWorldConfig().getUuid();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void build(@NonNull Ref<EntityStore> ref, @NonNull UICommandBuilder uiCommandBuilder, @NonNull UIEventBuilder uiEventBuilder, @NonNull Store<EntityStore> store) {



                uiCommandBuilder.append("Pages/ControllerConfigPage.ui");
                uiCommandBuilder.append("#Content", "Pages/ControllerConfigContent.ui");
                uiCommandBuilder.set("#PositionLabel.Text", "Position: " + this.x + ", " + this.y + ", " + this.z);

                this.buildContainerGrid(ref,uiCommandBuilder,uiEventBuilder,store);

            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", (new EventData()).append("Action", "close"), false);
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,  "#BackToControllerButton", (new EventData()).append("Action", "backtocontroller"), false);



    }
    protected void buildContainerGrid(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        CableNetworkManager manager = CablePlugin.getInstance().getPipeNetworkManager();
        commandBuilder.clear("#ItemGrid");
        commandBuilder.set("#ItemIconInput.Visible", false);
        commandBuilder.set("#PositionLabelInput.Visible",false);
        commandBuilder.set("#ItemIconOutput.Visible", false);
        commandBuilder.set("#PositionLabelOutput.Visible",false);
        commandBuilder.set("#BackToInputButton.Visible",false);
        commandBuilder.set("#BackToOutputButton.Visible",false);
        commandBuilder.set("#PlayerInventorySection.Visible",false);


        if(manager == null){
            commandBuilder.set("#Title.Text", "Error: Cable Manager not found");

            return;

        }
        else {
            var wh = manager.worldHolders.get(this.worldId);
            if(wh == null) {
                commandBuilder.set("#Title.Text", "Error: world holder not found");
                return;
            }
            try{
                wh.acquireLock();

                NodeHolder node = wh.getNodeAt(new BlockPos(this.x,this.y,this.z));
                if(node == null || node.controllerNode == null){
                    commandBuilder.set("#Title.Text", "Error: Controller not found");
                    return;
                }
                else{
                    int i = 0;

                    int maxDistance = 0;
                    for(var inventory : node.getNetwork().connectedInventories.keySet()){
                        var dist = node.getPosition().XZDifference(inventory);
                        if(maxDistance< dist) maxDistance = dist;
                    }

                    var cardinalPoint = "";
                        HeadRotation headrotation = ref.getStore().getComponent(ref,HeadRotation.getComponentType());
                        if(headrotation != null){
                            var yawDegrees = Math.toDegrees(headrotation.getRotation().getYaw());
                            yawDegrees = (yawDegrees % 360 + 360) % 360; // Normalize to 0-360
                            if (yawDegrees >= 315 || yawDegrees < 45) {
                                cardinalPoint=( "NORTH");
                            } else if (yawDegrees >= 45 && yawDegrees < 135) {
                                cardinalPoint=( "WEST");
                            } else if (yawDegrees >= 135 && yawDegrees < 225) {
                                cardinalPoint=( "SOUTH");
                            } else {
                                cardinalPoint=( "EAST");
                            }
                    }
                    else DebugLog.log("headrotation null");

                    if (cardinalPoint.equals("")) cardinalPoint = "NORTH";

                    for (var inventory : node.getNetwork().connectedInventories.entrySet()){
                        var key = inventory.getKey();
                        String selector = "#ItemGrid[" + i + "]";
                        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(key.getX(),  key.getZ()));

                        BlockType type = ChunkBlockHelper.getBlockTypeAt(chunk,key.getX(),key.getY(),key.getZ());
                        if(type == null) continue;

                        var item = type.getItem();
                        if(item == null) continue;

                        String itemId = type.getItem().getId();
                        commandBuilder.append("#ItemGrid", "Pages/ControllerContainerSlot.ui");

                        commandBuilder.set(selector + " #ItemIcon.ItemId",itemId);
                        commandBuilder.set(selector + " #PositionLabel.Text","[" + key.getX() + ","+key.getY()+","+key.getZ()+"]");
                        var dist = (float)node.getPosition().XZDifference(inventory.getKey());
                        var Zdiff =  inventory.getKey().getZ()-node.getPosition().getZ();
                        var Xdiff =  inventory.getKey().getX()- node.getPosition().getX();
                        var Mdiff = node.getPosition().manhattanDistance(inventory.getKey());
                        var total = (float)(Math.abs(Zdiff) + Math.abs(Xdiff));
                        var distByTotal = ((float)maxDistance)/dist;
                        var normalizedZ = (((float)Zdiff) /  total)/distByTotal;
                        var normalizedX = (((float)Xdiff) /  total)/distByTotal;



                        var anch = new Anchor();
                        anch.setWidth(Value.of(32));
                        anch.setHeight(Value.of(32));

                        switch (cardinalPoint){
                            case "NORTH":
                                anch.setLeft(Value.of(55+(int)(((float)55)*normalizedX)));
                                anch.setTop(Value.of(55+(int)(((float)55)*normalizedZ)));
                                break;
                            case  "SOUTH":
                                anch.setLeft(Value.of(55+(int)(((float)55)*(-normalizedX))));
                                anch.setTop(Value.of(55+(int)(((float)55)*(-normalizedZ))));
                                break;
                            case "EAST":
                                anch.setLeft(Value.of(55+(int)(((float)55)*(normalizedZ))));
                                anch.setTop(Value.of(55+(int)(((float)55)*(-normalizedX))));
                                break;
                            case "WEST":
                                anch.setLeft(Value.of(55+(int)(((float)55)*(-normalizedZ))));
                                anch.setTop(Value.of(55+(int)(((float)55)*normalizedX)));
                                break;
                        }

                        String imageloc = "";
                        if(node.getPosition().getY()> inventory.getKey().getY()){
                            imageloc = "Pages/circle-red.png";
                        }
                        else if(node.getPosition().getY()== inventory.getKey().getY()){
                            imageloc = "Pages/circle-white.png";
                        }
                        else {
                            imageloc = "Pages/circle-green.png";
                        }
                        PatchStyle patch = new PatchStyle(Value.of(imageloc));
                        commandBuilder.setObject(selector + " #PositionCircle.Anchor", anch);
                        commandBuilder.setObject(selector + " #PositionCircle.Background", patch);
                        commandBuilder.set(selector + " #DistanceLabel.Text",""+ Mdiff);


                        var outputList = node.controllerNode.InputsAndOutputs.get(new BlockPos(key.getX(),key.getY(),key.getZ()));

                        if(outputList!=null && !outputList.isEmpty()){
                            commandBuilder.set(selector + " #InfoLabelInputOutput.Visible",true);
                            commandBuilder.set(selector + " #InfoLabelInputOutput.Text", outputList.size() + " inventory output"+ (outputList.size()>1 ?  "s" : "" )+" with this input.");

                        }

                        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector + " #ItemButton", (new EventData()).append("Action", "selectinput:" + key), false);

                        i++;
                    }
                }
            }
            finally {
                wh.releaseLock();
            }


        }
    }



        public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ControllerEventData data) {
        CableNetworkManager manager = CablePlugin.getInstance().getPipeNetworkManager();
        if (manager == null) {
            this.close();
        } else {

                String action = data.action;
                String value = data.value;
                if(action.equals("close")){
                    this.close();
                }
                else if (action.equals("test")) {
                    UICommandBuilder builder = new UICommandBuilder();
                    UIEventBuilder eventBuilder = new UIEventBuilder();
                    builder.clear("#Content");
                    builder.append("#Content", "Pages/ControllerConfigTest.ui");
                    this.sendUpdate(builder, eventBuilder, false);
                }
                else if (action.startsWith("selectinput:") )
                {
                    BlockPos pos = null;
                    try{
                        pos = BlockPos.FromString(action.split(":")[1]);

                    }
                    catch(Exception e){
                        DebugLog.log("Error building Blockpos in UI : " + e.getMessage());
                        return;
                    }
                    UICommandBuilder builder = new UICommandBuilder();
                    UIEventBuilder event = new UIEventBuilder();
                    var wh = manager.worldHolders.get(store.getExternalData().getWorld().getWorldConfig().getUuid());
                    if(wh == null){
                        DebugLog.log("World holder is null ! (UI selectinput)");
                        this.close();
                        return;
                    }
                    try{
                        wh.acquireLock();
                        var node = wh.getNodeAt( new BlockPos(this.x,this.y,this.z));
                        if(node == null || node.controllerNode == null){
                            DebugLog.log("Controller is is null ! (UI selectinput)");
                            this.close();
                            return;
                        }

                        this.handleSelectInput(ref,store,data,node,pos,builder,event);

                    }
                    finally {
                        wh.releaseLock();
                    }

                    this.sendUpdate(builder,event,false);


                }
                else if (action.startsWith("selectoutput:")){
                    UICommandBuilder builder = new UICommandBuilder();
                    UIEventBuilder eventBuilder = new UIEventBuilder();
                    //builder.clear("#Content");
                    BlockPos pos = null;
                    try{
                        pos = BlockPos.FromString(action.split(":")[1]);

                    }
                    catch(Exception e){
                        DebugLog.log("Error building Blockpos in UI : " + e.getMessage());
                        return;
                    }
                    if(pos == null){
                        DebugLog.log("Error building Blockpos in UI (null)" );
                        return;
                    }
                    selectedOutput = pos;
                    var wh = manager.worldHolders.get(store.getExternalData().getWorld().getWorldConfig().getUuid());
                    if(wh == null){
                        DebugLog.log("World holder is null ! (UI selectoutput)");
                        this.close();
                        return;
                    }
                    try{
                        wh.acquireLock();
                        var node = wh.getNodeAt( new BlockPos(this.x,this.y,this.z));
                        if(node == null || node.controllerNode == null){
                            DebugLog.log("Controller is is null ! (UI selectoutput)");
                            this.close();
                            return;
                        }

                        if(node.controllerNode.InputsAndOutputs.get(this.selectedInput) != null && node.controllerNode.InputsAndOutputs.get(this.selectedInput).stream().anyMatch((a)-> a.outputPos.equals(selectedOutput))){
                            node.controllerNode.RemoveInputOutput(this.selectedInput,this.selectedOutput);
                        }
                        else{
                            node.controllerNode.AddOrModifyInputOutput(this.selectedInput,new OutputData(this.selectedOutput,0,true,new ArrayList<>()));
                        }

                        handleSelectInput(ref,store,data,node,selectedInput, builder,eventBuilder);

                    }
                    finally {
                        wh.releaseLock();
                    }



                        this.sendUpdate(builder,eventBuilder,false);



                }
                else if(action.equals("backtocontroller")){

                    UICommandBuilder builder = new UICommandBuilder();
                    UIEventBuilder event = new UIEventBuilder();
                    this.buildContainerGrid(ref,builder,event,store);
                    this.sendUpdate(builder,event,false);
                }
                else if(action.startsWith("swaplistmode:")){
                    UICommandBuilder builder = new UICommandBuilder();
                    UIEventBuilder event = new UIEventBuilder();
                    BlockPos pos = null;
                    try{
                        pos = BlockPos.FromString(action.split(":")[1]);

                    }
                    catch(Exception e){
                        DebugLog.log("Error building Blockpos in UI : " + e.getMessage());
                        return;
                    }
                    if(pos == null){
                        DebugLog.log("Error building Blockpos in UI (null)" );
                        return;
                    }
                    BlockPos finalPos = pos;

                    var wh = manager.worldHolders.get(store.getExternalData().getWorld().getWorldConfig().getUuid());
                    if(wh == null){
                        DebugLog.log("World holder is null ! (UI swaplistmode)");
                        this.close();
                        return;
                    }
                    try{
                        wh.acquireLock();
                        var node = wh.getNodeAt( new BlockPos(this.x,this.y,this.z));
                        if(node == null || node.controllerNode == null){
                            DebugLog.log("Controller is is null ! (UI swaplistmode)");
                            this.close();
                            return;
                        }
                        var dataset = node.controllerNode.InputsAndOutputs.get(this.selectedInput).stream().filter(a-> a.outputPos.equals(finalPos)).toList();
                        if(dataset.isEmpty()) {
                            DebugLog.log("dataset  is empty ! (UI swaplistmode)");
                            this.close();
                            return;
                        }
                        else{
                            dataset.getFirst().allowAll = !dataset.getFirst().allowAll;
                            handleSelectInput(ref,store,data,node,selectedInput,builder,event);

                        }


                    }
                    finally {
                        wh.releaseLock();
                    }
                    this.sendUpdate(builder,event,false);




                }
                else if (action.startsWith("selectlistmode:")){
                    BlockPos pos = null;
                    try{
                        pos = BlockPos.FromString(action.split(":")[1]);

                    }
                    catch(Exception e){
                        DebugLog.log("Error building Blockpos in UI : " + e.getMessage());
                        return;
                    }
                    UICommandBuilder builder = new UICommandBuilder();
                    UIEventBuilder event = new UIEventBuilder();


                    var wh = manager.worldHolders.get(store.getExternalData().getWorld().getWorldConfig().getUuid());
                    if(wh == null){
                        DebugLog.log("World holder is null ! (UI swaplistmode)");
                        this.close();
                        return;
                    }
                    try{
                        wh.acquireLock();
                        var node = wh.getNodeAt( new BlockPos(this.x,this.y,this.z));
                        if(node == null || node.controllerNode == null){
                            DebugLog.log("Controller is is null ! (UI swaplistmode)");
                            this.close();
                            return;
                        }

                        this.handleSelectListMode(ref,store,data,node,pos,builder,event);

                    }
                    finally {
                        wh.releaseLock();
                    }


                    this.sendUpdate(builder,event,false);

                }
                else if(action.startsWith("addtofilter:")){
                    String itemId = action.split(":")[1];

                    UICommandBuilder builder = new UICommandBuilder();
                    UIEventBuilder event = new UIEventBuilder();

                    BlockPos finalPos = selectedOutput;


                    var wh = manager.worldHolders.get(store.getExternalData().getWorld().getWorldConfig().getUuid());
                    if(wh == null){
                        DebugLog.log("World holder is null ! (UI addtofilter)");
                        this.close();
                        return;
                    }
                    try{
                        wh.acquireLock();
                        var node = wh.getNodeAt( new BlockPos(this.x,this.y,this.z));
                        if(node == null || node.controllerNode == null){
                            DebugLog.log("Controller is is null ! (UI addtofilter)");
                            this.close();
                            return;
                        }

                        var dataset = node.controllerNode.InputsAndOutputs.get(this.selectedInput).stream().filter(a-> a.outputPos.equals(finalPos)).toList();
                        if(dataset.isEmpty())return;
                        else{
                            var l = dataset.getFirst().itemIds;
                            if(!l.contains(itemId))l.add(itemId);
                        }

                        this.handleSelectListMode(ref,store,data,node,selectedOutput,builder,event);

                    }
                    finally {
                        wh.releaseLock();
                    }


                    this.sendUpdate(builder,event,false);
                }
                else if(action.startsWith("removefromfilter:")){
                    String itemId = action.split(":")[1];

                    UICommandBuilder builder = new UICommandBuilder();
                    UIEventBuilder event = new UIEventBuilder();

                    BlockPos finalPos = selectedOutput;



                    var wh = manager.worldHolders.get(store.getExternalData().getWorld().getWorldConfig().getUuid());
                    if(wh == null){
                        DebugLog.log("World holder is null ! (UI removefromfilter)");
                        this.close();
                        return;
                    }
                    try{
                        wh.acquireLock();
                        var node = wh.getNodeAt( new BlockPos(this.x,this.y,this.z));
                        if(node == null || node.controllerNode == null){
                            DebugLog.log("Controller is is null ! (UI removefromfilter)");
                            this.close();
                            return;
                        }

                        var dataset = node.controllerNode.InputsAndOutputs.get(this.selectedInput).stream().filter(a-> a.outputPos.equals(finalPos)).toList();
                        if(dataset.isEmpty())return;
                        else{
                            var l = dataset.getFirst().itemIds;
                            l.remove(itemId);
                        }

                        this.handleSelectListMode(ref,store,data,node,selectedOutput,builder,event);

                    }
                    finally {
                        wh.releaseLock();
                    }



                    this.sendUpdate(builder,event,false);
                }


        }
    }

    public void handleSelectListMode(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ControllerEventData data,NodeHolder node, BlockPos pos, UICommandBuilder commandBuilder, UIEventBuilder event){
        UIEventBuilder eventBuilder =event;
        selectedOutput = pos;
        if(pos == null){
            DebugLog.log("Error building Blockpos in UI (null)" );
            return;
        }
        if(node == null || node.controllerNode == null){
            DebugLog.log("Error:Node or controller is null." );
            return;
        }
        else{
            int i = 0;
            commandBuilder.clear("#ItemGrid");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,  "#BackToInputButton", (new EventData()).append("Action", "selectinput:" + selectedInput), false);
            var selectedOutputList = node.controllerNode.InputsAndOutputs.get(selectedInput);
            if(selectedOutputList == null) return;
            var selectedOutputData = selectedOutputList.stream().filter(a-> a.outputPos.equals(selectedOutput)).toList().getFirst();
            if(selectedOutputData == null)return;
            commandBuilder.append("#ItemGrid","Pages/ControllerFilterContainer.ui");
            for(var itemId : selectedOutputData.itemIds){
                String selector = "#FilterGrid[" + i + "]";
                commandBuilder.append("#FilterGrid", "Pages/ControllerFilterSlot.ui");
                commandBuilder.set(selector + " #ItemIcon.ItemId",itemId);
                Item itemAsset = (Item)Item.getAssetMap().getAsset(itemId);
                String translationKey = itemAsset != null ? itemAsset.getTranslationKey() : itemId;
                commandBuilder.set(selector + ".TooltipText", Message.translation(translationKey));
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector , (new EventData()).append("Action", "removefromfilter:" + itemId), false);

                i++;
            }
            commandBuilder.set("#PlayerInventorySection.Visible",true);

            Player player = (Player)store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                ItemContainer inv = player.getInventory().getCombinedHotbarFirst();
                commandBuilder.clear("#PlayerGrid");
                int visualIndex = 0;

                for(int j = 0; j < inv.getCapacity(); ++j) {
                    ItemStack stack = inv.getItemStack((short)j);
                    if (stack != null && !stack.isEmpty()) {
                        String selector = "#PlayerGrid[" + visualIndex + "]";
                        commandBuilder.append("#PlayerGrid", "Pages/ControllerFilterSlot.ui");
                        commandBuilder.set(selector + " #ItemIcon.ItemId", stack.getItemId());
                        Item itemAsset = (Item)Item.getAssetMap().getAsset(stack.getItemId());
                        String translationKey = itemAsset != null ? itemAsset.getTranslationKey() : stack.getItemId();
                        commandBuilder.set(selector + ".TooltipText", Message.translation(translationKey));
                        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, (new EventData()).append("Action", "addtofilter:" + stack.getItemId()), false);
                        ++visualIndex;
                    }
                }

            }

        }
    }

    public void handleSelectInput(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ControllerEventData data,NodeHolder node, BlockPos pos, UICommandBuilder commandBuilder, UIEventBuilder event) {
        UICommandBuilder builder = commandBuilder;
        UIEventBuilder eventBuilder =event;
        //builder.clear("#Content");

        selectedInput = pos;
        if(pos == null){
            DebugLog.log("Error building Blockpos in UI (null)" );
            return;
        }
        if(node == null || node.controllerNode == null){
            DebugLog.log("Error:Node or controller is null." );
            return;
        }
        else{
            int i = 0;



            int maxDistance = 0;
            for(var inventory : node.getNetwork().connectedInventories.keySet()){
                var dist = node.getPosition().XZDifference(inventory);
                if(maxDistance< dist) maxDistance = dist;
            }

            var cardinalPoint = "";
            HeadRotation headrotation = ref.getStore().getComponent(ref,HeadRotation.getComponentType());
            if(headrotation != null){
                var yawDegrees = Math.toDegrees(headrotation.getRotation().getYaw());
                yawDegrees = (yawDegrees % 360 + 360) % 360; // Normalize to 0-360
                if (yawDegrees >= 315 || yawDegrees < 45) {
                    cardinalPoint=( "NORTH");
                } else if (yawDegrees >= 45 && yawDegrees < 135) {
                    cardinalPoint=( "WEST");
                } else if (yawDegrees >= 135 && yawDegrees < 225) {
                    cardinalPoint=( "SOUTH");
                } else {
                    cardinalPoint=( "EAST");
                }
            }
            else DebugLog.log("headrotation null");

            if (cardinalPoint.equals("")) cardinalPoint = "NORTH";



            builder.clear("#ItemGrid");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,  "#BackToInputButton", (new EventData()).append("Action", "selectinput:" + selectedInput), false);
            var selectedOutputList = node.controllerNode.InputsAndOutputs.get(pos);
            for (var inventory : node.getNetwork().connectedInventories.entrySet()){
                var key = inventory.getKey();
                WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(key.getX(),  key.getZ()));

                BlockType type = ChunkBlockHelper.getBlockTypeAt(chunk,key.getX(),key.getY(),key.getZ());
                if(key.equals(pos)){
                    if(type!=null){
                        var item = type.getItem();
                        if(item != null)
                            builder.set("#ItemIconInput.ItemId",item.getId());
                        builder.set("#PositionLabelInput.Text","[" + key.getX() + ","+key.getY()+","+key.getZ()+"]");
                        builder.set("#BackToInputButton.Visible",true);
                        builder.set("#ItemIconInput.Visible", true);
                        builder.set("#PositionLabelInput.Visible",true);

                    }


                    continue;
                }
                String selector = "#ItemGrid[" + i + "]";

                if(type == null) continue;
                var item = type.getItem();
                if(item == null) continue;
                String itemId = type.getItem().getId();
                builder.append("#ItemGrid", "Pages/ControllerContainerSlot.ui");

                builder.set(selector + " #ItemIcon.ItemId",itemId);
                builder.set(selector + " #PositionLabel.Text","[" + key.getX() + ","+key.getY()+","+key.getZ()+"]");

                var dist = (float)node.getPosition().XZDifference(inventory.getKey());
                var Zdiff =  inventory.getKey().getZ()-node.getPosition().getZ();
                var Xdiff =  inventory.getKey().getX()- node.getPosition().getX();
                var Mdiff = node.getPosition().manhattanDistance(inventory.getKey());
                var total = (float)(Math.abs(Zdiff) + Math.abs(Xdiff));
                var distByTotal = ((float)maxDistance)/dist;
                var normalizedZ = (((float)Zdiff) /  total)/distByTotal;
                var normalizedX = (((float)Xdiff) /  total)/distByTotal;



                var anch = new Anchor();
                anch.setWidth(Value.of(32));
                anch.setHeight(Value.of(32));

                switch (cardinalPoint){
                    case "NORTH":
                        anch.setLeft(Value.of(55+(int)(((float)55)*normalizedX)));
                        anch.setTop(Value.of(55+(int)(((float)55)*normalizedZ)));
                        break;
                    case  "SOUTH":
                        anch.setLeft(Value.of(55+(int)(((float)55)*(-normalizedX))));
                        anch.setTop(Value.of(55+(int)(((float)55)*(-normalizedZ))));
                        break;
                    case "EAST":
                        anch.setLeft(Value.of(55+(int)(((float)55)*(normalizedZ))));
                        anch.setTop(Value.of(55+(int)(((float)55)*(-normalizedX))));
                        break;
                    case "WEST":
                        anch.setLeft(Value.of(55+(int)(((float)55)*(-normalizedZ))));
                        anch.setTop(Value.of(55+(int)(((float)55)*normalizedX)));
                        break;
                }

                String imageloc = "";
                if(node.getPosition().getY()> inventory.getKey().getY()){
                    imageloc = "Pages/circle-red.png";
                }
                else if(node.getPosition().getY()== inventory.getKey().getY()){
                    imageloc = "Pages/circle-white.png";
                }
                else {
                    imageloc = "Pages/circle-green.png";
                }
                PatchStyle patch = new PatchStyle(Value.of(imageloc));
                commandBuilder.setObject(selector + " #PositionCircle.Anchor", anch);
                commandBuilder.setObject(selector + " #PositionCircle.Background", patch);
                commandBuilder.set(selector + " #DistanceLabel.Text",""+ Mdiff);


                var outputpos = new BlockPos(key.getX(),key.getY(),key.getZ());
                if(selectedOutputList!=null ){
                    var dataset = selectedOutputList.stream().filter(a-> a.outputPos.equals(outputpos)).toList();
                    if(dataset.size()>0){
                        builder.set(selector + " #InfoLabelInputOutput.Visible",true);
                        builder.set(selector + " #InfoLabelInputOutput.Text","Set as output !");
                        builder.set(selector + " #OutputDataGroup.Visible",true);
                        builder.set((selector) + " #SetListMode.Text", dataset.get(0).allowAll ? "Blacklist" : "Whitelist");
                    }

                }
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector + " #SetListMode", (new EventData()).append("Action", "swaplistmode:" + key), false);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector + " #SelectListMode", (new EventData()).append("Action", "selectlistmode:" + key), false);

                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector + " #ItemButton", (new EventData()).append("Action", "selectoutput:" + key), false);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", (new EventData()).append("Action", "close"), false);



                i++;
            }
            builder.set("#BackToOutputButton.Visible",false);
            builder.set("#PlayerInventorySection.Visible",false);

        }




    }


    public static class ControllerEventData {
        public static final BuilderCodec<ControllerEventData> CODEC;
        public String action = "";
        public String value = "";

        static {
            CODEC = ((BuilderCodec.<ControllerEventData>builder(ControllerEventData.class, ControllerEventData::new)
                    .append(new KeyedCodec<>("Action", Codec.STRING),
                            (e, v) -> e.action = v, (e) -> e.action)
                    .add())
                    .append(new KeyedCodec<>("Value", Codec.STRING),
                            (ControllerEventData e, String v) -> e.value = v != null ? v : "", (ControllerEventData e) -> e.value)
                    .add())
                    .build();
        }
    }
}
