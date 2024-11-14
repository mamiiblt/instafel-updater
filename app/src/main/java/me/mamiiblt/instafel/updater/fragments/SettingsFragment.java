package me.mamiiblt.instafel.updater.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import android.widget.Toast;

import me.mamiiblt.instafel.updater.R;
import me.mamiiblt.instafel.updater.UpdateWorkHelper;

public class SettingsFragment extends PreferenceFragmentCompat {
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.app_options, rootKey);

        SwitchPreferenceCompat dynamicColorPreference = findPreference("material_you");
        if (dynamicColorPreference != null) {
            dynamicColorPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isDynamicColorEnabled = (Boolean) newValue;

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                sharedPreferences.edit().putBoolean("material_you", isDynamicColorEnabled).apply();

                getActivity().recreate();
                return true;
            });
        }

        ListPreference checkerInterval = findPreference("checker_interval");
        checkerInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                sharedPreferences.edit().putString("checker_interval", String.valueOf(newValue)).apply();
                Toast.makeText(getContext(), "Work is restarted.", Toast.LENGTH_SHORT).show();
                UpdateWorkHelper.restartWork(getActivity());
                return true;
            }
        });

        Preference sourceCode = findPreference("source_code");
        sourceCode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(sourceCode.getSummary().toString()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
                return false;
            }
        });

        Preference createIssue = findPreference("create_issue");
        createIssue.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(sourceCode.getSummary().toString() + "/issues"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
                return false;
            }
        });
    }
}