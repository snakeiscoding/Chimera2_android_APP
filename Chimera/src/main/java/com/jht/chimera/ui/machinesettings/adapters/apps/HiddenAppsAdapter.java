package com.jht.chimera.ui.machinesettings.adapters.apps;

import android.content.ClipData;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;

import java.util.List;

/**
 * Created by Giancarlo.Cuadros on 1/29/2019.
 */
public class HiddenAppsAdapter extends RecyclerView.Adapter<HiddenAppsAdapter.AppViewHolder> {

    public HiddenAppsAdapter() {
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.inactive_app_item, parent, false);
        return new AppViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final AppViewHolder holder, int position) {

        holder.itemView.setOnLongClickListener(v -> {
            AppIcon app = getMatchingAppInHidden(holder.itemTitle.getText().toString());
            if(app != null) {
                ClipData data = ClipData.newPlainText("type", "inactive");
                v.startDragAndDrop(data, new View.DragShadowBuilder(v), new DragData(app.getIconId(), v, holder.getLayoutPosition(), -1, false, holder.itemTitle.getText() + ""), 0);
            }
            return true;
        });

        List<AppIcon> hiddenApps = AppIconManager.getInstance().getHiddenApps();
        AppIcon icon = hiddenApps.get(position);
        holder.itemTitle.setText(icon.getAppName());
        holder.itemBackground.setImageDrawable(icon.getAppIcon());
    }

    private AppIcon getMatchingAppInHidden(String title) {
        for (AppIcon app : AppIconManager.getInstance().getHiddenApps()) {
            if (app.getAppName().equals(title)) {
                return app;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return AppIconManager.getInstance().getHiddenApps().size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView itemBackground;
        TextView itemTitle;

        AppViewHolder(View itemView) {
            super(itemView);
            itemBackground = itemView.findViewById(R.id.app_item_image);
            itemTitle = itemView.findViewById(R.id.app_item_title);
        }
    }
}
