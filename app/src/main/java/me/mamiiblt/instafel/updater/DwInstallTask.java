package me.mamiiblt.instafel.updater;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;

import me.mamiiblt.instafel.updater.utils.AppPreferences;
import me.mamiiblt.instafel.updater.utils.LogUtils;
import me.mamiiblt.instafel.updater.utils.ShizukuInstaller;
import me.mamiiblt.instafel.updater.utils.Utils;

public class DwInstallTask extends AsyncTask<String, Integer, String> {

    private String arch, type, versionName, version;
    private LogUtils logUtils;
    private SharedPreferences sharedPreferences;
    private Context ctx;

    private double currentDownloadedSizeMegabyte;
    private long fileSize;
    private String formattedFileSize;
    private DecimalFormat df;
    private AppPreferences appPreferences;

    public DwInstallTask(AppPreferences appPreferences, String arch, String type, String versionName, String version, LogUtils logUtils, SharedPreferences sharedPreferences, Context ctx) {
        this.appPreferences = appPreferences;
        this.arch = arch;
        this.type = type;
        this.versionName = versionName;
        this.version = version;
        this.logUtils = logUtils;
        this.sharedPreferences = sharedPreferences;
        this.ctx = ctx;
        this.df = new DecimalFormat("#.##");
    }

    private NotificationManager notificationManager;
    private Notification.Builder notificationBuilder;
    private int notificationId = 105;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();


    }

    @Override
    protected String doInBackground(String... f_url) {
        logUtils.w("Downloading update.. ");
        int count;
        try {
            File f = new File(ctx.getExternalFilesDir(null), "downloaded_apks");
            if (!f.exists()) {
                f.mkdirs();
            } else {
                f.delete();
                f.mkdirs();
            }

            File ifl_update_file = new File(f.getPath(), "update.apk");
            try {
                URL url = new URL(f_url[0]);
                URLConnection connection = url.openConnection();
                connection.connect();

                long fileOfSize = connection.getContentLength();
                fileSize = fileOfSize;
                formattedFileSize = df.format((double) fileOfSize / (1024 * 1024));
                if (formattedFileSize.contains(".")) {
                    String[] parts = formattedFileSize.split("\\.");
                    if (parts.length > 1 && parts[1].length() == 1) {
                        formattedFileSize += "0";
                    }
                }

                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);
                OutputStream output = new FileOutputStream(ifl_update_file.getPath());
                byte data[] = new byte[1024];
                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    currentDownloadedSizeMegabyte = (double) total / (1024 * 1024);;
                    publishProgress((int) ((total * 100) / fileSize));
                    output.write(data, 0, count);
                }

                output.flush();

                output.close();
                input.close();

            } catch (Exception e) {
                e.printStackTrace();
                sendError("Error while downloading update.");
                sendErrorNotif("Update Installation Failed", "An error occured when downloading update");
            }
        } catch (Exception e) {
            sendError("Error while downloading update.");
            e.printStackTrace();
            sendErrorNotif("Update Installation Failed", "An error occured when downloading update");
        }

        return null;
    }

    private void sendErrorNotif(String updateInstallationFailed, String s) {
        if (appPreferences.isAllowNotification()) {
            notificationBuilder = new Notification.Builder(ctx, "ifl_updater_ota")
                    .setContentTitle(updateInstallationFailed)
                    .setContentText(s)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setProgress(0, 0, false)
                    .setPriority(Notification.PRIORITY_DEFAULT);

            notificationManager.notify(notificationId, notificationBuilder.build());
        }
        if (appPreferences.isAllowToast()) {
            Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
        }
    }

    private static final int PROGRESS_UPDATE_INTERVAL = 1200; // 1.2 saniye
    private long lastUpdateTime = 0;
    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (appPreferences.isAllowNotification()) {
            int prog = progress[0];
            String formattedDownloadedSize = df.format(currentDownloadedSizeMegabyte);
            if (formattedDownloadedSize.contains(".")) {
                String[] parts = formattedDownloadedSize.split("\\.");
                if (parts.length > 1 && parts[1].length() == 1) {
                    formattedDownloadedSize += "0";
                }
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime >= PROGRESS_UPDATE_INTERVAL || prog == 100) {
                lastUpdateTime = currentTime;

                String finalFormattedDownloadedSize = formattedDownloadedSize;
                notificationBuilder
                        .setContentText(prog + "% downloaded (" + finalFormattedDownloadedSize + " MB)")
                        .setProgress(100, prog, false);
                notificationManager.notify(notificationId, notificationBuilder.build());
            }
        }
    }

    @Override
    protected void onPostExecute(String file_url) {
        File f = new File(ctx.getExternalFilesDir(null), "downloaded_apks");
        File ifl_update_file = new File(f.getPath(), "update.apk");

        if (ifl_update_file.length() == fileSize) {
            if (ifl_update_file.exists()) {

                if (appPreferences.isAllowNotification()) {
                    notificationBuilder
                            .setContentText("Download complete, installing...")
                            .setProgress(0, 0, true)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done);
                    notificationManager.notify(notificationId, notificationBuilder.build());
                    logUtils.w("Download complete");
                }

                if (ShizukuInstaller.isShizukuSupported()) {
                    if (Utils.hasShizukuPermission()) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                logUtils.w("Copying downloaded apk file to temp.");
                                ShizukuInstaller.runCommand(ctx, "cp " + ifl_update_file + " /data/local/tmp/INSTAFEL_UPDATE.apk");
                                logUtils.w("Installing update");
                                String updateLog = ShizukuInstaller.runCommand(ctx, "pm install /data/local/tmp/INSTAFEL_UPDATE.apk");
                                if (updateLog.trim().equals("Success")) {
                                    logUtils.w("Update installed.");
                                    ShizukuInstaller.runCommand(ctx, "rm -r /data/local/tmp/INSTAFEL_UPDATE.apk");
                                    logUtils.w("Downloaded apk & temp apk removed");
                                    if (appPreferences.isAllowNotification()) {
                                        notificationBuilder = new Notification.Builder(ctx, "ifl_updater_ota")
                                                .setContentTitle("Update Successfully Completed")
                                                .setContentText("Instafel updated to v" + version)
                                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                                .setProgress(0, 0, false)
                                                .setPriority(Notification.PRIORITY_DEFAULT);

                                        notificationManager.notify(notificationId, notificationBuilder.build());
                                    }
                                    if (appPreferences.isAllowToast()) {
                                        Toast.makeText(ctx, "Instafel succesfully updated.", Toast.LENGTH_SHORT).show();
                                    }
                                    logUtils.w("Instafel succesfully updated.");
                                } else {
                                    logUtils.w("Update installation failed.");
                                    sendErrorNotif("Update Installation Failed", "An error occured when installing update");
                                }

                            }
                        }).start();
                    } else {
                        logUtils.w("Shizuku permission is not granted for Install.");
                    }
                } else {
                    logUtils.w("Shizuku is not supported.");
                }
            }
        }
    }

    private void sendError(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            LogUtils logUtils = new LogUtils(ctx);
            logUtils.w("ERROR: " + message);
        });
    }
}
