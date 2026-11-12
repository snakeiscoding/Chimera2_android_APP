package com.jht.chimera.ui.machinesettings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.LEDItem;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;

public class LEDColorRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    class CustomViewHolder extends RecyclerView.ViewHolder {
        TextView itemColorText;
        AppCompatButton itemHexColorButton;
        TextView itemHexInput;
        View itemColorView;
        CheckBox itemLEDColorOverrideCheckbox;


        CustomViewHolder(View itemView) {
            super(itemView);
            itemColorText = itemView.findViewById(R.id.color_led_text);
            itemHexColorButton = itemView.findViewById(R.id.hexColorButton);
            itemHexInput = itemView.findViewById(R.id.hexColor);
            itemColorView = itemView.findViewById(R.id.color);
            itemLEDColorOverrideCheckbox = itemView.findViewById(R.id.use_color_default);
        }
    }

    public interface ILEDColorRecyclerInterface {
        void onColorSelected(LEDItem ledItem);
    }

    private Context context;
    private ILEDColorRecyclerInterface iledColorRecyclerInterface;
    private List<LEDItem> mItems;

    public LEDColorRecyclerAdapter(Context context, List<LEDItem> items, ILEDColorRecyclerInterface iledColorRecyclerInterface) {
        mItems = items;
        this.context = context;
        this.iledColorRecyclerInterface = iledColorRecyclerInterface;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.led_color_selection_item, viewGroup, false);
        return new CustomViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LEDItem ledItem = mItems.get(position);
        switch (ledItem.group) {
            case Default:
            default:
                ((CustomViewHolder) holder).itemLEDColorOverrideCheckbox.setVisibility(View.VISIBLE);
                ((CustomViewHolder)holder).itemColorText.setText(context.getResources().getString(R.string.default_led_tc));
                ledItem.color = DataMatrix.getInt(SettingKeys.Experimental.DATA_LED_DEFAULT_COLOR_HEX_INT);
                break;
            case WorkoutRunning:
                ((CustomViewHolder) holder).itemLEDColorOverrideCheckbox.setVisibility(View.GONE);
                ((CustomViewHolder)holder).itemColorText.setText(context.getResources().getString(R.string.workout_running_led_tc));
                ledItem.color = DataMatrix.getInt(SettingKeys.Experimental.DATA_LED_WORKOUT_RUNNING_COLOR_HEX_INT);
                break;
            case WorkoutPaused:
                ((CustomViewHolder) holder).itemLEDColorOverrideCheckbox.setVisibility(View.GONE);
                ((CustomViewHolder)holder).itemColorText.setText(context.getResources().getString(R.string.workout_paused_led_tc));
                ledItem.color = DataMatrix.getInt(SettingKeys.Experimental.DATA_LED_WORKOUT_PAUSED_COLOR_HEX_INT);
                break;
            case WorkoutSummary:
                ((CustomViewHolder) holder).itemLEDColorOverrideCheckbox.setVisibility(View.GONE);
                ((CustomViewHolder)holder).itemColorText.setText(context.getResources().getString(R.string.workout_summary_led_tc));
                ledItem.color = DataMatrix.getInt(SettingKeys.Experimental.DATA_LED_WORKOUT_SUMMARY_COLOR_HEX_INT);
                break;
            case Concierge:
                ((CustomViewHolder) holder).itemLEDColorOverrideCheckbox.setVisibility(View.GONE);
                ((CustomViewHolder)holder).itemColorText.setText(context.getResources().getString(R.string.concierge_tc));
                ledItem.color = DataMatrix.getInt(SettingKeys.Experimental.DATA_LED_CONCIERGE_COLOR_HEX_INT);
                break;
        }
        ((CustomViewHolder)holder).itemLEDColorOverrideCheckbox.setChecked(DataMatrix.getBoolean(SettingKeys.ProjectOne.DATA_OVERRIDE_LED_COLOR_BOOL));
        ((CustomViewHolder)holder).itemLEDColorOverrideCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.ProjectOne.DATA_OVERRIDE_LED_COLOR_BOOL, isChecked));

        ((CustomViewHolder) holder).itemHexColorButton.setOnClickListener(v -> {
            iledColorRecyclerInterface.onColorSelected(ledItem);
        });
        ((CustomViewHolder)holder).itemHexInput.setText("#" + Integer.toHexString(ledItem.color));
        ((CustomViewHolder)holder).itemColorView.setBackgroundColor(ledItem.color);

    }
    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }
}