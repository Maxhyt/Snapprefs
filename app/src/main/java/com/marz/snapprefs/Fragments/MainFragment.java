package com.marz.snapprefs.Fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.marz.snapprefs.BuildConfig;
import com.marz.snapprefs.Obfuscator;
import com.marz.snapprefs.R;


public class MainFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_layout,
                container, false);
        TextView build = (TextView) view.findViewById(R.id.build_version);
        TextView sc_version = (TextView) view.findViewById(R.id.sc_version);
        build.setText(build.getText() + " " + BuildConfig.VERSION_NAME);
        sc_version.setText(sc_version.getText() + " " + Obfuscator.SUPPORTED_VERSION_CODENAME);

        return view;
    }
}
