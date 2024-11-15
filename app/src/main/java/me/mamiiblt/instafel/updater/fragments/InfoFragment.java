package me.mamiiblt.instafel.updater.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import me.mamiiblt.instafel.updater.update.UpdateWorkHelper;
import me.mamiiblt.instafel.updater.utils.LogUtils;
import me.mamiiblt.instafel.updater.R;
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
    private TextView viewShizukuStatus, viewArchitecture, viewIType, viewStatus;
    private Button viewStartBtn, viewStopBtn;
    private FloatingActionButton viewFab;
    private SharedPreferences sharedPreferences;
    public String STRING_AUTHORIZED, STRING_UNAUTHORIZED, STRING_RUNNING, STRING_STOPPED;
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
       viewFab = view.findViewById(R.id.fab);
       logUtils = new LogUtils(getActivity());

       Context ctx = getContext();
       STRING_AUTHORIZED = ctx.getString(R.string.authorized);
       STRING_UNAUTHORIZED = ctx.getString(R.string.unauthorized);
       STRING_STOPPED = ctx.getString(R.string.stopped);

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
               logUtils.w(getContext().getString(R.string.upd_started));
               Toast.makeText(ctx, getContext().getString(R.string.upd_started), Toast.LENGTH_SHORT).show();
               UpdateWorkHelper.scheduleWork(getActivity());
               updateUI();
           }
       });

       viewStopBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               logUtils.w(getContext().getString(R.string.upd_stopped));
               Toast.makeText(ctx, getContext().getString(R.string.upd_stopped), Toast.LENGTH_SHORT).show();
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
                        viewStatus.setText(getContext().getString(R.string.running, "15 " + getContext().getString(R.string.minute)));
                    } else {
                        viewStatus.setText(getContext().getString(R.string.running, sharedPreferences.getString("checker_interval", "NULL")));
                    }
                    viewStartBtn.setEnabled(false);
                    viewStopBtn.setEnabled(true);
                } else {
                    viewStatus.setText(STRING_STOPPED);
                    viewStartBtn.setEnabled(true);
                    viewStopBtn.setEnabled(false);
                }
            }
        });
    }
}