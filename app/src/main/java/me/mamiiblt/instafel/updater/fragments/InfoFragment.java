package me.mamiiblt.instafel.updater.fragments;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import me.mamiiblt.instafel.updater.UpdateWorkHelper;
import me.mamiiblt.instafel.updater.utils.LogUtils;
import me.mamiiblt.instafel.updater.R;
import me.mamiiblt.instafel.updater.utils.ShizukuInstaller;
import me.mamiiblt.instafel.updater.utils.Utils;
import rikka.shizuku.Shizuku;

public class InfoFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public InfoFragment() {
    }

    public static InfoFragment newInstance(String param1, String param2) {
        InfoFragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }
    private TextView viewShizukuStatus, viewArchitecture, viewIType, viewStatus, viewNextCheckStatus;
    private MaterialCardView viewNextCheck;
    private Button viewStartBtn, viewStopBtn;
    private FloatingActionButton viewFab;
    private SharedPreferences sharedPreferences;
    public final String STRING_AUTHORIZED = "✔ Authorized";
    public final String STRING_UNAUTHORIZED = "❌ Unauthorized";
    public final String STRING_RUNNING = "✔ Running";
    public final String STRING_STOPPED = "❌ Stopped";
    private LogUtils logUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       View view = inflater.inflate(R.layout.fragment_info, container, false);
       viewShizukuStatus = view.findViewById(R.id.statusTextView);
       viewArchitecture = view.findViewById(R.id.statusTextView2);
       viewIType = view.findViewById(R.id.statusTextView3);
       viewStatus = view.findViewById(R.id.statusTextView4);
       viewStartBtn = view.findViewById(R.id.startButton);
       viewStopBtn = view.findViewById(R.id.stopButton);
       viewNextCheck = view.findViewById(R.id.cardView5);
       viewNextCheckStatus = view.findViewById(R.id.statusTextView5);
       viewFab = view.findViewById(R.id.fab);
       logUtils = new LogUtils(getActivity());


       sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
       SharedPreferences.Editor prefEditor = sharedPreferences.edit();

       viewArchitecture.setText(sharedPreferences.getString("checker_arch", "NULL"));
       viewIType.setText(sharedPreferences.getString("checker_type", "NULL"));

       if (Utils.hasShizukuPermission()) {
           viewShizukuStatus.setText(STRING_AUTHORIZED);
       } else {
           viewShizukuStatus.setText(STRING_UNAUTHORIZED);
           view.findViewById(R.id.shizuku_status).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                    if (Utils.hasShizukuPermission()) {
                        viewShizukuStatus.setText(STRING_AUTHORIZED);
                    } else {
                        Shizuku.requestPermission(100);
                    }
               }
           });
       }

       updateUI();

       viewStartBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               logUtils.w("Updater started.");
               UpdateWorkHelper.scheduleWork(getActivity());
               updateUI();
           }
       });

       viewStopBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               logUtils.w("Updater stopped.");
               UpdateWorkHelper.cancelWork(getActivity());
               updateUI();
           }
       });

       viewFab.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
                UpdateWorkHelper.restartWork(getActivity());
           }
       });
       viewFab.setOnLongClickListener(new View.OnLongClickListener() {
           @Override
           public boolean onLongClick(View view) {
               prefEditor.putBoolean("15m_rule", !sharedPreferences.getBoolean("15m_rule", false));
               prefEditor.apply();
               Toast.makeText(getActivity(), "15M Rule changed, restarting work.", Toast.LENGTH_SHORT).show();
               return false;
           }
       });
       return view;
    }

    private void updateUI() {
        UpdateWorkHelper.isWorkManagerActive(getActivity(), new UpdateWorkHelper.WorkManagerActiveCallback() {
            @Override
            public void onResult(boolean isActive) {
                if (isActive) {
                    if (sharedPreferences.getBoolean("15m_rule", false)) {
                        viewStatus.setText(STRING_RUNNING + " with 15 minute mode.");
                    } else {
                        viewStatus.setText(STRING_RUNNING + " with " + sharedPreferences.getString("checker_interval", "NULL") + " hour mode.");
                    }
                    viewStartBtn.setEnabled(false);
                    viewStopBtn.setEnabled(true);
                    viewNextCheck.setVisibility(View.GONE);
                } else {
                    viewStatus.setText(STRING_STOPPED);
                    viewStartBtn.setEnabled(true);
                    viewStopBtn.setEnabled(false);
                    viewNextCheck.setVisibility(View.GONE);
                }
            }
        });
    }
}