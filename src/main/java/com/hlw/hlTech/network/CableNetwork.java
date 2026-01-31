/*
 * PipeNetwork.class - Streamlined pipe logistics system for automated item transfer between containers. Hytale plugin.
 * Copyright (C) 2026 WanMine
 * * --- MODIFICATION NOTICE ---
 * This file has been modified by Antoine Jollet (holeow) on 27/01/2026.
 * The modifications are licensed under the LGPL v3.
 * * Summary of changes:
 * - Renamed some variables fields, and functions to fit the new system
 * - Added the controller system in the code.
 * - Used the NodeHolder to hold both cables (pipes) and controllers.
 * - Added linking to the cached inventories
 * - Removed unused functions due to the controller system like the ones for cable input mode or type
 * ----------------------------
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation...
 */

package com.hlw.hlTech.network;

import com.hlw.hlTech.util.BlockPos;
import com.hlw.hlTech.util.Direction;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CableNetwork {
    private final Map<BlockPos, NodeHolder> nodes = new HashMap<>();
    private final Set<BlockPos> inputNodes = new HashSet();
    private final Set<BlockPos> outputNodes = new HashSet();
    private final Map<BlockPos, Integer> inputRoundRobinIndex = new HashMap();
    private long tickCount = 0L;
    public final Map<BlockPos, ItemContainer> connectedInventories = new ConcurrentHashMap<>();



    public void addNode(NodeHolder node) {
        this.nodes.put(node.getPosition(), node);
        node.setNetwork(this);
    }

    public NodeHolder getNode(BlockPos position) {
        return (NodeHolder) this.nodes.get(position);
    }
    public Collection<NodeHolder> getNodes() {
        return this.nodes.values();
    }
    public int size() {
        return this.nodes.size();
    }
    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    public void removeInventory(BlockPos pos){
        connectedInventories.remove(pos);
        for(var node : nodes.entrySet()){
            if(node.getValue().controllerNode!=null){
                node.getValue().controllerNode.removeInventory(pos);
            }
        }
    }
    public long getTickCount() {
        return this.tickCount;
    }



    public List<BlockPos> findPathToSpecificOutput(BlockPos start, BlockPos targetOutput) {
        if (this.nodes.containsKey(start) && this.outputNodes.contains(targetOutput)) {
            Queue<BlockPos> queue = new LinkedList();
            Map<BlockPos, BlockPos> cameFrom = new HashMap();
            Set<BlockPos> visited = new HashSet();
            queue.add(start);
            visited.add(start);
            cameFrom.put(start, (BlockPos) null);

            while(!queue.isEmpty()) {
                BlockPos current = (BlockPos)queue.poll();
                if (current.equals(targetOutput)) {
                    return this.reconstructPath(cameFrom, current);
                }

                NodeHolder currentNode = (NodeHolder) this.nodes.get(current);
                if (currentNode != null) {
                    for(Direction dir : currentNode.getConnections()) {
                        BlockPos neighbor = current.offset(dir);
                        if (!visited.contains(neighbor) && this.nodes.containsKey(neighbor)) {
                            visited.add(neighbor);
                            cameFrom.put(neighbor, current);
                            queue.add(neighbor);
                        }
                    }
                }
            }

            return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }
    }

    private List<BlockPos> reconstructPath(Map<BlockPos, BlockPos> cameFrom, BlockPos end) {
        List<BlockPos> path = new ArrayList();

        for(BlockPos current = end; current != null; current = (BlockPos)cameFrom.get(current)) {
            path.add(0, current);
        }

        return path;
    }
/*
    public List<BlockPos> getOrderedOutputCandidates(BlockPos start, CableNode.DistributionStrategy strategy) {
        if (this.nodes.containsKey(start) && !this.outputNodes.isEmpty()) {
            List<BlockPos> candidates = new ArrayList(this.outputNodes);
            candidates.remove(start);
            if (candidates.isEmpty()) {
                return Collections.emptyList();
            } else {
                candidates.sort((a, b) -> {
                    CableNode nodeA = (CableNode)this.nodes.get(a);
                    CableNode nodeB = (CableNode)this.nodes.get(b);
                    int pA = nodeA != null ? nodeA.getPriority() : 0;
                    int pB = nodeB != null ? nodeB.getPriority() : 0;
                    int cmp = Integer.compare(pB, pA);
                    if (cmp != 0) {
                        return cmp;
                    } else {
                        switch (strategy) {
                            case NEAREST:
                                return Double.compare(this.getDistanceSq(start, a), this.getDistanceSq(start, b));
                            case FARTHEST:
                                return Double.compare(this.getDistanceSq(start, b), this.getDistanceSq(start, a));
                            case ROUND_ROBIN:
                            default:
                                int cX = Integer.compare(a.getX(), b.getX());
                                if (cX != 0) {
                                    return cX;
                                } else {
                                    int cY = Integer.compare(a.getY(), b.getY());
                                    return cY != 0 ? cY : Integer.compare(a.getZ(), b.getZ());
                                }
                        }
                    }
                });
                return strategy == CableNode.DistributionStrategy.ROUND_ROBIN ? this.applyPriorityGroupedRoundRobin(start, candidates) : candidates;
            }
        } else {
            return Collections.emptyList();
        }
    }
*/
    /*
    private List<BlockPos> applyPriorityGroupedRoundRobin(BlockPos start, List<BlockPos> sortedCandidates) {
        List<BlockPos> finalOrder = new ArrayList();
        int currentIndex = (Integer)this.inputRoundRobinIndex.getOrDefault(start, 0);

        int j;
        for(int i = 0; i < sortedCandidates.size(); i = j) {
            int currentPriority = ((CableNode)this.nodes.get(sortedCandidates.get(i))).getPriority();

            for(j = i + 1; j < sortedCandidates.size() && ((CableNode)this.nodes.get(sortedCandidates.get(j))).getPriority() == currentPriority; ++j) {
            }

            List<BlockPos> group = new ArrayList(sortedCandidates.subList(i, j));
            int groupSize = group.size();
            Collections.rotate(group, -(currentIndex % groupSize));
            finalOrder.addAll(group);
        }

        return finalOrder;
    }
    */
    public void onTransferSuccess(BlockPos start) {
        //this.inputRoundRobinIndex.merge(start, 1, Integer::sum);
    }

    private double getDistanceSq(BlockPos p1, BlockPos p2) {
        double dbX = (double)(p1.getX() - p2.getX());
        double dbY = (double)(p1.getY() - p2.getY());
        double dbZ = (double)(p1.getZ() - p2.getZ());
        return dbX * dbX + dbY * dbY + dbZ * dbZ;
    }

    public void merge(CableNetwork other) {
        if (other != this) {
            for(NodeHolder node : other.nodes.values()) {
                this.addNode(node);
            }
            this.connectedInventories.putAll(other.connectedInventories);
            this.inputNodes.addAll(other.inputNodes);
            this.outputNodes.addAll(other.outputNodes);
            this.inputRoundRobinIndex.putAll(other.inputRoundRobinIndex);
        }

    }

    public void tick() {
        ++this.tickCount;

        for(NodeHolder node : new ArrayList<NodeHolder>(this.nodes.values())) {
            if(node.controllerNode!= null)
                node.tick();
        }

    }
}
