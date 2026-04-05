package com.privacy.guardian;
import android.content.*; 
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
            ctx.startForegroundService(new Intent(ctx, SensorMonitorService.class));
    }
}
