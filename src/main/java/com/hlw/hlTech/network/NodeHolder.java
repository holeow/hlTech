package com.hlw.hlTech.network;

import com.hlw.hlTech.util.BlockPos;
import com.hlw.hlTech.util.Direction;

import java.util.Set;
import java.util.UUID;

public class NodeHolder {
    public CableNode cableNode;
    public ControllerNode controllerNode;

    public NodeType type = NodeType.NONE;

    public void SetNode(CableNode cableNode){
        this.cableNode = cableNode;
        this.controllerNode = null;
        this.type = NodeType.CABLENODE;
    }
    public void SetNode(ControllerNode controllerNode){
        this.cableNode = null;
        this.controllerNode = controllerNode;
        this.type = NodeType.CONTROLLERNODE;
    }

    public NodeHolder(CableNode cableNode){
        this.cableNode = cableNode;
        this.controllerNode = null;
        this.type = NodeType.CABLENODE;
    }
    public NodeHolder(ControllerNode controllerNode){
        this.cableNode = null;
        this.controllerNode = controllerNode;
        this.type = NodeType.CONTROLLERNODE;
    }

    public UUID getWorldId() {
        if(type == NodeType.CABLENODE )return this.cableNode.getWorldId();
        else if (type == NodeType.CONTROLLERNODE) return this.controllerNode.getWorldId();
        else throw new UnsupportedOperationException("NodeType isn't cablenode nor controllernode !");
    }


    public void addConnection(Direction direction) {
        if(type == NodeType.CABLENODE ) this.cableNode.addConnection(direction);
        else if (type == NodeType.CONTROLLERNODE)  this.controllerNode.addConnection(direction);
        else throw new UnsupportedOperationException("NodeType isn't cablenode nor controllernode !");
    }

    public void removeConnection(Direction direction) {


        if(type == NodeType.CABLENODE ) this.cableNode.removeConnection(direction);
        else if (type == NodeType.CONTROLLERNODE)  this.controllerNode.removeConnection(direction);
        else throw new UnsupportedOperationException("NodeType isn't cablenode nor controllernode !");
    }

    public Set<Direction> getConnections() {

        if(type == NodeType.CABLENODE )return this.cableNode.getConnections();
        else if (type == NodeType.CONTROLLERNODE) return this.controllerNode.getConnections();
        else throw new UnsupportedOperationException("NodeType isn't cablenode nor controllernode !");

    }


    public CableNetwork getNetwork() {

        if(type == NodeType.CABLENODE )return this.cableNode.getNetwork();
        else if (type == NodeType.CONTROLLERNODE) return this.controllerNode.getNetwork();
        else throw new UnsupportedOperationException("NodeType isn't cablenode nor controllernode !");

    }

    public BlockPos getPosition() {

        if(type == NodeType.CABLENODE )return this.cableNode.getPosition();
        else if (type == NodeType.CONTROLLERNODE) return this.controllerNode.getPosition();
        else throw new UnsupportedOperationException("NodeType isn't cablenode nor controllernode !");
    }

    public void setNetwork(CableNetwork network) {

        if(type == NodeType.CABLENODE ) this.cableNode.setNetwork(network);
        else if (type == NodeType.CONTROLLERNODE)  this.controllerNode.setNetwork(network);
        else throw new UnsupportedOperationException("NodeType isn't cablenode nor controllernode !");
    }

    public void tick() {
        if(type == NodeType.CABLENODE ) this.cableNode.tick();
        else if (type == NodeType.CONTROLLERNODE)  this.controllerNode.tick();
        else throw new UnsupportedOperationException("NodeType isn't cablenode nor controllernode !");
    }

    public void onTransfer() {
        if(type == NodeType.CABLENODE ) this.cableNode.onTransfer();
        else if (type == NodeType.CONTROLLERNODE)  this.controllerNode.onTransfer();
        else throw new UnsupportedOperationException("NodeType isn't cablenode nor controllernode !");
    }




    public enum NodeType {
        NONE,CABLENODE,CONTROLLERNODE,INVENTORYNODE
    }
}
