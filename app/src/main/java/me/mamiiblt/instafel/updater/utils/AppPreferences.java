package me.mamiiblt.instafel.updater.utils;

public class AppPreferences {
    private String checker_interval, arch, itype;
    private boolean send_notification, send_toast, use_mobile_data, hour_mode12;

    public AppPreferences(String checker_interval, String arch, String itype, boolean send_notification, boolean send_toast, boolean use_mobile_data, boolean hour_mode12) {
        this.checker_interval = checker_interval;
        this.arch = arch;
        this.itype = itype;
        this.send_notification = send_notification;
        this.send_toast = send_toast;
        this.use_mobile_data = use_mobile_data;
        this.hour_mode12 = hour_mode12;
    }

    public String getChecker_interval() {
        return checker_interval;
    }

    public String getArch() {
        return arch;
    }

    public String getItype() {
        return itype;
    }

    public boolean isAllowNotification() {
        return send_notification;
    }

    public boolean isAllowToast() {
        return send_toast;
    }

    public boolean isAllowUseMobileData() {
        return use_mobile_data;
    }

    public boolean isAllow12HourMode() {
        return hour_mode12;
    }
}
