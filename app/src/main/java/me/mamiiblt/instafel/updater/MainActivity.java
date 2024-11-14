package me.mamiiblt.instafel.updater;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;

import me.mamiiblt.instafel.updater.utils.LogUtils;
import me.mamiiblt.instafel.updater.utils.ShizukuInstaller;
import me.mamiiblt.instafel.updater.utils.Utils;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private TextView titleView;
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

    SharedPreferences prefsApp;
    SharedPreferences.Editor prefsEditor;
    LogUtils logUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsApp = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEditor = prefsApp.edit();
        // Log.v("IFL", prefsApp.getString("checker_arch", "NULL"));
        logUtils = new LogUtils(this);


        if (!prefsApp.getBoolean("init", false)) {
            prefsEditor.putString("checker_arch", "NULL");
            prefsEditor.putString("checker_type", "NULL");
            prefsEditor.putBoolean("init", true);
            prefsEditor.apply();
        }

        if (prefsApp.getBoolean("material_you", true) == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DynamicColors.applyToActivityIfAvailable(this);
            } else {
                Toast.makeText(this, "Your android version is below 12", Toast.LENGTH_SHORT).show();
            }
        } else {
            setTheme(R.style.Base_Theme_InstafelUpdater);
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        if (prefsApp.getString("checker_arch", "NULL").equals("NULL") || prefsApp.getString("checker_type", "NULL").equals("NULL")) {
            Intent intent = new Intent(MainActivity.this, SetupActivity.class);
            startActivity(intent);
            finish();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 105);
                } else {
                    Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
                    if (!Utils.hasShizukuPermission()) { // if permission not granted
                        logUtils.w("Shizuku permission is not granted, requesting permission.");
                        Shizuku.requestPermission(100);
                    }
                }
            }
        }

        titleView = findViewById(R.id.title);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                if (destination.getId() == R.id.nav_info) {
                    titleView.setText("Status");
                } else if (destination.getId() == R.id.nav_logs) {
                    titleView.setText("Logs");
                } else if (destination.getId() == R.id.nav_settings) {
                    titleView.setText("Settings");
                }
            }
        });
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        if (requestCode == 100) {
            boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                recreate();
                logUtils.w("Shizuku permission is granted.");
            } else {
                logUtils.w("Shizuku permission is rejected.");
                Utils.showDialog(this, "Permission Rejected", "Please authorize Instafel Updater from Shizuku App");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 105) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Permission rejected, please allow from settings", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }
}