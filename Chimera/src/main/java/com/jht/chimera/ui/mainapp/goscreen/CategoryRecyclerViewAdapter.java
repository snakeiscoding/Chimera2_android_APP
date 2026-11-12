package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;

import java.util.List;

/**
 * Created by AlexN on 11/3/2018.
 */

public class CategoryRecyclerViewAdapter extends RecyclerView.Adapter<CategoryRecyclerViewAdapter.CustomViewHolder> {

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        TextView itemNameText;
        ImageView itemBackground;
        ImageView itemIcon;

        CustomViewHolder(View itemView) {
            super(itemView);
            itemNameText = itemView.findViewById(R.id.category_text);
            itemBackground = itemView.findViewById(R.id.category_background);
            itemIcon = itemView.findViewById(R.id.category_icon);
        }
    }

    public interface ICategoryRecyclerInterface {
        void onItemClicked(int position);
    }

    private int defaultSelectedIndex = 0;

    private List<CategoryItem> mItems;
    private boolean mShowNames = true;
    private Context context;
    private int selectedIndex = -1;
    private ImageView selectedBackground;
    private ImageView selectedIcon;
    private TextView selectedText;
    private PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(0xFFFCAF1A, PorterDuff.Mode.MULTIPLY);
    private PorterDuffColorFilter colorFilterWhite = new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
    private ICategoryRecyclerInterface iCategoryRecyclerInterface;

    public CategoryRecyclerViewAdapter(Context context, List<CategoryItem> items, ICategoryRecyclerInterface iCategoryRecyclerInterface) {
        mItems = items;
        this.context = context;
        this.iCategoryRecyclerInterface = iCategoryRecyclerInterface;
    }

    //Used in Parker Flavor
    public void setShowNames(boolean shown) { mShowNames = shown; }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate( (mShowNames ? R.layout.category_list_item : R.layout.category_list_item_no_name), viewGroup, false);
        return new CustomViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final CustomViewHolder customViewHolder, int position) {
        final CategoryItem item = mItems.get(position);
        customViewHolder.itemNameText.setText(item.buttonTitle);
        customViewHolder.itemIcon.setImageDrawable(ContextCompat.getDrawable(context, item.icon));
        customViewHolder.itemIcon.setColorFilter(colorFilterWhite); // Setting the color filter removes some jagged edges
        customViewHolder.itemBackground.setContentDescription("category" + position); // add descriptor for DQA NON-NLS
        customViewHolder.itemBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectItem(customViewHolder);
                iCategoryRecyclerInterface.onItemClicked(selectedIndex);
            }
        });
        //Select default index once
        if (position == defaultSelectedIndex) {
            selectItem(customViewHolder);
        }
    }

    private void selectItem(CustomViewHolder customViewHolder) {
        if (selectedIndex != customViewHolder.getBindingAdapterPosition()) {
            selectedIndex = customViewHolder.getBindingAdapterPosition();
            if (selectedBackground != null) {
                selectedText.setSelected(false);
                selectedIcon.setColorFilter(colorFilterWhite); // Set back to white using this to prevent jagged edges
                selectedBackground.setSelected(false);
            }
            customViewHolder.itemNameText.setSelected(true);
            customViewHolder.itemBackground.setSelected(true);
            customViewHolder.itemIcon.setColorFilter(colorFilter);

            selectedText = customViewHolder.itemNameText;
            selectedIcon = customViewHolder.itemIcon;
            selectedBackground = customViewHolder.itemBackground;
        }

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
