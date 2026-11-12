package com.jht.chimera.lib.widget.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.jht.chimera.R;
import com.jht.chimera.lib.widget.ScaleView;
import com.jht.chimera.lib.widget.selectron.SelectronButton;

import java.util.List;

public class StringInfoListViewAdapter extends ArrayAdapter<String> {

    boolean mAllowMultipleLines = false;

    public interface OnInfoClickListener {
        void onClick(int position);
    }

    private TextView mTextView;
    private List<String> mStringList;
    private SelectronButton mSelectronInfo;
    private ListView mListView;
    private OnInfoClickListener mOnInfoClickListener;

    public StringInfoListViewAdapter(Context context, List<String> stringList, ListView listView, OnInfoClickListener onInfoClickListener) {
        super(context, 0, stringList);
        mStringList = stringList;
        mListView = listView;
        mOnInfoClickListener = onInfoClickListener;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.string_info_list_view_item, parent, false);
            ScaleView.scaleContents(convertView);
        }

        mTextView = convertView.findViewById(R.id.txtStringItem);
        if (mAllowMultipleLines) {
            mTextView.setMaxLines(2);
        }
        mTextView.setText(mStringList.get(position));

        mSelectronInfo = convertView.findViewById(R.id.btnInfo);
        // Note getCheckedItemPosition seems to work for mouse click and getSelectedItemPosition for touch click
        if (mListView != null && (mListView.getCheckedItemPosition() == position || mListView.getSelectedItemPosition() == position)) {
            mSelectronInfo.setVisibility(View.VISIBLE);
            mSelectronInfo.setOnClickListener(v -> {
                if (mOnInfoClickListener != null) mOnInfoClickListener.onClick(position);
            });
        } else {
            mSelectronInfo.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    public void allowMultipleLines(boolean allowMultipleLines) {
        mAllowMultipleLines = allowMultipleLines;
    }
}