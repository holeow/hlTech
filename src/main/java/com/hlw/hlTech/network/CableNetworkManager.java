/*
 * PipeNetworkManager.class - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 30/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Renamed some variables fields, and functions to fit the new system
 * - Added the controller system in the code.
 * - Used the NodeHolder to hold both cables (pipes) and controllers.
 * - Added linking to the cached inventories in the network class
 * - Removed unused functions due to the controller system like the ones for cable input mode or type
 * - Changed the processItemTransfer function to reflect the centralization around the controller
 * - Changed the save and load to file to reflect the new centralization around the controller.
 * - Moved most of everything to the worldHolder class for each world to be separate
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */


package com.hlw.hlTech.network;

import com.hlw.hlTech.CableConfig;
import com.hlw.hlTech.DebugLog;
import com.hlw.hlTech.util.*;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState;
import org.bson.BsonDocument;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CableNetworkManager {

    private final Map<UUID, World> worldRegistry = new ConcurrentHashMap<>();
    public World getWorldFromRegistry(UUID uuid) {return  worldRegistry.get(uuid);}
    public final Map<UUID,WorldHolder> worldHolders = new ConcurrentHashMap<>();
/*
    private static final String SAVE_FILE_NAME = "pipe_networks.dat";
    private static final int SAVE_FORMAT_VERSION = 5;
    private static final long AUTO_SAVE_INTERVAL = 1200L;
    private static final int RETRY_INTERVAL = 20;
    private static final int MAX_RETRIES = 10;
    private final Map<UUID, Map<BlockPos, NodeHolder>> worldNodes = new ConcurrentHashMap<>();
    private final Map<UUID, Set<CableNetwork>> worldNetworks = new ConcurrentHashMap<>();
    private final Map<BlockPos, ItemContainer> inventoryCache = new ConcurrentHashMap<>();
    private final Set<CableRetryEntry> pendingConnections = ConcurrentHashMap.newKeySet();
    private final Set<ChunkRetryEntry> pendingChunks = ConcurrentHashMap.newKeySet();
    private long tickCount = 0L;
    private boolean dirty = false;
    private CableConfig config;
    public Map<UUID,Map<Long,ArrayList<NodeHolder>>> worldToLoad = new HashMap<>();


    public Set<ChunkRetryEntry> getPendingChucks(){return pendingChunks;}
    public void setConfig(CableConfig config) {
        this.config = config;
    }
    */

    public void ensureWorldRegistered(World world) {
        if (!this.worldRegistry.containsKey(world.getWorldConfig().getUuid())) {
            this.registerWorld(world.getWorldConfig().getUuid(), world);
        }

    }

    public void registerWorld(UUID worldId, World world) {
        boolean isNewWorld = !this.worldRegistry.containsKey(worldId);
        this.worldRegistry.put(worldId, world);
        this.worldHolders.compute(worldId, (a,b)-> {return b==null? new WorldHolder(a,this) : b;});
        if (isNewWorld && this.worldHolders.containsKey(worldId)) {
            PrintStream var10000 = System.out;
            var wh = this.worldHolders.get(worldId);
        }

    }
/*
    public void onPipePlaced(UUID worldId, BlockPos position) {
        this.dirty = true;
        CableNode newNode = new CableNode(worldId, position);
        NodeHolder newNodeHolder =new NodeHolder(newNode);
        (this.worldNodes.computeIfAbsent(worldId, (k) -> new ConcurrentHashMap<BlockPos,NodeHolder>())).put(position,newNodeHolder );
        Set<CableNetwork> adjacentNetworks = new HashSet();
        List<Direction> potentialInventories = new ArrayList();

        for(Direction dir : Direction.values()) {
            BlockPos neighborPos = position.offset(dir);
            NodeHolder adjNode = this.getCableAt(worldId, neighborPos);
            if (adjNode != null) {
                newNode.addConnection(dir);
                adjNode.addConnection(dir.getOpposite());
                if (adjNode.getNetwork() != null) {
                    adjacentNetworks.add(adjNode.getNetwork());
                }
            }
            else{
                World world = this.worldRegistry.get(worldId);
                if(world!= null && ChunkBlockHelper.hasInventoryAt(world,neighborPos)){
                    potentialInventories.add(dir);
                }
            }
        }

        this.mergeOrInitNetwork(worldId, newNodeHolder, adjacentNetworks);

        for(Direction dir : potentialInventories) {
            this.validateInventoryLink(worldId, newNodeHolder, position.offset(dir), dir);
        }

    }

    public void onControllerPlaced(UUID worldId,BlockPos position){
        this.dirty = true;
        DebugLog.log("OnControllerPlaced");
        ControllerNode newNode = new ControllerNode(worldId,position);
        NodeHolder newNodeHolder =new NodeHolder(newNode);
        ((Map<BlockPos,NodeHolder>)this.worldNodes.computeIfAbsent(worldId, (k) -> new ConcurrentHashMap<BlockPos,NodeHolder>())).put(position,newNodeHolder );
        Set<CableNetwork> adjacentNetworks = new HashSet();
        List<Direction> potentialInventories = new ArrayList();

        for(Direction dir : Direction.values()) {
            BlockPos neighborPos = position.offset(dir);
            NodeHolder adjNode = this.getCableAt(worldId, neighborPos);
            if (adjNode != null) {
                newNode.addConnection(dir);
                adjNode.addConnection(dir.getOpposite());
                if (adjNode.getNetwork() != null) {
                    adjacentNetworks.add(adjNode.getNetwork());
                }
            }
            else{
                World world = this.worldRegistry.get(worldId);
                if(world!= null && ChunkBlockHelper.hasInventoryAt(world,neighborPos)){
                    potentialInventories.add(dir);
                }
            }
        }

        this.mergeOrInitNetwork(worldId, newNodeHolder, adjacentNetworks);

        for(Direction dir : potentialInventories) {
            this.validateInventoryLink(worldId, newNodeHolder, position.offset(dir), dir);
        }
    }
*/
    /*
    public void onPotentialInventoryPlaced(UUID worldId, BlockPos inventoryPos) {

        Map<BlockPos, NodeHolder> worldCables = this.worldNodes.get(worldId);
        if (worldCables != null && !worldCables.isEmpty()) {
            for(NodeHolder cable : worldCables.values()) {
                BlockPos pipePos = cable.getPosition();
                int dx = Math.abs(pipePos.getX() - inventoryPos.getX());
                int dy = Math.abs(pipePos.getY() - inventoryPos.getY());
                int dz = Math.abs(pipePos.getZ() - inventoryPos.getZ());
                if (dx <= 3 && dy <= 3 && dz <= 3) {
                    for(Direction dir : Direction.values()) {
                        BlockPos neighborPos = pipePos.offset(dir);
                        if (neighborPos.equals(inventoryPos) && this.getCableAt(worldId, neighborPos) == null) {
                            this.pendingConnections.add(new CableRetryEntry(worldId, pipePos, neighborPos, dir, 0));
                        }
                    }
                }
            }
        }

    }

    public void onInventoryRemoved(UUID worldId, BlockPos removedPos) {
        Map<BlockPos, NodeHolder> pipes = this.worldNodes.get(worldId);
        if (pipes != null && !pipes.isEmpty()) {
            Set<BlockPos> inventoriesToRemove = new HashSet();

            for(BlockPos invPos : this.inventoryCache.keySet()) {
                if (invPos.equals(removedPos)) {
                    inventoriesToRemove.add(invPos);
                }
            }

            if (!inventoriesToRemove.isEmpty()) {
                World world = (World)this.worldRegistry.get(worldId);

                for(BlockPos invPosx : inventoriesToRemove) {
                    boolean stillValid = false;
                    if (world != null) {
                        stillValid = ChunkBlockHelper.hasInventoryAt(world, invPosx);
                    }

                    if (!stillValid) {
                        this.inventoryCache.remove(invPosx);

                        for(Direction dir : Direction.values()) {
                            BlockPos neighborPos = invPosx.offset(dir);
                            NodeHolder adjacentPipe = pipes.get(neighborPos);
                            if (adjacentPipe != null) {
                                Direction pipeToInventoryDir = dir.getOpposite();
                                adjacentPipe.removeConnection(pipeToInventoryDir);
                                CableNetwork network = adjacentPipe.getNetwork();
                                if (network != null) {
                                    network.removeInventory((invPosx));
                                    network.unmarkAsInput(adjacentPipe.getPosition());
                                    network.unmarkAsOutput(adjacentPipe.getPosition());
                                }
                            }
                        }
                    }
                }
            }
        }

    }
*/
    /*
    public void onCableRemoved(UUID worldId, BlockPos position) {
        this.dirty = true;
        Map<BlockPos, NodeHolder> pipes = this.worldNodes.get(worldId);
        if (pipes != null) {
            NodeHolder removedNode = pipes.remove(position);
            if (removedNode != null)
            {
                for(Direction dir : removedNode.getConnections()) {
                    NodeHolder neighbor = pipes.get(position.offset(dir));
                    if (neighbor != null) {
                        neighbor.removeConnection(dir.getOpposite());
                    }
                }

                CableNetwork oldNetwork = removedNode.getNetwork();
                if (oldNetwork != null) {
                    this.rebuildNetworksAfterRemoval(worldId, oldNetwork, position);
                }

                for(var d : Direction.values()){
                    var na = NetworksAround(removedNode.getWorldId(),removedNode.getPosition().offset(d));
                    if(!na.contains(removedNode.getNetwork())){
                        oldNetwork.connectedInventories.remove(position);
                    }
                    if(na.isEmpty()){
                        this.inventoryCache.remove(position);
                    }
                }

            }
        }

    }


    public void onControllerRemoved(UUID worldId,BlockPos position){
        this.dirty = true;
        Map<BlockPos,NodeHolder> cables = this.worldNodes.get(worldId);
        if(cables != null){
            NodeHolder removedNode = cables.remove(position);
            if(removedNode != null){
                for(Direction dir : removedNode.getConnections()){
                    NodeHolder neighbor = cables.get(position.offset(dir));
                    if(neighbor != null){
                        neighbor.removeConnection((dir.getOpposite()));
                    }
                }

                CableNetwork oldNetwork = removedNode.getNetwork();
                if (oldNetwork != null) {
                    this.rebuildNetworksAfterRemoval(worldId, oldNetwork, position);
                }
                for(var d : Direction.values()){
                    var na = NetworksAround(removedNode.getWorldId(),removedNode.getPosition().offset(d));
                    if(!na.contains(removedNode.getNetwork())){
                        oldNetwork.connectedInventories.remove(position);
                    }
                    if(na.isEmpty()){
                        this.inventoryCache.remove(position);
                    }
                }
            }
        }
    }
*/
    /*
    public ArrayList<CableNetwork> NetworksAround(UUID w,BlockPos pos)
    {
        var l = new ArrayList<CableNetwork>();
        for(var d : Direction.values())
        {
            var n = getCableAt(w,pos.offset(d));
            if(n!=null && !l.contains(n.getNetwork()))
            {
                l.add(n.getNetwork());
            }
        }
        return l;
    }*/
/*
    public CableNode.CableMode togglePipeMode(UUID worldId, BlockPos position) {
        CableNode node = this.getCableAt(worldId, position);
        if (node == null) {
            return null;
        } else {
            this.dirty = true;
            CableNode.CableMode newMode = node.getMode() == CableNode.CableMode.INPUT ? CableNode.CableMode.OUTPUT : CableNode.CableMode.INPUT;
            node.setMode(newMode);
            CableNetwork network = node.getNetwork();
            if (network != null) {
                if (newMode == CableNode.CableMode.INPUT) {
                    network.unmarkAsOutput(position);
                    network.markAsInput(position);
                } else {
                    network.unmarkAsInput(position);
                    network.markAsOutput(position);
                }
            }

            return newMode;
        }
    }

    public CableNode.CableMode setCableMode(UUID worldId, BlockPos position, CableNode.CableMode mode) {
        CableNode node = this.getCableAt(worldId, position);
        if (node == null) {
            return null;
        } else if (node.getMode() == mode) {
            return mode;
        } else {
            this.dirty = true;
            node.setMode(mode);
            CableNetwork network = node.getNetwork();
            if (network != null) {
                if (mode == CableNode.CableMode.INPUT) {
                    network.unmarkAsOutput(position);
                    network.markAsInput(position);
                } else {
                    network.unmarkAsInput(position);
                    network.markAsOutput(position);
                }
            }

            return mode;
        }
    }
*/
/*
    public boolean setCablePriority(UUID worldId, BlockPos position, int priority) {
        CableNode node = this.getCableAt(worldId, position);
        if (node == null) {
            return false;
        } else {
            priority = Math.max(0, Math.min(10, priority));
            if (node.getPriority() == priority) {
                return true;
            } else {
                this.dirty = true;
                node.setPriority(priority);
                return true;
            }
        }
    }

    public boolean setCableDistributionStrategy(UUID worldId, BlockPos position, CableNode.DistributionStrategy strategy) {
        CableNode node = this.getCableAt(worldId, position);
        if (node == null) {
            return false;
        } else if (node.getDistributionStrategy() == strategy) {
            return true;
        } else {
            this.dirty = true;
            node.setDistributionStrategy(strategy);
            return true;
        }
    }

*//*
    public NodeHolder getCableAt(UUID worldId, BlockPos position) {
        Map<BlockPos, NodeHolder> Nodes = (Map<BlockPos,NodeHolder>)this.worldNodes.get(worldId);
        return Nodes != null ? (NodeHolder)Nodes.get(position) : null;
    }


    public void onChunkLoaded(UUID worldId, long chunkId)
    {
        var w = this.worldToLoad.get(worldId);
        if(w!=null)
        {
            var c = w.get(chunkId);
            if(c!=null)
            {
                for(var n : c)
                {
                    for(var d : Direction.values())
                    {
                        var ne = n.getPosition().offset(d);
                        onPotentialInventoryPlaced(worldId,ne);
                    }
                }
            }
        }

    }

*/

/*

    public void tick() {
        ++this.tickCount;
        if (this.dirty && this.tickCount % 600L == 0L) {
            this.saveNetworks();
        }


        if (this.tickCount % 10L == 0L && !this.pendingConnections.isEmpty() ) {
            this.processInventoryConnectionRetries();


        }
        if(this.tickCount%50L == 0L){
            ArrayList<ChunkRemoveEntry> toremove = new ArrayList<>();

            for(var w : worldToLoad.entrySet()){
                var world = worldRegistry.get(w.getKey());
                if(world != null){
                    for(var c : w.getValue().entrySet())
                    {
                        boolean result = TryChunk(world,c.getKey());
                        if(result){
                            toremove.add(new ChunkRemoveEntry(w.getKey(),c.getKey()));
                        }
                    }





                }

            }
            DebugLog.log("REMOVING A NUMBER OF CHUNK TO LOAD: " + toremove.size());
            for(var ctr : toremove){
                worldToLoad.get(ctr.worldID).remove(ctr.chunkID);
            }
        }
        for(Map.Entry<UUID, World> worldEntry : this.worldRegistry.entrySet()) {
            UUID worldId = (UUID)worldEntry.getKey();
            World world = (World)worldEntry.getValue();
            if (world != null && world.isAlive()) {
                Set<CableNetwork> networks = (Set<CableNetwork>)this.worldNetworks.get(worldId);
                if (networks != null && !networks.isEmpty()) {
                    world.execute(() -> {
                        for(CableNetwork network : new HashSet<CableNetwork>(networks)) {
                            this.tickNetwork(worldId, network);
                        }

                    });
                }
            }
        }

    }
*/
    /*
    public boolean TryChunk(World w, long chunkId){
        var c= ChunkBlockHelper.isChunkLoaded(w,chunkId);
        if(c)
        {
            onChunkLoaded(w.getWorldConfig().getUuid(), chunkId);
            return true;
        }
        else {

            return false;
        }
    }


    public String getStats() {
        int totalPipes = this.worldNodes.values().stream().mapToInt(Map::size).sum();
        int totalNetworks = this.worldNetworks.values().stream().mapToInt(Set::size).sum();
        return "Cables: " + totalPipes + ", Networks: " + totalNetworks + ", Inventories: " + this.inventoryCache.size();
    }
*/
/*
    private void mergeOrInitNetwork(UUID worldId, NodeHolder newNode, Set<CableNetwork> adjacentNetworks) {
        if (adjacentNetworks.isEmpty()) {
            CableNetwork newNetwork = new CableNetwork();
            newNetwork.addNode(newNode);
            ((Set<CableNetwork>)this.worldNetworks.computeIfAbsent(worldId, (k) -> new HashSet<>())).add(newNetwork);
        } else if (adjacentNetworks.size() == 1) {
            ((CableNetwork)adjacentNetworks.iterator().next()).addNode(newNode);
        } else {
            Iterator<CableNetwork> iterator = adjacentNetworks.iterator();
            CableNetwork primaryNetwork = (CableNetwork)iterator.next();
            primaryNetwork.addNode(newNode);
            Set<CableNetwork> networks = this.worldNetworks.get(worldId);

            while(iterator.hasNext()) {
                CableNetwork networkToMerge = (CableNetwork)iterator.next();
                primaryNetwork.merge(networkToMerge);
                networks.remove(networkToMerge);
            }
        }

    }

    private void rebuildNetworksAfterRemoval(UUID worldId, CableNetwork oldNetwork, BlockPos removedPos) {
        Set<CableNetwork> networks = this.worldNetworks.get(worldId);
        if (networks != null) {
            networks.remove(oldNetwork);
            Set<BlockPos> remainingPositions = new HashSet<>();

            for(NodeHolder node : oldNetwork.getNodes()) {
                if (!node.getPosition().equals(removedPos)) {
                    remainingPositions.add(node.getPosition());
                    node.setNetwork((CableNetwork)null);
                }
            }

            while(!remainingPositions.isEmpty()) {
                BlockPos start = (BlockPos)remainingPositions.iterator().next();
                CableNetwork newNetwork = new CableNetwork();
                List<NodeHolder> nodesInNewNetwork = new ArrayList<>();
                Queue<BlockPos> queue = new LinkedList<>();
                queue.add(start);

                while(!queue.isEmpty()) {
                    BlockPos current = (BlockPos)queue.poll();
                    if (remainingPositions.remove(current)) {
                        NodeHolder nodex = this.getCableAt(worldId, current);
                        if (nodex != null) {
                            newNetwork.addNode(nodex);
                            nodesInNewNetwork.add(nodex);

                            for(Direction dir : nodex.getConnections()) {
                                BlockPos neighbor = current.offset(dir);
                                if (remainingPositions.contains(neighbor)) {
                                    queue.add(neighbor);
                                }
                            }
                        }
                    }
                }

                for(NodeHolder nodex : nodesInNewNetwork) {
                    if (this.hasInventoryConnection(worldId, nodex)) {

                        for(Direction dir : Direction.values()) {
                            this.validateInventoryLink(worldId, nodex, nodex.getPosition().offset(dir), dir);

                        }
                    }
                }

                if (!newNetwork.isEmpty()) {
                    networks.add(newNetwork);
                }
            }
        }

    }
    */
 /*
    public boolean hasInventoryConnection(UUID worldId, NodeHolder node) {
        for(Direction dir : node.getConnections()) {
            BlockPos neighborPos = node.getPosition().offset(dir);
            if (this.getCableAt(worldId, neighborPos) == null && this.inventoryCache.containsKey(neighborPos)) {
                return true;
            }
        }

        return false;
    }

    public void validateInventoryLink(UUID worldId, NodeHolder node, BlockPos neighborPos, Direction direction) {
        World world = this.worldRegistry.get(worldId);
        if (world != null) {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(neighborPos.getX(), neighborPos.getZ()));
            if (chunk == null) {
                this.pendingConnections.add(new CableRetryEntry(worldId, node.getPosition(), neighborPos, direction, 0));
            } else {
                this.checkInventoryConnectionWithChunk(worldId, node, neighborPos, direction, chunk);
            }
        }

    }

    private void checkInventoryConnectionWithChunk(UUID worldId, NodeHolder node, BlockPos neighborPos, Direction direction, WorldChunk chunk) {
        int y = neighborPos.getY();
        if (ChunkBlockHelper.isValidY(y)) {
            try {
                BlockState blockState = ChunkBlockHelper.getBlockStateFollowingFiller(chunk, neighborPos.getX(), y, neighborPos.getZ());
                if (blockState instanceof ItemContainerBlockState) {
                    ItemContainerBlockState containerState = (ItemContainerBlockState)blockState;
                    ItemContainer container = containerState.getItemContainer();
                    if (container == null) {
                        this.pendingConnections.add(new CableRetryEntry(worldId, node.getPosition(), neighborPos, direction, 0));
                        return;
                    }

                    this.registerInventoryConnection(node, neighborPos, direction, container);
                } else if (blockState == null) {
                }
            } catch (ArrayIndexOutOfBoundsException var10) {
                this.pendingConnections.add(new CableRetryEntry(worldId, node.getPosition(), neighborPos, direction, 0));
            }
        }

    }


    private void registerInventoryConnection(NodeHolder node, BlockPos inventoryPos, Direction direction, ItemContainer container) {
        this.inventoryCache.put(inventoryPos, container);
        node.addConnection(direction);
        CableNetwork network = node.getNetwork();
        network.connectedInventories.put(inventoryPos,container);
        if (network != null) {

        }

    }

    private void processInventoryConnectionRetries() {
        for(CableRetryEntry entry : new HashSet<CableRetryEntry>(this.pendingConnections)) {
            this.pendingConnections.remove(entry);
            NodeHolder node = this.getCableAt(entry.worldId(), entry.pipePos());
            if (node != null && !this.inventoryCache.containsKey(entry.inventoryPos()) && !this.tryInventoryConnectionRetry(entry.worldId(), node, entry.inventoryPos(), entry.direction()) && entry.retryCount() < 10) {
                this.pendingConnections.add(entry.withIncrementedRetry());
            }
        }

    }
*//*
    private boolean tryInventoryConnectionRetry(UUID worldId, NodeHolder node, BlockPos neighborPos, Direction direction) {
        World world = this.worldRegistry.get(worldId);
        if (world == null) {
            return false;
        } else {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(neighborPos.getX(), neighborPos.getZ()));
            if (chunk == null) {
                return false;
            } else {
                int y = neighborPos.getY();
                if (!ChunkBlockHelper.isValidY(y)) {
                    return false;
                } else {
                    try {
                        BlockState state = ChunkBlockHelper.getBlockStateFollowingFiller(chunk, neighborPos.getX(), y, neighborPos.getZ());
                        if (state instanceof ItemContainerBlockState) {
                            ItemContainerBlockState containerState = (ItemContainerBlockState)state;
                            ItemContainer container = containerState.getItemContainer();
                            if (container != null) {
                                this.registerInventoryConnection(node, neighborPos, direction, container);
                                return true;
                            }
                        }
                    } catch (Exception var11) {
                    }

                    return false;
                }
            }
        }
    }
*/
    /*
    private void refreshInventoryLinks(UUID worldId) {
        Map<BlockPos, NodeHolder> pipes = this.worldNodes.get(worldId);
        if (pipes != null && !pipes.isEmpty()) {
            for(NodeHolder node : pipes.values()) {
                for(Direction dir : Direction.values()) {
                    BlockPos neighborPos = node.getPosition().offset(dir);
                    if (this.getCableAt(worldId, neighborPos) == null) {
                        this.validateInventoryLink(worldId, node, neighborPos, dir);
                    }
                }
            }
        }

    }

    private void tickNetwork(UUID worldId, CableNetwork network) {
        network.tick();
        if(network.getTickCount() %10 == 0){
            this.processItemTransfer(network);
        }

    }
*//*
    private void processItemTransfer(CableNetwork network) {
        DebugLog.log("ProcessItemTransfer");
        for(NodeHolder holder : network.getNodes()){
            if(holder.controllerNode!= null){
                for(BlockPos start : holder.controllerNode.InputsAndOutputs.keySet()){
                    var w = worldRegistry.get(holder.getWorldId());
                    if(w == null) continue;
                    if(!network.connectedInventories.containsKey(start))continue;
                    ItemContainer startContainer = ChunkBlockHelper.getItemContainer(w,start );
                    if(startContainer != null){
                        DebugLog.log("startContainer not null!");
                        var endPoints =holder.controllerNode.InputsAndOutputs.get(start);
                        if(endPoints== null)continue;
                        for(OutputData endPoint : endPoints){
                            if(!network.connectedInventories.containsKey(endPoint.outputPos))continue;

                            ItemContainer endContainer = ChunkBlockHelper.getItemContainer(worldRegistry.get(holder.getWorldId()),endPoint.outputPos );
                            if(endContainer != null){
                                this.transferItems(startContainer,endContainer,1,endPoint);
                                holder.onTransfer();
                                network.onTransferSuccess(holder.getPosition());
                            }
                        }
                    }
                }
            }
        }
    }

    private ItemContainer findAdjacentInventory(NodeHolder node) {
        World world = (World)this.worldRegistry.get(node.getWorldId());
        if (world == null) {
            return null;
        } else {
            for(Direction dir : Direction.values()) {
                BlockPos adjacent = node.getPosition().offset(dir);
                if (this.getCableAt(node.getWorldId(), adjacent) == null && this.inventoryCache.containsKey(adjacent)) {
                    WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(adjacent.getX(), adjacent.getZ()));
                    if (chunk != null) {
                        try {
                            BlockState state = ChunkBlockHelper.getBlockStateFollowingFiller(chunk, adjacent.getX(), adjacent.getY(), adjacent.getZ());
                            if (state instanceof ItemContainerBlockState) {
                                ItemContainerBlockState containerState = (ItemContainerBlockState)state;
                                ItemContainer container = containerState.getItemContainer();
                                if (container != null) {
                                    this.inventoryCache.put(adjacent, container);
                                    node.getNetwork().connectedInventories.put(adjacent,container);
                                    return container;
                                }
                            }
                        } catch (Exception var12) {
                            return (ItemContainer)this.inventoryCache.get(adjacent);
                        }
                    }
                }
            }

            return null;
        }
    }
*//*
    private int transferItems(ItemContainer source, ItemContainer dest, int maxItemsPerSlot,OutputData outputdata) {
        int transferred = 0;
        short startSlot = 0;
        short endSlot = source.getCapacity();
        //REMOVE THIS WHEN U MAKE A FILTER !
        Object sourceFilter = null;
        Object destFilter = null;
        if (source instanceof CombinedItemContainer combined) {
            if (combined.getContainersSize() > 1) {
                ItemContainer outputContainer = combined.getContainer(combined.getContainersSize() - 1);
                startSlot = (short)(source.getCapacity() - outputContainer.getCapacity());
            }
        }
        DebugLog.log("transferItems");
        for(short slot = startSlot; slot < endSlot; ++slot) {
            ItemStack stack = source.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                if (outputdata != null) {
                    String itemId = stack.getItemId();
                    if(outputdata.allowAll){
                        DebugLog.log("allowAll");
                        if(outputdata.itemIds.contains(itemId)) continue;
                    }
                    else{
                        DebugLog.log("disallowAll");
                        if(!outputdata.itemIds.contains(itemId))continue;
                    }

                }

                }

                int countToTransfer = Math.min(stack.getQuantity(), maxItemsPerSlot);
                ItemStack toTransfer = stack.withQuantity(countToTransfer);
                if (toTransfer != null) {
                    ItemStackTransaction addResult = dest.addItemStack(toTransfer);
                    if (addResult.succeeded()) {
                        ItemStack remainder = addResult.getRemainder();
                        int actuallyAdded = countToTransfer;
                        if (remainder != null && !remainder.isEmpty()) {
                            actuallyAdded = countToTransfer - remainder.getQuantity();
                        }

                        if (actuallyAdded > 0) {
                            int remaining = stack.getQuantity() - actuallyAdded;
                            if (remaining <= 0) {
                                source.removeItemStackFromSlot(slot);
                            } else {
                                source.removeItemStackFromSlot(slot, actuallyAdded);
                            }

                            transferred += actuallyAdded;
                            return transferred;
                        }
                    }
                }
            }
        }

        return transferred;
    }
*/
    /*
    private Path getSaveFilePath() {
        return Path.of("plugins", "hltech").toAbsolutePath().resolve("cable_networks.dat");
    }


    public synchronized void saveNetworks() {
        Path saveFile = this.getSaveFilePath();
        Map<UUID, List<NodeHolder>> pipeSnapshot = new HashMap<>();
        int totalPipes = 0;

        for(Map.Entry<UUID, Map<BlockPos, NodeHolder>> worldEntry : this.worldNodes.entrySet()) {
            List<NodeHolder> nodes = new ArrayList<>((worldEntry.getValue()).values());
            pipeSnapshot.put((UUID)worldEntry.getKey(), nodes);
            totalPipes += nodes.size();
        }

        try {
            if (saveFile.getParent() != null) {
                Files.createDirectories(saveFile.getParent());
            }

            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(saveFile)))) {
                out.writeInt(5);
                out.writeInt(totalPipes);

                for(Map.Entry<UUID, List<NodeHolder>> worldEntry : pipeSnapshot.entrySet()) {
                    UUID worldId = (UUID)worldEntry.getKey();

                    for(NodeHolder node : (List<NodeHolder>)worldEntry.getValue()) {
                        out.writeLong(worldId.getMostSignificantBits());
                        out.writeLong(worldId.getLeastSignificantBits());
                        out.writeInt(node.type.ordinal());
                        out.writeInt(node.getPosition().getX());
                        out.writeInt(node.getPosition().getY());
                        out.writeInt(node.getPosition().getZ());
                        if(node.type == NodeHolder.NodeType.CONTROLLERNODE){
                            out.writeInt(node.controllerNode.InputsAndOutputs.size());
                            for(var item : node.controllerNode.InputsAndOutputs.entrySet()){
                                out.writeInt(item.getKey().getX());
                                out.writeInt(item.getKey().getY());
                                out.writeInt(item.getKey().getZ());
                                out.writeInt(item.getValue().size());
                                for(var itm : item.getValue()){
                                    itm.writeToDataStream(out);
                                }

                            }
                        }
                        //out.writeUTF(node.getPipeType().name());
                        //out.writeUTF(node.getMode().name());
                        //out.writeInt(node.getPriority());
                        //out.writeUTF(node.getDistributionStrategy().name());

                    }
                }
            }

            System.out.println("Saved " + totalPipes + " pipes");
            this.dirty = false;
        } catch (IOException var12) {
            System.out.println("Failed to save pipe networks: " + var12.getMessage());
        }

    }
*/
    /*
    public void loadNetworks() {
        Path saveFile = this.getSaveFilePath();
        if (!Files.exists(saveFile, new LinkOption[0])) {
            System.out.println("No saved pipe networks found");
        } else {
            try {
                try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(saveFile)))) {
                    int version = in.readInt();
                    if (version > 5) {
                        System.out.println("Save file version " + version + " is newer than supported");
                    }

                    int pipeCount = in.readInt();
                    if (pipeCount < 0 || pipeCount > 1000000) {
                        System.out.println("Invalid pipe count: " + pipeCount);
                        return;
                    }

                    List<LoadedCableData> loadedPipes = new ArrayList<>(pipeCount);
                    HashMap<BlockPos,ArrayList<OutputData>> IO = new HashMap<>();
                    for(int i = 0; i < pipeCount; ++i) {
                        try {
                            UUID worldId = new UUID(in.readLong(), in.readLong());
                            NodeHolder.NodeType ty = NodeHolder.NodeType.values()[in.readInt()];
                            BlockPos position = new BlockPos(in.readInt(), in.readInt(), in.readInt());
                            if(ty == NodeHolder.NodeType.CONTROLLERNODE){
                                int IOsize = in.readInt();
                                IO = new HashMap<>();
                                for(int j = 0; j<IOsize; j++){
                                    BlockPos inputPos = new BlockPos(in.readInt(),in.readInt(),in.readInt());
                                    int Osize = in.readInt();
                                    ArrayList<OutputData> positions = new ArrayList<>();
                                    for(int k = 0;k<Osize;k++){
                                        var outputdata = OutputData.readFromDataStream(in);
                                        positions.add(outputdata);
                                    }
                                    IO.put(inputPos,positions);
                                }
                            }
                            //CableType pipeType = CableType.valueOf(in.readUTF());
                            //CableNode.CableMode mode = CableNode.CableMode.valueOf(in.readUTF());
                            //int priority = version >= 2 ? in.readInt() : 0;
                            //CableNode.DistributionStrategy strategy = version >= 3 ? CableNode.DistributionStrategy.valueOf(in.readUTF()) : CableNode.DistributionStrategy.ROUND_ROBIN;
                            loadedPipes.add(new LoadedCableData(worldId,ty, position,IO, pipeType, mode, priority, strategy));
                            if (version >= 4) {
                                boolean hasFilter = in.readBoolean();
                                if (hasFilter) {
                                    String filterModeStr = in.readUTF();
                                    int itemCount = in.readInt();
                                    List<String> filterItems = new ArrayList();

                                    for(int j = 0; j < itemCount; ++j) {
                                        String itemId = in.readUTF();
                                        filterItems.add(itemId.isEmpty() ? null : itemId);
                                    }

                                    ItemFilter filter = new ItemFilter(FilterMode.valueOf(filterModeStr), filterItems);
                                    ((LoadedPipeData)loadedPipes.get(loadedPipes.size() - 1)).setFilter(filter);
                                    if (version >= 5) {
                                        boolean hasMetadata = in.readBoolean();
                                        if (hasMetadata) {
                                            String json = in.readUTF();

                                            try {
                                                BsonDocument metadata = BsonDocument.parse(json);
                                                ((LoadedPipeData)loadedPipes.get(loadedPipes.size() - 1)).setMetadata(metadata);
                                            } catch (Exception var22) {
                                                System.out.println("Failed to parse filter metadata for pipe " + i);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (IllegalArgumentException var23) {
                            System.out.println("Skipping invalid pipe " + i);
                        }
                    }

                    if (!loadedPipes.isEmpty()) {
                        this.rebuildNetworksFromLoadedData(loadedPipes);
                        System.out.println("Loaded " + loadedPipes.size() + " pipes");
                    }
                }

                return;
            } catch (IOException var15) {
                System.out.println("Failed to load pipe networks: " + var15.getMessage());
            }
        }

    }
*//*
    private void rebuildNetworksFromLoadedData(List<LoadedCableData> loadedPipes) {
        for(LoadedCableData data : loadedPipes) {
            NodeHolder node = null;
            if(data.type == NodeHolder.NodeType.CABLENODE){
                node = new NodeHolder( new CableNode(data.worldId, data.position));
            }
            else if(data.type == NodeHolder.NodeType.CONTROLLERNODE){
                node = new NodeHolder((new ControllerNode(data.worldId,data.position)));
                node.controllerNode.InputsAndOutputs = data.controllerData;
            }

            AddNodeToChunkToLoad(node);
            //node.setMode(data.mode);
            //node.setPriority(data.priority);
            //node.setDistributionStrategy(data.strategy);

            (this.worldNodes.computeIfAbsent(data.worldId, (k) -> new ConcurrentHashMap<>())).put(data.position, node);
        }

        for(LoadedCableData data : loadedPipes) {
            NodeHolder node = this.getCableAt(data.worldId, data.position);
            if (node != null) {
                Set<CableNetwork> adjacentNetworks = new HashSet<>();

                for(Direction dir : Direction.values()) {
                    NodeHolder neighbor = this.getCableAt(data.worldId, data.position.offset(dir));
                    if (neighbor != null) {
                        node.addConnection(dir);
                        neighbor.addConnection(dir.getOpposite());
                        if (neighbor.getNetwork() != null) {
                            adjacentNetworks.add(neighbor.getNetwork());
                        }
                    }
                }

                if (node.getNetwork() == null) {
                    this.mergeOrInitNetwork(data.worldId, node, adjacentNetworks);
                }
            }

        }

        PrintStream var10000 = System.out;
        Stream var10001 = this.worldNetworks.values().stream();
        var10000.println("Rebuilt " + var10001.mapToInt((a)-> ((HashSet)a).size()).sum() + " networks");

    }
*//*
    public void AddNodeToChunkToLoad(NodeHolder node){
        var world = worldToLoad.get(node.getWorldId());
        ArrayList<NodeHolder> chunk = null;
        var chunkId= ChunkBlockHelper.getChunkIdAt(node.getPosition());

        if(world!=null){
            chunk = world.get(chunkId);
            if(chunk!= null)chunk.add(node);
            else{
                chunk = new ArrayList<>();
                chunk.add(node);
                world.put(chunkId,chunk);
            }
        }
        else{
            var hm = new HashMap<Long,ArrayList<NodeHolder>>();
            var al = new ArrayList<NodeHolder>();
            al.add(node);
            hm.put(chunkId,al);
            worldToLoad.put(node.getWorldId(),hm);
        }
    }

*/
    /*
    private static class LoadedCableData {
        final UUID worldId;
        final BlockPos position;
        final NodeHolder.NodeType type;
        final Map<BlockPos,ArrayList<OutputData>> controllerData;

        BsonDocument metadata;

        LoadedCableData(UUID worldId,NodeHolder.NodeType type, BlockPos position, Map<BlockPos,ArrayList<OutputData>> controllerData) {
            this.worldId = worldId;
            this.type = type;
            this.position = position;
            this.controllerData = controllerData;

        }

        void setMetadata(BsonDocument metadata) {
            this.metadata = metadata;
        }
    }

    private static record CableRetryEntry(UUID worldId, BlockPos pipePos, BlockPos inventoryPos, Direction direction, int retryCount) {
        CableRetryEntry withIncrementedRetry() {
            return new CableRetryEntry(this.worldId, this.pipePos, this.inventoryPos, this.direction, this.retryCount + 1);
        }
    }
    public void AddChunckRetryEntry(WorldChunk chunk){
        this.pendingChunks.add(new ChunkRetryEntry(chunk));
    }
    public class ChunkRetryEntry{
        public WorldChunk chunk;
        public int retryCount = 0;

        public ChunkRetryEntry(WorldChunk chunk){
            this.chunk = chunk;
        }
    }
    public class ChunkRemoveEntry{
        public UUID worldID;
        public long chunkID;

        public ChunkRemoveEntry(UUID worldID, long chunkID){
            this.worldID = worldID;
            this.chunkID = chunkID;
        }
    }*/
}
