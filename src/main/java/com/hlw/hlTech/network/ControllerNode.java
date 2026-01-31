package com.hlw.hlTech.network;

import com.hlw.hlTech.util.BlockPos;
import com.hlw.hlTech.util.Direction;

import java.util.*;
import java.util.stream.Stream;

public class ControllerNode {
    private final UUID worldId;
    private final BlockPos position;
    private final Set<Direction> connections;
    private CableNetwork network;
    public Map<BlockPos, ArrayList<OutputData>> InputsAndOutputs = new HashMap<>();
    int ticksSinceLastTransfer = 0;

    public ControllerNode(UUID worldId, BlockPos position) {
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
        if(!this.connections.contains((direction))) this.connections.add(direction);
    }

    public void removeConnection(Direction direction) {
        this.connections.remove(direction);
        //var removedpos = getPosition().offset(direction);

    }
    public void removeInventory(BlockPos pos){
        this.InputsAndOutputs.remove(pos);
        List<BlockPos> entriesToRemove = new ArrayList<>() ;
        for(var list : InputsAndOutputs.entrySet()){
            var filter = list.getValue().stream().filter(a-> a.outputPos.equals(pos));
            for(OutputData data : filter.toList()){
                list.getValue().remove(data);
            }
                if(list.getValue().isEmpty()){
                    entriesToRemove.add(list.getKey());
                }

        }
        for(var entry : entriesToRemove){
            InputsAndOutputs.remove(entry);
        }
    }

    public void AddOrModifyInputOutput(BlockPos input, OutputData output){
        var outputlist = InputsAndOutputs.get(input);
        if(outputlist == null){
            outputlist = new ArrayList<>();
            outputlist.add(output);
            InputsAndOutputs.put(input, outputlist);
        }
        else{
            var filter = outputlist.stream().filter(a-> a.outputPos.equals(output.outputPos)).toList();
            if(filter.size() == 0){
                outputlist.add(output);

            }
            else{
                for(var o :filter){
                    o.replaceData(output);
                }
            }
        }
    }
    public void RemoveInputOutput(BlockPos input, BlockPos output){
        var outputlist = InputsAndOutputs.get(input);
        if(outputlist == null) return;
        var filter = outputlist.stream().filter(a-> a.outputPos.equals(output)).toList();

        if(filter.size()>0){
            if(outputlist.size()<=1){
                InputsAndOutputs.remove(input);
            }
            else{
                for(var o : filter){
                    outputlist.remove(o);
                }

            }
        }

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
        ++this.ticksSinceLastTransfer;
    }

    public void onTransfer() {
       this.ticksSinceLastTransfer = 0;
    }

    public BlockPos[] getConnectedNeighbors() {
        Stream<Direction> var10000 = this.connections.stream();
        BlockPos var10001 = this.position;
        Objects.requireNonNull(var10001);
        return (BlockPos[])var10000.map(var10001::offset).toArray((a) -> new BlockPos[a]);
    }

    public String toString() {
        String var10000 = String.valueOf(this.position);
        return "ControllerNode{pos=" + var10000 + ", connections=" + this.connections.size() + "}";
    }
}
