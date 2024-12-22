package me.mamiiblt.instafel.updater.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import me.mamiiblt.instafel.updater.BuildConfig;
import me.mamiiblt.instafel.updater.R;
import rikka.shizuku.Shizuku;

public class Utils {
    public static void showBatteryDialog(Context ctx) {
        if (Utils.getBatteryRestrictionStatus(ctx)) {
            new MaterialAlertDialogBuilder(ctx)
                    .setTitle(ctx.getString(R.string.battery_dialog_title))
                    .setMessage(ctx.getString(R.string.battery_dialog_msg))
                    .setPositiveButton(ctx.getString(R.string.dialog_ok), (dialog, which) -> openBatterySettings(ctx))
                    .setCancelable(false)
                    .show();
        }
    }

    public static void showAppInfoDialog(Context ctx) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(ctx.getString(R.string.about_app))
                .setMessage("version: v" + BuildConfig.VERSION_NAME + "-" + BuildConfig.BUILD_TYPE +
                        "\nsdk: API " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ")" +
                        "\ndevice: " + Build.DEVICE +
                        "\nproduct: " +  Build.PRODUCT +
                        "\nbuild_id: " + Build.ID  +
                        "\n\n" + ctx.getString(R.string.developed_by)
                )
                .setPositiveButton(ctx.getString(R.string.dialog_ok), (dialog, which) -> openBatterySettings(ctx))
                .show();
    }

    private static void openBatterySettings(Context ctx) {
        String packageName = ctx.getPackageName();
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + packageName));
        ctx.startActivity(intent);
    }

    public static boolean getBatteryRestrictionStatus(Context ctx) {
        PowerManager powerManager = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        String packageName = ctx.getPackageName();

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                return true;
            } catch (ActivityNotFoundException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public static String getAppVersionCode(Context ctx, String packageName) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "NOT_INSTALLED";
        }
    }
}
