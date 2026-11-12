package com.jht.chimera.ui.mymedia;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.jht.chimera.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class InstructionFragment extends Fragment {
    private static final String TAG = "InstructionFragment";
    private static InstructionFragment sInstance;

    public static InstructionFragment newInstance() {
        return new InstructionFragment();
    }


    public InstructionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.instruction, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sInstance = null;
    }
}
