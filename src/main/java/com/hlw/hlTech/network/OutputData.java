package com.hlw.hlTech.network;

import com.hlw.hlTech.util.BlockPos;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class OutputData {

     public BlockPos outputPos;
     public int priority;
     public boolean allowAll;
     public ArrayList<String> itemIds = new ArrayList<>();

     public void writeToDataStream (DataOutputStream stream) throws IOException {
         outputPos.writeToDataStream(stream);
         stream.writeInt(priority);
         stream.writeBoolean(allowAll);
         stream.writeInt(itemIds.size());
         for(String id : itemIds){
             stream.writeUTF(id);
         }
     }

     public OutputData(){

     }
     public void replaceData (OutputData other){
         this.outputPos = other.outputPos;
         this.priority = other.priority;
         this.allowAll = other.allowAll;
         this.itemIds = other.itemIds;
     }

     public OutputData(BlockPos outputPos,int priority,boolean allowAll, ArrayList<String> itemIds){
         this.outputPos = outputPos;
         this.priority = priority;
         this.allowAll = allowAll;
         this.itemIds = itemIds;
     }
     public static OutputData readFromDataStream(DataInputStream stream) throws IOException {
            OutputData data = new OutputData();
            data.outputPos = BlockPos.readFromDataStream(stream);
            data.priority = stream.readInt();
            data.allowAll = stream.readBoolean();
            int size = stream.readInt();
            for(int i =0;i<size;i++){
                data.itemIds.add(stream.readUTF());
            }
            return data;
     }
}
