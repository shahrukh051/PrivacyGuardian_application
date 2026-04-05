package com.privacy.guardian;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Reads installed apps, permissions (PackageManager),
 * background sensor access (AppOpsManager),
 * and weekly usage frequency (UsageStatsManager).
 */
public class PermissionScanner {
    private final Context ctx;
    private final PackageManager pm;
    private final AppOpsManager aom;
    private final UsageStatsManager usm;

    private static final String PERM_CAMERA   = "android.permission.CAMERA";
    private static final String PERM_MIC      = "android.permission.RECORD_AUDIO";
    private static final String PERM_LOC_FINE = "android.permission.ACCESS_FINE_LOCATION";
    private static final String PERM_LOC_CRS  = "android.permission.ACCESS_COARSE_LOCATION";
    private static final String PERM_CONTACTS = "android.permission.READ_CONTACTS";
    private static final String PERM_STORAGE  = "android.permission.READ_EXTERNAL_STORAGE";
    private static final String PERM_STORAGE2 = "android.permission.READ_MEDIA_IMAGES";

    public PermissionScanner(Context context) {
        ctx = context;
        pm  = context.getPackageManager();
        aom = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }

    public List<AppInfo> scanAll() {
        List<AppInfo> results = new ArrayList<>();
        List<PackageInfo> packages;
        try {
            packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        } catch (Exception e) { return results; }

        // Usage stats last 7 days
        Map<String, UsageStats> usageMap = new HashMap<>();
        if (usm != null) {
            long end = System.currentTimeMillis();
            long start = end - 7L * 24 * 60 * 60 * 1000;
            Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(start, end);
            if (stats != null) usageMap.putAll(stats);
        }

        for (PackageInfo pkg : packages) {
            if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (pkg.packageName.equals(ctx.getPackageName())) continue;

            AppInfo info = new AppInfo();
            info.packageName = pkg.packageName;
            info.appName     = pm.getApplicationLabel(pkg.applicationInfo).toString();
            info.category    = getCategory(pkg.packageName);
            try { info.icon  = pm.getApplicationIcon(pkg.packageName); } catch (Exception ignored) {}

            // Permissions
            String[] perms = pkg.requestedPermissions;
            if (perms != null) {
                for (String p : perms) {
                    if (p == null) continue;
                    if (p.equals(PERM_CAMERA))             info.hasCamera   = true;
                    else if (p.equals(PERM_MIC))           info.hasMic      = true;
                    else if (p.equals(PERM_LOC_FINE)
                          || p.equals(PERM_LOC_CRS))      info.hasLocation = true;
                    else if (p.equals(PERM_CONTACTS))      info.hasContacts = true;
                    else if (p.equals(PERM_STORAGE)
                          || p.equals(PERM_STORAGE2))      info.hasStorage  = true;
                }
            }

            int uid = pkg.applicationInfo.uid;
            // Background sensor checks (last 10 min = suspicious)
            info.bgCamera   = checkOpRecent(pkg.packageName, uid, AppOpsManager.OPSTR_CAMERA,   10);
            info.bgMic      = checkOpRecent(pkg.packageName, uid, AppOpsManager.OPSTR_RECORD_AUDIO, 10);
            info.bgLocation = checkOpRecent(pkg.packageName, uid, AppOpsManager.OPSTR_FINE_LOCATION, 10);
            info.lastUsedMs = getLastOpTime(pkg.packageName, uid);

            // Weekly usage from UsageStatsManager
            UsageStats us = usageMap.get(pkg.packageName);
            info.usageCount7Days = (us != null) ? (int)(us.getTotalTimeInForeground() / 60000) : 0;

            // Log BG activity
            if (info.bgMic)      ActivityLog.log(ctx, pkg.packageName, info.appName, "Mic",    true);
            if (info.bgCamera)   ActivityLog.log(ctx, pkg.packageName, info.appName, "Camera", true);
            if (info.bgLocation) ActivityLog.log(ctx, pkg.packageName, info.appName, "GPS",    true);

            info.riskScore = AppInfo.computeRisk(info);
            results.add(info);
        }
        results.sort((a, b) -> b.riskScore - a.riskScore);
        return results;
    }

    /** Returns true if op was used within last `minutes` minutes */
    private boolean checkOpRecent(String pkg, int uid, String op, int minutes) {
        try {
            Method getOpsForPackage = aom.getClass().getMethod("getOpsForPackage", int.class, String.class, String[].class);
            List<?> ops = (List<?>) getOpsForPackage.invoke(aom, uid, pkg, new String[]{op});
            if (ops == null) return false;
            for (Object po : ops) {
                Method getOps = po.getClass().getMethod("getOps");
                List<?> entries = (List<?>) getOps.invoke(po);
                for (Object oe : entries) {
                    long t;
                    if (Build.VERSION.SDK_INT >= 29) {
                        Method getLastAccessTime = oe.getClass().getMethod("getLastAccessTime", int.class);
                        // OP_FLAG_SELF = 1, OP_FLAG_TRUSTED_PROXIED = 2
                        t = (long) getLastAccessTime.invoke(oe, 1 | 2);
                    } else {
                        Method getTime = oe.getClass().getMethod("getTime");
                        t = (long) getTime.invoke(oe);
                    }
                    if (System.currentTimeMillis() - t < minutes * 60_000L) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private long getLastOpTime(String pkg, int uid) {
        long latest = 0;
        for (String op : new String[]{AppOpsManager.OPSTR_CAMERA,
                AppOpsManager.OPSTR_RECORD_AUDIO, AppOpsManager.OPSTR_FINE_LOCATION}) {
            try {
                Method getOpsForPackage = aom.getClass().getMethod("getOpsForPackage", int.class, String.class, String[].class);
                List<?> ops = (List<?>) getOpsForPackage.invoke(aom, uid, pkg, new String[]{op});
                if (ops == null) continue;
                for (Object po : ops) {
                    Method getOps = po.getClass().getMethod("getOps");
                    List<?> entries = (List<?>) getOps.invoke(po);
                    for (Object oe : entries) {
                        long t;
                        if (Build.VERSION.SDK_INT >= 29) {
                            Method getLastAccessTime = oe.getClass().getMethod("getLastAccessTime", int.class);
                            t = (long) getLastAccessTime.invoke(oe, 1); // OP_FLAG_SELF = 1
                        } else {
                            Method getTime = oe.getClass().getMethod("getTime");
                            t = (long) getTime.invoke(oe);
                        }
                        if (t > latest) latest = t;
                    }
                }
            } catch (Exception ignored) {}
        }
        return latest;
    }

    private String getCategory(String pkg) {
        String p = pkg.toLowerCase();
        if (p.contains("whatsapp")||p.contains("telegram")||p.contains("instagram")||
            p.contains("facebook")||p.contains("snapchat")||p.contains("twitter")||
            p.contains("tiktok")||p.contains("viber")||p.contains("signal")) return "Social";
        if (p.contains("zoom")||p.contains("gmail")||p.contains("outlook")||
            p.contains("slack")||p.contains("teams")||p.contains("meet"))   return "Productivity";
        if (p.contains("maps")||p.contains("uber")||p.contains("ola")||
            p.contains("rapido")||p.contains("navigation"))                  return "Navigation";
        if (p.contains("spotify")||p.contains("youtube")||p.contains("netflix")||
            p.contains("prime")||p.contains("hotstar")||p.contains("gaana")) return "Entertainment";
        if (p.contains("amazon")||p.contains("flipkart")||p.contains("myntra")||
            p.contains("meesho"))                                             return "Shopping";
        if (p.contains("chrome")||p.contains("firefox")||p.contains("browser")||
            p.contains("opera"))                                              return "Browser";
        if (p.contains("bank")||p.contains("pay")||p.contains("wallet")||
            p.contains("gpay")||p.contains("phonepe"))                       return "Finance";
        return "App";
    }
}
