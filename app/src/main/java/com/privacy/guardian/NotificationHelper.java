package com.privacy.guardian;

import android.app.*;
import android.content.*;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    public static final String CH_ALERTS    = "pg_alerts";
    public static final String CH_SERVICE   = "pg_service";
    private static int nid = 10;

    public static void createChannels(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel alerts = new NotificationChannel(
            CH_ALERTS, ctx.getString(R.string.ch_alerts_name), NotificationManager.IMPORTANCE_HIGH);
        alerts.setDescription(ctx.getString(R.string.ch_alerts_desc));
        alerts.enableVibration(true);
        alerts.setLightColor(0xFFEF4444);
        alerts.enableLights(true);

        NotificationChannel service = new NotificationChannel(
            CH_SERVICE, ctx.getString(R.string.ch_service_name), NotificationManager.IMPORTANCE_LOW);
        service.setDescription(ctx.getString(R.string.ch_service_desc));

        nm.createNotificationChannel(alerts);
        nm.createNotificationChannel(service);
    }

    /** Alert: BG sensor access detected */
    public static void sendSensorAlert(Context ctx, AppInfo app) {
        createChannels(ctx);
        StringBuilder sensors = new StringBuilder();
        if (app.bgMic)      sensors.append(ctx.getString(R.string.sensor_mic));
        if (app.bgCamera)   sensors.append(ctx.getString(R.string.sensor_cam));
        if (app.bgLocation) sensors.append(ctx.getString(R.string.sensor_gps));

        String title = ctx.getString(R.string.notif_sensor_alert_title, app.appName);
        String body  = ctx.getString(R.string.notif_sensor_alert_body, sensors.toString(), app.riskScore, ctx.getString(app.getRiskLabelRes()));

        Intent i = new Intent(ctx, AppDetailActivity.class);
        i.putExtra("pkg",      app.packageName);
        i.putExtra("name",     app.appName);
        i.putExtra("category", app.category);
        i.putExtra("risk",     app.riskScore);
        i.putExtra("hasCamera",   app.hasCamera);   i.putExtra("hasMic",   app.hasMic);
        i.putExtra("hasLocation", app.hasLocation); i.putExtra("hasContacts", app.hasContacts);
        i.putExtra("hasStorage",  app.hasStorage);
        i.putExtra("bgCamera",    app.bgCamera);    i.putExtra("bgMic",    app.bgMic);
        i.putExtra("bgLocation",  app.bgLocation);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getActivity(ctx, nid, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(ctx, CH_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title).setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi).setAutoCancel(true)
            .setColor(0xFFEF4444).build();

        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(nid++, n);
    }

    /** Persistent foreground service notification */
    public static Notification buildServiceNotif(Context ctx, String text) {
        createChannels(ctx);
        Intent i = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(ctx, CH_SERVICE)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(ctx.getString(R.string.notif_title_pg_active))
            .setContentText(text).setContentIntent(pi).setOngoing(true).build();
    }
}
