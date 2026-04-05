package com.privacy.guardian;

import java.util.Date;

/**
 * Model: one installed app + its permission/risk/usage data.
 * Risk Score Formula (0–100):
 *   Base:  Camera+15, Mic+20, GPS+15, Contacts+10, Storage+5
 *   BG:    bgMic+25,  bgCam+20,  bgGPS+15
 *   Category multiplier: Social/Entertainment × 1.15
 *   Frequency bonus: if used >5× in last 7 days, +10
 */
public class AppInfo {
    public String  packageName;
    public String  appName;
    public String  category;
    public android.graphics.drawable.Drawable icon;

    // Declared permissions
    public boolean hasCamera, hasMic, hasLocation, hasContacts, hasStorage;

    // Background access (AppOpsManager, last 10 min)
    public boolean bgCamera, bgMic, bgLocation;

    // Usage stats
    public int     usageCount7Days;   // how many times used last 7 days
    public long    lastUsedMs;        // epoch ms

    // Computed
    public int     riskScore;         // 0-100

    // Activity log entries (last 5)
    public java.util.List<String> recentActivity = new java.util.ArrayList<>();

    public static int computeRisk(AppInfo a) {
        int s = 0;
        if (a.hasCamera)   s += 15;
        if (a.hasMic)      s += 20;
        if (a.hasLocation) s += 15;
        if (a.hasContacts) s += 10;
        if (a.hasStorage)  s +=  5;
        if (a.bgCamera)    s += 20;
        if (a.bgMic)       s += 25;
        if (a.bgLocation)  s += 15;
        if ("Social".equals(a.category) || "Entertainment".equals(a.category))
            s = (int)(s * 1.15);
        if (a.usageCount7Days > 5) s += 10;
        return Math.min(s, 100);
    }

    public int getRiskLabelRes() {
        if (riskScore >= 80) return R.string.risk_critical;
        if (riskScore >= 60) return R.string.risk_high;
        if (riskScore >= 40) return R.string.risk_medium;
        return R.string.risk_low;
    }

    public int getRiskColor() {
        if (riskScore >= 80) return 0xFFEF4444;
        if (riskScore >= 60) return 0xFFF97316;
        if (riskScore >= 40) return 0xFFEAB308;
        return 0xFF22C55E;
    }

    public boolean hasBackgroundActivity() {
        return bgCamera || bgMic || bgLocation;
    }

    public int sensitivePermCount() {
        int c = 0;
        if (hasCamera) c++; if (hasMic) c++;
        if (hasLocation) c++; if (hasContacts) c++;
        return c;
    }
}
