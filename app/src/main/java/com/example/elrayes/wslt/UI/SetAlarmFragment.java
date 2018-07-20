package com.example.elrayes.wslt.UI;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.elrayes.wslt.R;
import com.xw.repo.BubbleSeekBar;

import mehdi.sakout.fancybuttons.FancyButton;


/**
 * A simple {@link Fragment} subclass.
 */
public class SetAlarmFragment extends DialogFragment {

    BubbleSeekBar seekBar;
    FancyButton submit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_set_alarm, container, false);
        seekBar = root.findViewById(R.id.radius_seekbar);
        submit = root.findViewById(R.id.set_alarm);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MapActivity) getActivity()).setAlarm(seekBar.getProgress());
                ((MapActivity) getActivity()).enableAlarm();
                SetAlarmFragment.this.dismiss();
            }
        });
    }
}
