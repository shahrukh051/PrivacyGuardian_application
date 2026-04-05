package com.privacy.guardian;

import android.app.*;
import android.content.Intent;
import android.os.*;
import java.util.List;

/**
 * Foreground service — scans for BG sensor usage every 5 minutes.
 * Context-aware: only alerts on suspicious BG access (not foreground).
 * Auto-restarts on boot via BootReceiver.
 */
public class SensorMonitorService extends Service {
    private static final int    SCAN_INTERVAL = 5 * 60 * 1000;
    private static final int    FG_NOTIF_ID   = 999;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable scanTask = this::doScan;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(FG_NOTIF_ID,
            NotificationHelper.buildServiceNotif(this, "Monitoring sensor activity..."));
        handler.postDelayed(scanTask, SCAN_INTERVAL);
        return START_STICKY;
    }

    private void doScan() {
        new Thread(() -> {
            List<AppInfo> apps = new PermissionScanner(this).scanAll();
            for (AppInfo a : apps) {
                if (a.hasBackgroundActivity() && a.riskScore >= 60) {
                    NotificationHelper.sendSensorAlert(this, a);
                }
            }
        }).start();
        handler.postDelayed(scanTask, SCAN_INTERVAL);
    }

    @Override public void onDestroy() { handler.removeCallbacks(scanTask); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}
