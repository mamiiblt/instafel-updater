package me.mamiiblt.instafel.updater.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import rikka.shizuku.Shizuku;

public class Utils {

    public static boolean status = false;

    public static boolean hasShizukuPermission() {
        if (Shizuku.isPreV11()) {
            return false;
        }

        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    public static String getAppVersionCode(Context ctx, String packageName) {
        try {
            /*PackageManager packageManager = ctx.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            Log.v("IFL", "p: " + packageName);*/
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "NOT_INSTALLED";
        }
    }

    public static void showDialog(Context ctx, String title, String message) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(android.R.string.yes, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


}
