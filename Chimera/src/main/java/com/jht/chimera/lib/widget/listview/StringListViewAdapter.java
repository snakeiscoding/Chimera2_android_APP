package com.jht.chimera.lib.widget.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jht.chimera.lib.widget.ScaleView;

import java.util.List;

public class StringListViewAdapter extends ArrayAdapter<String> {

    private TextView mTextView;
    private int mLayoutResourceId;
    private int mTextViewResourceId;
    private List<String> mStringList;

    public StringListViewAdapter(Context context, int layoutResourceId, int textViewResourceId, List<String> stringList) {
        super(context, layoutResourceId, textViewResourceId, stringList);
        mLayoutResourceId = layoutResourceId;
        mTextViewResourceId = textViewResourceId;
        mStringList = stringList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(mLayoutResourceId, parent, false);
            ScaleView.scaleContents(convertView);
        }

        mTextView = convertView.findViewById(mTextViewResourceId);
        mTextView.setText(mStringList.get(position));

        return convertView;
    }

}