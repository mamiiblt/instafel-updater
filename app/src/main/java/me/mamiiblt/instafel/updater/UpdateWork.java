package me.mamiiblt.instafel.updater;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ServiceConfigurationError;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.mamiiblt.instafel.updater.utils.AppPreferences;
import me.mamiiblt.instafel.updater.utils.LogUtils;
import me.mamiiblt.instafel.updater.utils.ShizukuInstaller;
import me.mamiiblt.instafel.updater.utils.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateWork extends Worker {

    private static final String CHANNEL_ID = "ifl_updater_channel";
    private static final int NOTIFICATION_ID = 1;
    private AppPreferences appPreferences;
    private Context ctx;
    private double currentDownloadedSizeMegabyte;
    private long fileSize;
    private String formattedFileSize;
    private DecimalFormat df;
    private NotificationCompat.Builder notificationBuilder;
    private int notificationId = 105;
    private String uVersion;
    private LogUtils logUtils;
    private NotificationManager notificationManager;

    public UpdateWork(Context ctx, WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        logUtils = new LogUtils(getApplicationContext());
        ctx = getApplicationContext();
        logUtils.w("");
        logUtils.w("Work is running.");
        notificationBuilder = new NotificationCompat.Builder(ctx, CHANNEL_ID);
        notificationManager = ctx.getSystemService(NotificationManager.class);
        // Start Shizuku User Service for run commands

        logUtils.w("Starting UserService");
        ShizukuInstaller.ensureUserService(ctx);

        // Get arch and type from SharedPreferences

        String arch; String type;
        String prefArch = preferences.getString("checker_arch", "non");
        String prefType = preferences.getString("checker_type", "non");
        if (prefArch.equals("arm64-v8a (64-bit)")) {
            arch = "arm64";
        } else if (prefArch.equals("armeabi-v7a (32-bit)")) {
            arch = "arm32";
        } else {
            arch = null;
            sendError("You selected invalid arch, work is stopped.");
        }

        if (prefType.equals("Unclone")) {
            type = "uc";
        } else if (prefType.equals("Clone")){
            type = "c";
        } else {
            type = null;
            sendError("You selected invalid installation type, work is stopped.");
        }

        logUtils.w("Work arch is " + arch + " and type is " + type);


        // Set AppPreferences

        appPreferences = new AppPreferences(
                preferences.getString("checker_interval", "4"),
                arch, type,
                preferences.getBoolean("send_notification", true),
                preferences.getBoolean("send_toast", true),
                preferences.getBoolean("use_mobile_data", false),
                preferences.getBoolean("12_hour_rule", false));

        // Check Network Statues

        boolean mDataAllowStatus;

        if (isNetworkAvailable(getApplicationContext())) {
            if (isMobileDataConnected(getApplicationContext())) {
                if (appPreferences.isAllowUseMobileData()) {
                    mDataAllowStatus = true;
                } else {
                    mDataAllowStatus = false;
                }
            } else {
                mDataAllowStatus = true;
            }

            if (mDataAllowStatus) {
                // Get IG's Version Code

                String versionName;
                if (type.equals("uc")) {
                    versionName = Utils.getAppVersionCode(getApplicationContext(), "com.instagram.android");
                } else if (type.equals("c")) {
                    versionName = Utils.getAppVersionCode(getApplicationContext(), "com.instafel.android");
                } else {
                    versionName = null;
                }

                if (versionName != null) {
                    if (versionName.equals("NOT_INSTALLED")) {
                        sendError("IG (Instafel / Instagram) is not installed.");
                    } else {
                        logUtils.w("Installed IG version is " + versionName);
                    }
                } else {
                    sendError("versionCode is NULL");
                }

                // Send API Request

                try {
                    OkHttpClient client = new OkHttpClient();
                    String urlPart;
                    if (arch.equals("arm64")) {
                        urlPart = "arm64-v8a";
                    } else {
                        urlPart = "armeabi-v7a";
                    }
                    Request request = new Request.Builder()
                            .url("https://api.github.com/repos/mamiiblt/instafel_release_" + urlPart + "/releases/latest")
                            .build();
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        JSONObject res = new JSONObject(response.body().string());
                        String version = res.getString("body").split("\n")[1].split("v")[1].split(" ")[0];
                        if (versionName.equals(version)) {
                            logUtils.w("Update not needed, app is up-to-date.");
                            return Result.success();
                        } else {
                            logUtils.w("New version found " + version);
                            if (appPreferences.isAllow12HourMode()) {
                                String publishTs = res.getString("published_at");
                                OffsetDateTime parsedDateTime = OffsetDateTime.parse(publishTs, DateTimeFormatter.ISO_DATE_TIME);
                                OffsetDateTime currentDateTime = OffsetDateTime.now(ZoneOffset.UTC);
                                Duration duration = Duration.between(parsedDateTime, currentDateTime);

                                if (duration.toHours() >= 12) {
                                    // allow
                                } else {
                                    logUtils.w("Duration is " + duration + ". So update stopped.");
                                    return Result.success();
                                }
                            }

                            JSONArray assets = res.getJSONArray("assets");

                            String b_download_url = null;

                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                if (asset.getString("name").contains("_" + type + "_")) {
                                    b_download_url = asset.getString("browser_download_url");
                                }
                            }

                            if (b_download_url != null) {

                                // DOWNLOAD & INSTALL UPDATE
                                uVersion = version;
                                Intent fgServiceIntent = new Intent(ctx, InstafelUpdateService.class);
                                fgServiceIntent.putExtra("file_url", b_download_url);
                                fgServiceIntent.putExtra("version", uVersion);
                                ctx.startService(fgServiceIntent);
                            } else {
                                sendError("Updater can't found update asset!");
                            }
                        }
                    } else {
                        sendError("Response code is not 200 (OK).");
                    }
                } catch (Exception e) {
                    sendError("Error while sending / reading API request");
                }
            } else {
                logUtils.w("Update couldn't checked because mobile data is enabled.");
            }
        } else {
            logUtils.w("We can't connect to Internet, check failed.");
        }

        return Result.success();
    }

    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            return connectivityManager.getNetworkCapabilities(network) != null;
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    public boolean isMobileDataConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                return networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            }
        }
        return false;
    }

    private void sendError(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {

            // WRITE LOG

            LogUtils logUtils = new LogUtils(getApplicationContext());
            logUtils.w("ERROR: " + message);

            // SHOW NOTIFICATION OR TOAST

            if (appPreferences.isAllowNotification()) {
                NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Warning Channel",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );
                    notificationManager.createNotificationChannel(channel);
                }

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                        .setContentTitle("Work Error")
                        .setContentText(message)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            } else {
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
