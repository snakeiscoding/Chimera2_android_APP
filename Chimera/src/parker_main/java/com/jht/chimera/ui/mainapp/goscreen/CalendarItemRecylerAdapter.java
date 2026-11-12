package com.jht.chimera.ui.mainapp.goscreen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;

import java.util.Calendar;
import java.util.HashSet;

/** RecyclerAdapter for the boxes on the workout calendar */
public class CalendarItemRecylerAdapter
    extends RecyclerView.Adapter<CalendarItemRecylerAdapter.CalendarItemHolder>
{
    protected static final String TAG = "CalendarItemRecylerA...";

    public interface DateSelectionListener {
        void dateSelected(@Nullable Calendar date);
    }

    CalendarItemHolder currentlySelectedItemHolder = null;
    
    class CalendarItemHolder extends RecyclerView.ViewHolder {
        boolean isInCurrentMonth;
        boolean hasWorkouts;
        public Calendar date;

        CalendarItemHolder(View view) {
            super(view);
            view.setClickable(true);
            view.setFocusable(true);
            view.setOnClickListener(view1 -> {
                select(this);
            });
        }

        void updateUi() {
            boolean isSelected = this == currentlySelectedItemHolder;
            this.itemView.setBackgroundColor(
                    isSelected ? 0xFFFCAF1A :
                    !isInCurrentMonth ? 0xFF000000 :
                    hasWorkouts ? 0xFFD1D1D1 : 0xFF646464 );
            TextView dateLabel = ((TextView)this.itemView.findViewById(R.id.date_label));
            dateLabel.setText(""+date.get(Calendar.DAY_OF_MONTH));
            dateLabel.setTextColor(
                    isSelected ? 0xFFFFFFFF :
                    !isInCurrentMonth ? 0x00 :
                    hasWorkouts ? 0xFF646464 : 0xFFFFFFFF);
        }

        void setData(boolean isInMonth, Calendar date, boolean hasWorkouts) {
            this.isInCurrentMonth = isInMonth;
            this.date = date;
            this.hasWorkouts = hasWorkouts;
            this.updateUi();
        }
    }

    protected CalendarWorkoutDataSource workoutDataSource;

    public CalendarItemRecylerAdapter(CalendarWorkoutDataSource workoutDataSource) {
        assert(workoutDataSource != null);
        if(workoutDataSource == null) throw new NullPointerException("workoutDataSource is null");
        this.workoutDataSource = workoutDataSource;
    }

    protected void select(CalendarItemHolder newSelection) {
        CalendarItemHolder oldSelection = currentlySelectedItemHolder;
        currentlySelectedItemHolder = newSelection;
        if (oldSelection != null) oldSelection.updateUi();
        if (newSelection != null) newSelection.updateUi();
        daySelected(newSelection == null ? null : newSelection.date);
    }

    protected HashSet<DateSelectionListener> dateSelectionListeners = new HashSet<>(1);

    public void addDateSelectionListener(DateSelectionListener l) {
        dateSelectionListeners.add(l);
    }

    void daySelected(@Nullable Calendar date) {
        for (DateSelectionListener l : dateSelectionListeners) l.dateSelected(date);
    }

    @NonNull
    @Override
    public CalendarItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.calendar_item, viewGroup, false);
        return new CalendarItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarItemHolder holder, int position) {
        boolean inMonth = position >= firstDayOfMonthIndex && position < firstDayOfNextMonthIndex;
        Calendar day = (Calendar) firstDayOfMonth.clone();
        day.set(Calendar.DAY_OF_MONTH, position - firstDayOfMonthIndex + firstDayOfMonth.get(Calendar.DAY_OF_MONTH));
        holder.setData(inMonth, day, !workoutDataSource.getWorkoutsInDay(day).isEmpty());
    }

    @Override
    public int getItemCount() {
        return 7 * (int)Math.ceil(firstDayOfNextMonthIndex / 7.0);
    }

    Calendar firstDayOfMonth;
    int firstDayOfMonthIndex = 0;
    int firstDayOfNextMonthIndex = 0;

    public void goToMonth(Calendar cal) {
        CalendarFragment.assertTimeZero(cal, getClass()+"#goToMonth");
        Calendar firstDay = ((Calendar)cal.clone());
        firstDay.set(Calendar.DAY_OF_MONTH, 1);
        this.firstDayOfMonth = firstDay;
        int firstDayOfMonthDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK);
        firstDayOfMonthIndex = firstDayOfMonthDayOfWeek - Calendar.SUNDAY;
        firstDayOfNextMonthIndex = firstDayOfMonthIndex + cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        notifyDataSetChanged();
        select(null);
    }
}
