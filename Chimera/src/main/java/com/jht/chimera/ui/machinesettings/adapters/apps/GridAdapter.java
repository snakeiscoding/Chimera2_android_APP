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
 * Displays the application ordering page
 */
public class GridAdapter extends RecyclerView.Adapter<GridAdapter.GridItemViewHolder> {
    private int mPage;
    private ViewGroup mParentRootView;

    GridAdapter(int page, ViewGroup parentRootView) {
        this.mPage = page;
        this.mParentRootView = parentRootView;
    }

    @NonNull
    @Override
    public GridItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.active_app_item, parent, false);
        return new GridItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final GridItemViewHolder holder, int position) {

        holder.itemView.setOnLongClickListener(v -> {
            if (mPage > 0) {
                mParentRootView.findViewById(R.id.previousApplicationPage).setVisibility(View.VISIBLE);
            }

            List<AppIcon> appOrder = AppIconManager.getInstance().getAppOrder();
            // Check if we should show the next page icon (only if next page has apps on it)
            if ((double) appOrder.size() / (double) AppIconManager.APPS_PER_PAGE > (double) (mPage + 1)) {
                mParentRootView.findViewById(R.id.nextApplicationPage).setVisibility(View.VISIBLE);
            }
            ClipData data2 = ClipData.newPlainText("type", "active");
            List<AppIcon> appIcons = AppIconManager.getInstance().getAppsOnPage(mPage);
            v.startDragAndDrop(data2, new View.DragShadowBuilder(v), new DragData(appIcons.get(position).getIconId(), v, holder.getLayoutPosition(), mPage, true, holder.itemTitle.getText() + ""), 0);
            return true;
        });
        updateHolderWithApp(holder, position);
    }

    private void updateHolderWithApp(GridItemViewHolder holder, int position) {
        List<AppIcon> pageIcons = AppIconManager.getInstance().getAppsOnPage(mPage);
        AppIcon app = pageIcons.get(position);
        holder.itemTitle.setText(app.getAppName());
        holder.itemBackground.setImageDrawable(app.getAppIcon());
    }

    @Override
    public int getItemCount() {
        List<AppIcon> apps = AppIconManager.getInstance().getAppsOnPage(mPage);
        return apps.size();
    }

    static class GridItemViewHolder extends RecyclerView.ViewHolder {

        ImageView itemBackground;
        TextView itemTitle;

        GridItemViewHolder(View itemView) {
            super(itemView);
            itemBackground = itemView.findViewById(R.id.active_app_item_image);
            itemTitle = itemView.findViewById(R.id.active_app_item_title);
        }
    }
}
