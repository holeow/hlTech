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
 * - Changed file structure to have one file per world
 * - Added chunk reload system
 * - Changed the tick system to use the GlobalUpdateSystem (Hytale per-world ticking system)
 * - Added a lock to make sure it is thread safe.
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */

package com.hlw.hlTech.network;

import com.hlw.hlTech.DebugLog;
import com.hlw.hlTech.util.BlockPos;
import com.hlw.hlTech.util.ChunkBlockHelper;
import com.hlw.hlTech.util.Direction;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class WorldHolder {
    private double currentTime = 0d;
    private double updateTime = 1;
    private long updateAmount = 0;
    private final UUID uuid;
    private final CableNetworkManager manager;

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Map<BlockPos, NodeHolder> nodes =  new HashMap<>();
    private final ArrayList<CableNetwork> networks = new ArrayList<>();
    public Map<Long,ArrayList<NodeHolder>> chunksToLoad = new HashMap<>();
    private final ArrayList<CableRetryEntry> pendingConnections = new ArrayList<>();
    private final Map<BlockPos, ItemContainer> inventoryCache = new ConcurrentHashMap<>();
    private final Map<Long,ArrayList<BlockPos>> inventoryPositionsToRecheck = new HashMap<>();



    private boolean dirty;


    public WorldHolder(UUID uuid,CableNetworkManager manager){
        this.uuid = uuid;
        this.manager = manager;
    }



    public void acquireLock(){
        lock.lock();
    }
    public void releaseLock(){
        lock.unlock();
    }

    public Map<BlockPos,NodeHolder> getNodes(){
        return nodes;
    }

    public NodeHolder getNodeAt(BlockPos pos){

            return nodes.get(pos);

    }

    public ArrayList<CableNetwork> getNetworks(){
        return networks;
    }



















    public void tick(double deltaTime){
        currentTime+= deltaTime;
        if(currentTime>updateTime){
            currentTime-=updateTime;
            this.update();
        }
    }
    private void update(){
        updateAmount++;

        try{
            acquireLock();


            if (this.dirty && this.updateAmount % 60L == 0L) {
                this.saveNetworks();
            }


            if (!this.pendingConnections.isEmpty() ) {
                this.processInventoryConnectionRetries();


            }
            if(this.updateAmount%5L == 0L)
            {

                ArrayList<Long> toremove = new ArrayList<>();


                for(var c : chunksToLoad.entrySet())
                {
                    boolean result = TryChunk(c.getKey());
                    if(result){
                        toremove.add(c.getKey());
                    }
                }




                for(var ctr : toremove){
                    chunksToLoad.remove(ctr);
                }
            }

            var world = this.manager.getWorldFromRegistry(uuid);
            if (world != null && world.isAlive())
            {
                ArrayList<CableNetwork> networks = this.networks;
                if (networks != null && !networks.isEmpty())
                {

                    for(var n : networks)
                        this.updateNetwork(n);
                }

            }




        }
        finally {
            releaseLock();
        }

    }
    private void updateNetwork( CableNetwork network) {
            this.processItemTransfer(network);
    }

    public void onCablePlaced( BlockPos position) {
        this.dirty = true;
        CableNode newNode = new CableNode(uuid, position);
        NodeHolder newNodeHolder =new NodeHolder(newNode);
        this.nodes.put(position,newNodeHolder);
        Set<CableNetwork> adjacentNetworks = new HashSet();
        List<Direction> potentialInventories = new ArrayList();

        for(Direction dir : Direction.values()) {
            BlockPos neighborPos = position.offset(dir);
            NodeHolder adjNode = this.getNodeAt(neighborPos);
            if (adjNode != null) {
                newNode.addConnection(dir);
                adjNode.addConnection(dir.getOpposite());
                if (adjNode.getNetwork() != null) {
                    adjacentNetworks.add(adjNode.getNetwork());
                }
            }
            else{
                World world = this.manager.getWorldFromRegistry(uuid);
                if(world!= null && ChunkBlockHelper.hasInventoryAt(world,neighborPos)){
                    potentialInventories.add(dir);
                }
            }
        }

        this.mergeOrInitNetwork(newNodeHolder, adjacentNetworks);

        for(Direction dir : potentialInventories) {
            this.validateInventoryLink(newNodeHolder, position.offset(dir), dir);
        }

    }

    public void onControllerPlaced(BlockPos position){
        this.dirty = true;
        ControllerNode newNode = new ControllerNode(uuid,position);
        NodeHolder newNodeHolder =new NodeHolder(newNode);
        this.nodes.put(position,newNodeHolder );
        Set<CableNetwork> adjacentNetworks = new HashSet<>();
        List<Direction> potentialInventories = new ArrayList<>();

        for(Direction dir : Direction.values()) {
            BlockPos neighborPos = position.offset(dir);
            NodeHolder adjNode = this.getNodeAt(neighborPos);
            if (adjNode != null) {
                newNode.addConnection(dir);
                adjNode.addConnection(dir.getOpposite());
                if (adjNode.getNetwork() != null) {
                    adjacentNetworks.add(adjNode.getNetwork());
                }
            }
            else{
                World world = this.manager.getWorldFromRegistry(uuid);
                if(world!= null && ChunkBlockHelper.hasInventoryAt(world,neighborPos)){
                    potentialInventories.add(dir);
                }
            }
        }

        this.mergeOrInitNetwork(newNodeHolder, adjacentNetworks);

        for(Direction dir : potentialInventories) {
            this.validateInventoryLink( newNodeHolder, position.offset(dir), dir);
        }
    }

    public void onPotentialInventoryPlaced( BlockPos inventoryPos) {

        Map<BlockPos, NodeHolder> worldCables = this.nodes;
        if (!worldCables.isEmpty()) {
            for(NodeHolder node : worldCables.values()) {
                BlockPos nodePos = node.getPosition();
                int dx = Math.abs(nodePos.getX() - inventoryPos.getX());
                int dy = Math.abs(nodePos.getY() - inventoryPos.getY());
                int dz = Math.abs(nodePos.getZ() - inventoryPos.getZ());
                if (dx <= 3 && dy <= 3 && dz <= 3) {
                    for(Direction dir : Direction.values()) {
                        BlockPos neighborPos = nodePos.offset(dir);
                        if (neighborPos.equals(inventoryPos) && this.getNodeAt( neighborPos) == null) {
                            this.pendingConnections.add(new CableRetryEntry( nodePos, neighborPos, dir, 0));
                        }
                    }
                }
            }
        }

    }
    public void onInventoryRemoved( BlockPos removedPos)
    {
        Map<BlockPos, NodeHolder> nodes = this.nodes;
        if (!nodes.isEmpty())
        {
            Set<BlockPos> inventoriesToRemove = new HashSet<>();

            for(BlockPos invPos : this.inventoryCache.keySet())
            {
                if (invPos.equals(removedPos))
                {
                    inventoriesToRemove.add(invPos);
                }
            }

            if (!inventoriesToRemove.isEmpty())
            {
                World world = this.manager.getWorldFromRegistry(uuid);

                for(BlockPos invPosx : inventoriesToRemove)
                {
                    boolean stillValid = false;
                    if (world != null)
                    {
                        stillValid = ChunkBlockHelper.hasInventoryAt(world, invPosx);
                    }

                    if (!stillValid)
                    {

                        this.inventoryCache.remove(invPosx);

                        for(Direction dir : Direction.values())
                        {
                            BlockPos neighborPos = invPosx.offset(dir);
                            NodeHolder adjacentPipe = nodes.get(neighborPos);
                            if (adjacentPipe != null)
                            {

                                Direction pipeToInventoryDir = dir.getOpposite();
                                adjacentPipe.removeConnection(pipeToInventoryDir);
                                CableNetwork network = adjacentPipe.getNetwork();
                                if (network != null)
                                {
                                    network.removeInventory((invPosx));
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    public void onCableRemoved( BlockPos position)
    {

        this.dirty = true;
        Map<BlockPos, NodeHolder> nodes = this.nodes;

        NodeHolder removedNode = nodes.remove(position);
        if (removedNode != null)
        {
            for(Direction dir : removedNode.getConnections()) {
                NodeHolder neighbor = nodes.get(position.offset(dir));
                if (neighbor != null) {
                    neighbor.removeConnection(dir.getOpposite());
                }
            }

            CableNetwork oldNetwork = removedNode.getNetwork();
            if (oldNetwork != null) {
                this.rebuildNetworksAfterRemoval( oldNetwork, position);
            }

        }


    }


    public void onControllerRemoved(BlockPos position){
        this.dirty = true;
        Map<BlockPos,NodeHolder> nodes = this.nodes;

        NodeHolder removedNode = nodes.remove(position);
        if(removedNode != null){
            for(Direction dir : removedNode.getConnections()){
                NodeHolder neighbor = nodes.get(position.offset(dir));
                if(neighbor != null){
                    neighbor.removeConnection((dir.getOpposite()));
                }
            }

            CableNetwork oldNetwork = removedNode.getNetwork();
            if (oldNetwork != null) {
                this.rebuildNetworksAfterRemoval(oldNetwork, position);
            }

        }

    }

    private void mergeOrInitNetwork(NodeHolder newNode, Set<CableNetwork> adjacentNetworks) {
        if (adjacentNetworks.isEmpty()) {
            CableNetwork newNetwork = new CableNetwork();
            newNetwork.addNode(newNode);
            this.networks.add(newNetwork);
        } else if (adjacentNetworks.size() == 1) {
            adjacentNetworks.iterator().next().addNode(newNode);
        } else {
            Iterator<CableNetwork> iterator = adjacentNetworks.iterator();
            CableNetwork primaryNetwork = iterator.next();
            primaryNetwork.addNode(newNode);
            ArrayList<CableNetwork> networks = this.networks;

            while(iterator.hasNext()) {
                CableNetwork networkToMerge = iterator.next();
                primaryNetwork.merge(networkToMerge);
                networks.remove(networkToMerge);
            }
        }

    }
    public void validateInventoryLink(NodeHolder node, BlockPos neighborPos, Direction direction) {
        World world = this.manager.getWorldFromRegistry(this.uuid);
        if (world != null) {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(neighborPos.getX(), neighborPos.getZ()));
            if (chunk == null) {
                this.pendingConnections.add(new CableRetryEntry( node.getPosition(), neighborPos, direction, 0));
            } else {
                this.checkInventoryConnectionWithChunk( node, neighborPos, direction, chunk);
            }
        }

    }
    private void checkInventoryConnectionWithChunk(NodeHolder node, BlockPos neighborPos, Direction direction, WorldChunk chunk) {
        int y = neighborPos.getY();
        if (ChunkBlockHelper.isValidY(y)) {
            try {
                BlockState blockState = ChunkBlockHelper.getBlockStateFollowingFiller(chunk, neighborPos.getX(), y, neighborPos.getZ());
                if (blockState instanceof ItemContainerBlockState) {
                    ItemContainerBlockState containerState = (ItemContainerBlockState)blockState;
                    ItemContainer container = containerState.getItemContainer();
                    if (container == null) {
                        this.pendingConnections.add(new CableRetryEntry( node.getPosition(), neighborPos, direction, 0));
                        return;
                    }

                    this.registerInventoryConnection(node, neighborPos, direction, container);
                } else if (blockState == null) {
                }
            } catch (ArrayIndexOutOfBoundsException var10) {
                this.pendingConnections.add(new CableRetryEntry(node.getPosition(), neighborPos, direction, 0));
            }
        }

    }
    private void registerInventoryConnection(NodeHolder node, BlockPos inventoryPos, Direction direction, ItemContainer container) {
        this.inventoryCache.put(inventoryPos, container);
        node.addConnection(direction);
        CableNetwork network = node.getNetwork();
        network.connectedInventories.put(inventoryPos,container);
    }
    public void onChunkLoaded(long chunkId)
    {

            var c = chunksToLoad.get(chunkId);
            var world = manager.getWorldFromRegistry(uuid);
            if(c!=null)
            {
                for(var n : c)
                {
                    for(var d : Direction.values())
                    {

                        var ne = n.getPosition().offset(d);
                        long offsetChunkId = ChunkBlockHelper.getChunkIdAt(ne);
                        if( offsetChunkId!= chunkId  && world.getChunkIfLoaded(offsetChunkId) == null){
                            var itl = inventoryPositionsToRecheck.get(offsetChunkId);
                            if(itl == null){
                                itl = new ArrayList<>();
                                itl.add(ne);
                                inventoryPositionsToRecheck.put(offsetChunkId,itl);
                            }
                            else{
                                if(!itl.contains(ne)){
                                    itl.add(ne);
                                }
                            }
                        }
                        else{
                            onPotentialInventoryPlaced(ne);

                        }
                    }
                }
            }
            var inventoriesToRecheck = inventoryPositionsToRecheck.get(chunkId);
            if(inventoriesToRecheck!=null ){
                for(var i : inventoriesToRecheck){
                    onPotentialInventoryPlaced(i);
                }
                inventoryPositionsToRecheck.remove(chunkId);
            }


    }
    public boolean TryChunk( long chunkId){
        var c= ChunkBlockHelper.isChunkLoaded(this.manager.getWorldFromRegistry(uuid),chunkId);
        if(c)
        {
            onChunkLoaded( chunkId);
            return true;
        }
        else {

            return false;
        }
    }
    private void rebuildNetworksAfterRemoval( CableNetwork oldNetwork, BlockPos removedPos) {
        ArrayList<CableNetwork> networks = this.networks;
        if (networks != null) {
            networks.remove(oldNetwork);
            Set<BlockPos> remainingPositions = new HashSet<>();

            for(NodeHolder node : oldNetwork.getNodes()) {
                if (!node.getPosition().equals(removedPos)) {
                    remainingPositions.add(node.getPosition());
                    node.setNetwork(null);
                }
            }

            while(!remainingPositions.isEmpty()) {
                BlockPos start = remainingPositions.iterator().next();
                CableNetwork newNetwork = new CableNetwork();
                List<NodeHolder> nodesInNewNetwork = new ArrayList<>();
                Queue<BlockPos> queue = new LinkedList<>();
                queue.add(start);

                while(!queue.isEmpty()) {
                    BlockPos current = (BlockPos)queue.poll();
                    if (remainingPositions.remove(current)) {
                        NodeHolder nodex = this.getNodeAt(current);
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
                    if (this.hasInventoryConnection(nodex)) {

                        for(Direction dir : Direction.values()) {
                            this.validateInventoryLink( nodex, nodex.getPosition().offset(dir), dir);

                        }
                    }
                }

                if (!newNetwork.isEmpty()) {
                    networks.add(newNetwork);
                }
            }
        }

    }
    public boolean hasInventoryConnection( NodeHolder node) {
        for(Direction dir : node.getConnections()) {
            BlockPos neighborPos = node.getPosition().offset(dir);
            if (this.getNodeAt(neighborPos) == null && this.inventoryCache.containsKey(neighborPos)) {
                return true;
            }
        }

        return false;
    }
    private void processInventoryConnectionRetries() {
        for(CableRetryEntry entry : new HashSet<CableRetryEntry>(this.pendingConnections)) {
            this.pendingConnections.remove(entry);
            NodeHolder node = this.getNodeAt(entry.nodePos());
            if (node != null && !node.getNetwork().connectedInventories.containsKey(entry.inventoryPos()) && !this.tryInventoryConnectionRetry(node, entry.inventoryPos(), entry.direction()) && entry.retryCount() < 10) {
                this.pendingConnections.add(entry.withIncrementedRetry());
            }
        }

    }

    private boolean tryInventoryConnectionRetry( NodeHolder node, BlockPos neighborPos, Direction direction) {
        World world = this.manager.getWorldFromRegistry(uuid);
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
    public void refreshInventoryLinks() {
        Map<BlockPos, NodeHolder> nodes = this.nodes;
        if (nodes != null && !nodes.isEmpty()) {
            for(NodeHolder node : nodes.values()) {
                for(Direction dir : Direction.values()) {
                    BlockPos neighborPos = node.getPosition().offset(dir);
                    if (this.getNodeAt( neighborPos) == null) {
                        this.validateInventoryLink(node, neighborPos, dir);
                    }
                }
            }
        }

    }
    private void processItemTransfer(CableNetwork network)
    {
        for(NodeHolder holder : network.getNodes())
        {
            if(holder.controllerNode!= null)
            {
                for(BlockPos start : holder.controllerNode.InputsAndOutputs.keySet())
                {
                    var w =manager.getWorldFromRegistry(holder.getWorldId());
                    if(w == null) continue;
                    if(!network.connectedInventories.containsKey(start))continue;
                    ItemContainer startContainer = ChunkBlockHelper.getItemContainer(w,start );
                    if(startContainer != null)
                    {
                        var endPoints =holder.controllerNode.InputsAndOutputs.get(start);
                        if(endPoints== null)continue;
                        for(OutputData endPoint : endPoints)
                        {
                            if(!network.connectedInventories.containsKey(endPoint.outputPos))continue;

                            ItemContainer endContainer = ChunkBlockHelper.getItemContainer(w,endPoint.outputPos );
                            if(endContainer != null)
                            {
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

    private int transferItems(ItemContainer source, ItemContainer dest, int maxItemsPerSlot,OutputData outputdata) {
        int transferred = 0;
        short startSlot = 0;
        short endSlot = source.getCapacity();
        if (source instanceof CombinedItemContainer combined) {
            if (combined.getContainersSize() > 1) {
                ItemContainer outputContainer = combined.getContainer(combined.getContainersSize() - 1);
                startSlot = (short)(source.getCapacity() - outputContainer.getCapacity());
            }
        }
        for(short slot = startSlot; slot < endSlot; ++slot) {
            ItemStack stack = source.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                if (outputdata != null) {
                    String itemId = stack.getItemId();
                    if(outputdata.allowAll){
                        if(outputdata.itemIds.contains(itemId)) continue;
                    }
                    else{
                        if(!outputdata.itemIds.contains(itemId))continue;
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

    private Path getSaveFilePath() {
        return Path.of("plugins", "hltech", "worlds").toAbsolutePath().resolve(uuid.toString()+".cable_networks.dat");
    }
    public synchronized void saveNetworks() {
        Path saveFile = this.getSaveFilePath();
        List<NodeHolder> nodeSnapshot = new ArrayList<>();
        int totalNodes = 0;

        for( Map.Entry<BlockPos, NodeHolder> worldEntry : this.nodes.entrySet()) {

            nodeSnapshot.add( worldEntry.getValue());
            totalNodes += 1;
        }

        try {
            if (saveFile.getParent() != null) {
                Files.createDirectories(saveFile.getParent());
            }

            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(saveFile))))
            {
                out.writeInt(1);
                out.writeInt(totalNodes);

                for(NodeHolder node : nodeSnapshot)
                {
                    out.writeInt(node.type.ordinal());
                    out.writeInt(node.getPosition().getX());
                    out.writeInt(node.getPosition().getY());
                    out.writeInt(node.getPosition().getZ());
                    if(node.type == NodeHolder.NodeType.CONTROLLERNODE)
                    {
                        out.writeInt(node.controllerNode.InputsAndOutputs.size());
                        for(var item : node.controllerNode.InputsAndOutputs.entrySet())
                        {
                            out.writeInt(item.getKey().getX());
                            out.writeInt(item.getKey().getY());
                            out.writeInt(item.getKey().getZ());
                            out.writeInt(item.getValue().size());
                            for(var itm : item.getValue())
                            {
                                itm.writeToDataStream(out);
                            }

                        }
                    }

                }
            }

            System.out.println("Saved " + totalNodes + " nodes");
            this.dirty = false;
        } catch (IOException var12) {
            System.out.println("Failed to save node networks: " + var12.getMessage());
        }

    }
    public void loadNetworks() {
        Path saveFile = this.getSaveFilePath();
        if (!Files.exists(saveFile, new LinkOption[0])) {
            System.out.println("No saved node networks found");
        } else {
            try {
                try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(saveFile)))) {
                    int version = in.readInt();
                    if (version > 1) {
                        System.out.println("Save file version " + version + " is newer than supported");
                    }

                    int nodeCount = in.readInt();
                    if (nodeCount < 0 || nodeCount > 1000000) {
                        System.out.println("Invalid node count: " + nodeCount);
                        return;
                    }

                    List<LoadedCableData> loadedNodes = new ArrayList<>(nodeCount);
                    HashMap<BlockPos,ArrayList<OutputData>> IO = new HashMap<>();
                    for(int i = 0; i < nodeCount; ++i) {
                        try {
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
                            loadedNodes.add(new LoadedCableData(ty, position,IO));

                        } catch (IllegalArgumentException var23) {
                            System.out.println("Skipping invalid node " + i);
                        }
                    }

                    if (!loadedNodes.isEmpty()) {
                        this.rebuildNetworksFromLoadedData(loadedNodes);
                        System.out.println("Loaded " + loadedNodes.size() + " nodes");
                    }
                }

                return;
            } catch (IOException var15) {
                System.out.println("Failed to load node networks: " + var15.getMessage());
            }
        }

    }

    private void rebuildNetworksFromLoadedData(List<LoadedCableData> loadedPipes) {
        for(LoadedCableData data : loadedPipes) {
            NodeHolder node = null;
            if(data.type == NodeHolder.NodeType.CABLENODE){
                node = new NodeHolder( new CableNode(uuid, data.position));
            }
            else if(data.type == NodeHolder.NodeType.CONTROLLERNODE){
                node = new NodeHolder((new ControllerNode(uuid,data.position)));
                node.controllerNode.InputsAndOutputs = data.controllerData;
            }

            AddNodeToChunkToLoad(node);
            this.nodes.put(data.position, node);
        }

        for(LoadedCableData data : loadedPipes) {
            NodeHolder node = this.getNodeAt( data.position);
            if (node != null) {
                Set<CableNetwork> adjacentNetworks = new HashSet<>();

                for(Direction dir : Direction.values()) {
                    NodeHolder neighbor = this.getNodeAt( data.position.offset(dir));
                    if (neighbor != null) {
                        node.addConnection(dir);
                        neighbor.addConnection(dir.getOpposite());
                        if (neighbor.getNetwork() != null) {
                            adjacentNetworks.add(neighbor.getNetwork());
                        }
                    }
                }

                if (node.getNetwork() == null) {
                    this.mergeOrInitNetwork( node, adjacentNetworks);
                }
            }

        }

        PrintStream var10000 = System.out;
        var10000.println("Rebuilt " + networks.size() + " networks");

    }

    public void AddNodeToChunkToLoad(NodeHolder node)
    {
        ArrayList<NodeHolder> chunk = null;
        var chunkId= ChunkBlockHelper.getChunkIdAt(node.getPosition());
        chunk = chunksToLoad.get(chunkId);
        if(chunk!= null)chunk.add(node);
        else
        {
            chunk = new ArrayList<>();
            chunk.add(node);
            chunksToLoad.put(chunkId,chunk);
        }


    }






    private static class LoadedCableData {
        final BlockPos position;
        final NodeHolder.NodeType type;
        final Map<BlockPos,ArrayList<OutputData>> controllerData;

        LoadedCableData(NodeHolder.NodeType type, BlockPos position, Map<BlockPos,ArrayList<OutputData>> controllerData/*, CableType cableType, CableNode.CableMode mode, int priority, CableNode.DistributionStrategy strategy*/) {
            this.type = type;
            this.position = position;
            this.controllerData = controllerData;

        }
    }

    private static record CableRetryEntry(BlockPos nodePos, BlockPos inventoryPos, Direction direction, int retryCount) {
        CableRetryEntry withIncrementedRetry() {
            return new CableRetryEntry( this.nodePos, this.inventoryPos, this.direction, this.retryCount + 1);
        }
    }

}

