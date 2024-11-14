package me.mamiiblt.instafel.updater;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import rikka.shizuku.Shizuku;

public class Utils {
    public static boolean status = false;

    public static boolean hasShizukuPermission() {
        if (Shizuku.isPreV11()) {
            return false;
        }

        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean getUpdaterStatus() {
        return status;
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
