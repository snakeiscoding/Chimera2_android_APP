package com.jht.chimera.lib.graph.workoutgraph;

import com.jht.chimera.lib.graph.graphview.PlotStyle;


public interface IWorkoutGraph
{
	public void reset();

    // Add a ChannelDataSource that will provide graph data to be plotted.
    public int addChannel(ChannelDataSource dataSource);

    // Set the drawing style for graph data that is in the past.
    public boolean setChannelHistoryPlotStyle(int channelIndex, PlotStyle plotStyle);
        
    // Set the drawing style for graph data that depeicts the future (i.e. from a ProfileData).
    public boolean setChannelFuturePlotStyle(int channelIndex, PlotStyle plotStyle);
    
    // Call this to update the graph data from History and a preset profile. Call this AFTER configuring
    // with setWorkoutModes() and addChannel().
//	public void setGraphData(boolean modifyCurrentInterval);

    // Call this to schedule a redraw. Call this AFTER configuring with the above functions.
    public void update();
}
