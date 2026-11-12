package com.jht.chimera.lib.graph;

/**
 * Created by alex.nguyen on 7/21/2016.
 */
public class GraphProfileData {
    public float [] frameNumbers = {0};
    public float [] values = {0};

    public GraphProfileData(float [] frameNumbers, float [] values){
        this.frameNumbers = frameNumbers;
        this.values = values;
    }

    public float [] getFrameNumbers(){
        return this.frameNumbers;
    }

    public float [] getValues(){
        return this.values;
    }
}
