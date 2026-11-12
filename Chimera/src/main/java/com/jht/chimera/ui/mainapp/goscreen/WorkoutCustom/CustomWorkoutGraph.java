package com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.jht.chimera.R;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by AlexN on 1/17/2019.
 */
public class CustomWorkoutGraph extends View {

    private static final float GRAPH_PERCENT = 0.585f;

    Paint backgroundPaint;
    Paint selectedPaint;
    Paint bannerPaint;
    Paint unselectedPaint;
    Paint textPaint;
    Paint disabledPaint;
    List<Paint> valuePaints;

    int width;
    int height;
    int heightNoPadding;
    int paddingX;
    int paddingY;

    int defaultColumns = 12;
    int maxColumns = 20;
    int numColumns = 0;
    int indexOffset = 0;
    int selectedColumn;

    float columnWidth;
    float columnSpacing;
    float indexWidth;
    float indexSpacing;

    Drawable plusButton;
    List<Float> selectedParameterValues;
    List<WorkoutParameter> parameters;
    Map<String,List<Float>> customWorkoutData;

    private ICustomWorkoutGraph mGraphCallback;

    public interface ICustomWorkoutGraph {
        void intervalSelected(List<Float> parameterValues);
        void canDeleteSegment(boolean canDelete);
    }

    public CustomWorkoutGraph(Context context) {
        super(context);
        init(context);
    }

    public CustomWorkoutGraph(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        plusButton = ContextCompat.getDrawable(context, R.drawable.ic_plus_button_green);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(0xFF4C4C4C);

        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setStyle(Paint.Style.FILL);
        selectedPaint.setColor(Color.BLACK);

        bannerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bannerPaint.setStyle(Paint.Style.FILL);
        bannerPaint.setColor(-675036);

        disabledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        disabledPaint.setStyle(Paint.Style.FILL);
        disabledPaint.setColor(0xFF131313);

        unselectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        unselectedPaint.setStyle(Paint.Style.FILL);
        unselectedPaint.setColor(0x70000000);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        valuePaints = new ArrayList<>();
        selectedParameterValues = new ArrayList<>();
    }

    /** Initialize the graph to reference the specified parameters and custom workout data. */
    public void loadWorkout(List<WorkoutParameter> parameters, Map<String,List<Float>> customWorkoutData) {
        if (customWorkoutData == null) return;

        this.parameters = parameters;

        numColumns = 0;
        indexOffset = 0;
        for (int i = 0; i < parameters.size(); i++) {
            WorkoutParameter param = parameters.get(i);

            int valueCount;
            if (!customWorkoutData.containsKey(param.identifier())) {
                customWorkoutData.put(param.identifier(), new ArrayList<>());
            }
            valueCount = customWorkoutData.get(param.identifier()).size();
            numColumns = Math.max(numColumns, valueCount);

            if (i >= valuePaints.size()) {
                valuePaints.add(new Paint(Paint.ANTI_ALIAS_FLAG));
            }
            if (parameters.get(i).graphStyle != null) {
                valuePaints.get(i).setStyle(param.graphStyle);
                valuePaints.get(i).setColor(param.graphColor);
            }
        }
        this.customWorkoutData = customWorkoutData;
        padIntervalData(1);
        calculateColumnWidth(Math.max(numColumns, defaultColumns));
        selectColumn(0);

        if (mGraphCallback != null) {
            mGraphCallback.canDeleteSegment(numColumns > 1);
        }

        invalidate();
    }

    public void setCustomWorkoutGraphCallback(ICustomWorkoutGraph iCustomWorkoutGraph) {
        this.mGraphCallback = iCustomWorkoutGraph;
    }

    public float computeTotalTime() {
        float time = 0;
        if (customWorkoutData != null) {
            List<Float> intervalData = customWorkoutData.get("TIME_GOAL_START"); //NON-NLS
            if (intervalData != null) {
                for (float f : intervalData) time += f;
            }
        }
        return time;
    }

    public void updateParameter(float value, String identifier) {
        //Only update parameters if workout loaded
        if (customWorkoutData != null) {
            List<Float> intervalData = customWorkoutData.get(identifier);
            if (intervalData != null) {
                intervalData.set(selectedColumn, value);
                invalidate();
                return;
            }

            String pn = "";
            for (String parameterId : customWorkoutData.keySet()) {
                if (!pn.isEmpty()) pn += ", ";
                pn += parameterId;
            }
        }
    }

    public void nextIndex() {
        if (selectedColumn < numColumns - 1) {
            selectedColumn++;
            selectColumn(selectedColumn);
            if (selectedColumn >= defaultColumns + indexOffset) {
                indexOffset++;
            }
            if (selectedColumn == numColumns - 1 && selectedColumn >= defaultColumns + indexOffset - 1 && selectedColumn < maxColumns - 1) {
                indexOffset++;
            }
        }
    }

    public void previousIndex() {
        if (selectedColumn > 0) {
            selectedColumn--;
            selectColumn(selectedColumn);
            if (selectedColumn < indexOffset) {
                indexOffset--;
            }
        }
    }

    public void deleteSegment() {
        if (numColumns > 1) {
            numColumns--;
            for (List<Float> intervalData : customWorkoutData.values()) {
                intervalData.remove(selectedColumn);
            }
            if (numColumns + 1 < defaultColumns + indexOffset) {
                indexOffset = numColumns + 1 - defaultColumns;
                indexOffset = Math.max(0, indexOffset);
            }
            selectedColumn = Math.min(numColumns - 1, selectedColumn);
            selectColumn(selectedColumn);
            calculateColumnWidth(Math.max(numColumns, defaultColumns));
            invalidate();

            if (numColumns == 1 && mGraphCallback != null) {
                mGraphCallback.canDeleteSegment(false);
            }
        }
    }

    public void resetSegment() {
        for (WorkoutParameter p : parameters) {
            customWorkoutData.get(p.identifier()).set(selectedColumn, p.defaultValue());
        }
        selectColumn(selectedColumn);
        invalidate();
    }

    public int getSelectedColumn() {
        return selectedColumn+1;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float paddingPercent = 0.025f;
        width = (int) (w * (1 - paddingPercent * 2));
        height = (int) (h * (1 - paddingPercent * 2));
        heightNoPadding = h;
        paddingX = (int) (w * paddingPercent);
        paddingY = (int) (h * paddingPercent);
        calculateColumnWidth(Math.max(numColumns, defaultColumns));

        indexWidth = (width / (defaultColumns * 1.125f));
        indexSpacing = indexWidth * 0.125f;

        textPaint.setTextSize(indexWidth * 0.5f);

        for (Paint paint : valuePaints) {
            paint.setStrokeWidth(indexWidth / 15);
        }
    }

    private void calculateColumnWidth(int numColumns) {
        columnWidth = (width / (numColumns * 1.125f));
        columnSpacing = columnWidth * 0.125f;
    }

    int columnX;
    int indexX;
    float valueHeight;
    float previousValueHeight;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Iterate through number of columns, either the default or more if new columns added
        for (int i = 0; i < Math.max(numColumns, defaultColumns); i++) {
            columnX = Math.round(i * (columnWidth + columnSpacing) + columnSpacing / 2f) + paddingX;
            indexX = Math.round(i * (indexWidth + indexSpacing) + indexSpacing / 2f) + paddingX;
            //Graph Columns
            drawColumn(canvas, i);
            //Graph Indices
            drawIndex(canvas, i);
        }
        drawValues(canvas);

    }

    private void drawColumn(Canvas canvas, int column) {
        if (column < numColumns) {
            canvas.drawRect(columnX, paddingY, columnX + columnWidth, height * GRAPH_PERCENT + paddingY, backgroundPaint);
        } else {
            //Disabled state
            canvas.drawRect(columnX, paddingY, columnX + columnWidth, height * GRAPH_PERCENT + paddingY, disabledPaint);
        }
    }

    private void drawIndex(Canvas canvas, int index) {
        //Account for UI index that may be offset from the literal row number
        if (index < defaultColumns) {
            index += indexOffset;
            int indexHeight = (int) (heightNoPadding - indexSpacing);
            if (index < numColumns) {
                //Bottom indices
                if (index == selectedColumn) {
                    //Selected index
                    canvas.drawRect(indexX, indexHeight - indexWidth, indexX + indexWidth, indexHeight - indexWidth + indexSpacing, bannerPaint);
                    canvas.drawRect(indexX, indexHeight - indexWidth + indexSpacing, indexX + indexWidth, heightNoPadding, selectedPaint);
                }
                else {
                    //Existing Unselected index
                    canvas.drawRect(indexX, indexHeight - indexWidth, indexX + indexWidth, indexHeight, backgroundPaint);
                }
                canvas.drawText((index + 1) + "", indexX + indexWidth * 0.5f, indexHeight - indexWidth * 0.3f, textPaint);
            } else {
                //Draw plus button
                if (index == numColumns) {
                    plusButton.setBounds(indexX, (int) (indexHeight - indexWidth), (int) (indexX + indexWidth), indexHeight);
                    plusButton.draw(canvas);
                } else {
                    //Disabled state
                    canvas.drawRect(indexX, indexHeight - indexWidth, indexX + indexWidth, indexHeight, disabledPaint);
                }
            }
        }
    }

    private void drawValues(Canvas canvas) {
        if (customWorkoutData != null) {
            for (int j = 0; j < parameters.size(); j++) {
                WorkoutParameter parameter = parameters.get(j);
                List<Float> data = customWorkoutData.get(parameter.identifier());
                previousValueHeight = -1;
                for (int index = 0; index < data.size(); index++) {
                    if (parameters.get(j).graphStyle != null) {
                        //Calculate new column value width and height
                        columnX = Math.round(index * (columnWidth + columnSpacing) + columnSpacing / 2f) + paddingX;
                        valueHeight = (float) ((height * GRAPH_PERCENT * (1 - ((data.get(index) - parameter.min())
                                / (parameter.max() - parameter.min())))) + paddingY);
                        //Check parameter drawing style
                        switch (parameter.graphStyle) {
                            case FILL:
                                //Draw rectangle fill
                                canvas.drawRect(columnX, valueHeight, columnX + columnWidth, height * GRAPH_PERCENT + paddingY, valuePaints.get(j));
                                //Draw shade if not selected column
                                if (index != selectedColumn) {
                                    canvas.drawRect(columnX, paddingY, columnX + columnWidth, height * GRAPH_PERCENT + paddingY, unselectedPaint);
                                }
                                break;

                            case FILL_AND_STROKE:
                                //Draw line across column
                                if (index < numColumns - 1) {
                                    canvas.drawLine(columnX, valueHeight, columnX + columnWidth + columnSpacing, valueHeight, valuePaints.get(j));
                                }
                                //Draw vertical connection line
                                if (index > 0) {
                                    canvas.drawLine(columnX, previousValueHeight, columnX, valueHeight, valuePaints.get(j));
                                    //Draw circles if values changed
                                    if (valueHeight != previousValueHeight) {
                                        canvas.drawCircle(columnX, valueHeight, columnSpacing / 2, valuePaints.get(j));
                                        canvas.drawCircle(columnX, previousValueHeight, columnSpacing / 2, valuePaints.get(j));
                                    }
                                }
                                //Draw circle for first column
                                if (index == 0) {
                                    canvas.drawCircle(columnX, valueHeight, columnSpacing / 2, valuePaints.get(j));
                                }
                                //Drawing for last column
                                if (index == numColumns - 1) {
                                    canvas.drawLine(columnX, valueHeight, columnX + columnWidth, valueHeight, valuePaints.get(j));
                                    canvas.drawCircle(columnX + columnWidth, valueHeight, columnSpacing / 2, valuePaints.get(j));
                                }
                                break;
                        }
                        previousValueHeight = valueHeight;
                    }
                }
            }
        }
    }

    private void selectColumn(int column) {
        if (column < 0 || column >= this.numColumns) {
            throw new RuntimeException("Column index out of range: "+column+"; numColumns:"+numColumns);
        }

        selectedColumn = column;
        if (mGraphCallback != null) {
            selectedParameterValues.clear();
            for (WorkoutParameter p : parameters) {
                selectedParameterValues.add(customWorkoutData.get(p.identifier()).get(column));
            }
            mGraphCallback.intervalSelected(selectedParameterValues);
        }
    }

    private void calculateTouch(float x, float y) {
        int column;
        //Touch on column
        if (y < height - indexWidth) {
            column = (int) ((x - paddingX) * Math.max(numColumns, defaultColumns) / (width));
            if (column < numColumns) {
                selectColumn(column);

                //If column is offscreen left, navigate to it
                if (column <= indexOffset) {
                    indexOffset = column;
                }
                //If column is offscreen right, navigate to it
                else if (column > defaultColumns + indexOffset - 1) {
                    indexOffset = column - defaultColumns + 1;
                }
                //Add one to show the plus button if last available column
                if (column == numColumns - 1 && column < maxColumns - 1
                        && column >= defaultColumns - 1) {
                    indexOffset = numColumns - defaultColumns + 1;
                }
                invalidate();
            }
        }
        //Touch on index
        else {
            column = (int) (x - paddingX) * defaultColumns / (width) + indexOffset;
            if (column < numColumns) {
                selectColumn(column);
                invalidate();
            } else if (column == numColumns && numColumns < 20) {
                plusButtonClicked();
            }
        }
    }

    /** Increase the number of columns to at least columnCount,
     * and ensure that there is interval data for each parameter / column */
    protected void padIntervalData(int columnCount) {
        this.numColumns = Math.max(columnCount, this.numColumns);
        for (WorkoutParameter p : parameters) {
            List<Float> values = customWorkoutData.get(p.identifier());
            Float defaultValue = values.size() == 0 ? p.defaultValue() : values.get(values.size()-1);
            while (values.size() < numColumns) values.add(defaultValue);
        }
    }

    private void plusButtonClicked() {
        //Plus button clicked, so add new column
        padIntervalData(++numColumns);
        if (numColumns >= defaultColumns) {
            //Hide plus button after max number of columns
            if (numColumns < maxColumns) {
                indexOffset++;
            }
            calculateColumnWidth(numColumns);
        }
        selectColumn(numColumns - 1);

        if (mGraphCallback != null) mGraphCallback.canDeleteSegment(true);

        invalidate();
    }

    float x;
    float y;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        x = event.getX();
        y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                calculateTouch(x, y);
                break;
        }
        return true;
    }
}
