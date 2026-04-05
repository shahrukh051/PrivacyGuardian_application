package com.privacy.guardian;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Persists sensor access events in SharedPreferences.
 * Each entry: { pkg, sensor, timestamp, background }
 */
public class ActivityLog {
    private static final String PREF = "pg_activity_log";
    private static final String KEY  = "entries";
    private static final int    MAX  = 200;

    public static void log(Context ctx, String pkg, String appName,
                           String sensor, boolean background) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            JSONObject e = new JSONObject();
            e.put("pkg",    pkg);
            e.put("app",    appName);
            e.put("sensor", sensor);
            e.put("bg",     background);
            e.put("ts",     System.currentTimeMillis());
            // Prepend newest
            JSONArray fresh = new JSONArray();
            fresh.put(e);
            for (int i = 0; i < Math.min(arr.length(), MAX - 1); i++) fresh.put(arr.get(i));
            sp.edit().putString(KEY, fresh.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<LogEntry> getAll(Context ctx) {
        List<LogEntry> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                LogEntry le = new LogEntry();
                le.pkg    = o.getString("pkg");
                le.app    = o.getString("app");
                le.sensor = o.getString("sensor");
                le.bg     = o.getBoolean("bg");
                le.ts     = o.getLong("ts");
                le.tsStr  = sdf.format(new Date(le.ts));
                list.add(le);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }

    /** Weekly usage count per sensor for trend analysis */
    public static Map<String, Integer> weeklyTrend(Context ctx) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Camera", 0); map.put("Mic", 0); map.put("GPS", 0);
        long cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        for (LogEntry e : getAll(ctx)) {
            if (e.ts < cutoff) continue;
            if (map.containsKey(e.sensor)) map.put(e.sensor, map.get(e.sensor) + 1);
        }
        return map;
    }

    public static class LogEntry {
        public String pkg, app, sensor, tsStr;
        public boolean bg;
        public long ts;
    }
}
