package com.jht.chimera.ui.mainapp;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.graph.GraphLegendWidget;
import com.jht.chimera.lib.widget.athena.NewWorkoutGraph;

public class GraphMaker {
    private static final int COLOR_GRAPH_LIGHT_YELLOW = 0xfff2a61a;
    private static final int COLOR_GRAPH_DARK_YELLOW = 0xffa5751c;
    private static final int COLOR_GRAPH_BLUE = 0xff00a0d0;
    private static final int COLOR_GRAPH_DARK_BLUE = 0xff0060a0;
    private static final int COLOR_GRAPH_RED = 0xffff0000;
    private static final int COLOR_GRAPH_GREEN = 0xff00cc00;
    private static final int COLOR_GRAPH_GRAY_ALPHA_20 = 0x30909090;
    private static final int COLOR_GRAPH_GRAY_ALPHA_40 = 0x60909090;

    public NewWorkoutGraph.LineGroup resistanceLine;
    public NewWorkoutGraph.LineGroup inclineLine;
    public NewWorkoutGraph.LineGroup speedLine;
    public NewWorkoutGraph.LineGroup rpmLine;
    public NewWorkoutGraph.LineGroup spmLine;
    public NewWorkoutGraph.LineGroup wattsLine;
    public NewWorkoutGraph.LineGroup bpmLine;


    private void GraphMakerInit (NewWorkoutGraph graphWidget, GraphLegendWidget glw) {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        glw.removeAllViews();

        glw.addItem(ContextUtil.getString(R.string.heart_rate_uc), COLOR_GRAPH_RED);

        switch (frameConfig.machineClassType) {
            case Treadmill:
                glw.addItem(ContextUtil.getString(R.string.speed_uc), COLOR_GRAPH_BLUE);
                glw.addItem(ContextUtil.getString(R.string.incline_uc), COLOR_GRAPH_LIGHT_YELLOW);

                inclineLine = graphWidget.addLineType(frameConfig.incline.getMin(), frameConfig.incline.getMax(), COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW);
                inclineLine.addFutureColor(COLOR_GRAPH_GRAY_ALPHA_40, COLOR_GRAPH_GRAY_ALPHA_40);

                speedLine = graphWidget.addLineType(frameConfig.speedKph.getMin(), frameConfig.speedKph.getMax(), COLOR_GRAPH_BLUE);
                speedLine.addFutureColor(COLOR_GRAPH_DARK_BLUE);
                break;

            case Ascent:
                glw.addItem(ContextUtil.getString(R.string.rpm_uc), COLOR_GRAPH_GREEN);
                glw.addItem(ContextUtil.getString(R.string.resistance_uc), COLOR_GRAPH_LIGHT_YELLOW);
                glw.addItem(ContextUtil.getString(R.string.incline_uc), COLOR_GRAPH_BLUE);

                resistanceLine = graphWidget.addLineType(frameConfig.resistance.getMin(), frameConfig.resistance.getMax(), COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW);
                resistanceLine.addFutureColor(COLOR_GRAPH_GRAY_ALPHA_40, COLOR_GRAPH_GRAY_ALPHA_40);

                inclineLine = graphWidget.addLineType(frameConfig.incline.getMin(), frameConfig.incline.getMax(), COLOR_GRAPH_BLUE);
                inclineLine.addFutureColor(COLOR_GRAPH_DARK_BLUE);

                break;

            case SpinBike:
            case Elliptical:
            case RecumbentElliptical:
            case Bike:
                glw.addItem(ContextUtil.getString(R.string.watts_uc), COLOR_GRAPH_DARK_BLUE);
                glw.addItem(ContextUtil.getString(R.string.rpm_uc), COLOR_GRAPH_GREEN);
                glw.addItem(ContextUtil.getString(R.string.resistance_uc), COLOR_GRAPH_LIGHT_YELLOW);

                resistanceLine = graphWidget.addLineType(0, 25, COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW);
                resistanceLine.addFutureColor(COLOR_GRAPH_GRAY_ALPHA_40, COLOR_GRAPH_GRAY_ALPHA_40);

                rpmLine = graphWidget.addLineType(0, 150, COLOR_GRAPH_GREEN);

                wattsLine = graphWidget.addLineType(0, 525,  COLOR_GRAPH_BLUE);
                break;

            case Climbmill:
            case Stepper:
                glw.addItem(ContextUtil.getString(R.string.spm_uc), COLOR_GRAPH_BLUE);
                glw.addItem(ContextUtil.getString(R.string.resistance_uc), COLOR_GRAPH_LIGHT_YELLOW);

                resistanceLine = graphWidget.addLineType(frameConfig.resistance.getMin(), frameConfig.resistance.getMax(), COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW);
                resistanceLine.addFutureColor(COLOR_GRAPH_GRAY_ALPHA_40, COLOR_GRAPH_GRAY_ALPHA_40);

                spmLine = graphWidget.addLineType(0, 150, COLOR_GRAPH_BLUE);
                spmLine.addFutureColor(COLOR_GRAPH_DARK_BLUE);

                break;
        }
        // set last so it shows over the top of the other lines
        bpmLine = graphWidget.addLineType(0, 220, COLOR_GRAPH_RED);
    }

    public GraphMaker(NewWorkoutGraph graphWidget, GraphLegendWidget glw) {
            GraphMakerInit(graphWidget, glw);
    }

    public GraphMaker(NewWorkoutGraph graphWidget, GraphLegendWidget glw, boolean isConstantWatts) {

        if (isConstantWatts)
        {
            glw.addItem(ContextUtil.getString(R.string.heart_rate_uc), COLOR_GRAPH_RED);

            glw.addItem(ContextUtil.getString(R.string.watts_uc), COLOR_GRAPH_LIGHT_YELLOW);
            wattsLine = graphWidget.addLineType(0, 525,  COLOR_GRAPH_LIGHT_YELLOW);
            wattsLine.addFutureColor(COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW);

            bpmLine = graphWidget.addLineType(0, 220, COLOR_GRAPH_RED);
        }
        else
        {
            GraphMakerInit(graphWidget, glw);
        }
    }
}
