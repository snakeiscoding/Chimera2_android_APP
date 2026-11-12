package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.jht.chimera.R;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.ui.mainapp.workoutapps.AppAdapter;

import java.util.List;

public class SetupAppAdapter extends AppAdapter {

    public interface OnIconClickListener {
        void onClick(AppIcon appIcon);
    }

    OnIconClickListener onIconClickListener;

    public SetupAppAdapter(Context context, int resource, int textViewResourceId, List<AppIcon> appIconList) {
        super(context, resource, textViewResourceId, appIconList);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        ImageView button = view.findViewById(R.id.appIconImage);
        button.setOnClickListener(v -> {
            if (onIconClickListener != null) onIconClickListener.onClick(appIconList.get(position));
        });

        return view;
    }

    void setOnIconClickListener(OnIconClickListener onIconClickListener) {
        this.onIconClickListener = onIconClickListener;
    }

}
