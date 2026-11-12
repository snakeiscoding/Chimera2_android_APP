package com.jht.chimera.lib.graph.graphview;

public class PlotStyle
{
	public enum FillType {
		Line,
		Filled,
		LinePlusFilled
	}
	
	public FillType fillType;
	public int lineColor;
	public int fillColor;

	PlotStyle(FillType fillType, int lineColor, int fillColor) {
		this.fillType = fillType;
		this.lineColor = lineColor;
		this.fillColor = fillColor;
	}	

	public static PlotStyle PlotStyleLine(int lineColor) {
		return new PlotStyle(FillType.Line, lineColor, 0);
	}

	public static PlotStyle PlotStyleFilled(int fillColor) {
		return new PlotStyle(FillType.Filled, 0, fillColor);
	}

	public static PlotStyle PlotStyleLinePlusFilled(int lineColor, int fillColor) {
		return new PlotStyle(FillType.LinePlusFilled, lineColor, fillColor);
	}	
}
