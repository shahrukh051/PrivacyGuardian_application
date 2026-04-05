package com.privacy.guardian;

import android.app.AppOpsManager;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import java.util.*;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    // Views — Dashboard
    private View    vDashboard, vApps, vLogs;
    private TextView tvHigh, tvBg, tvAvg, tvTotal;
    private TextView tvCamCount, tvMicCount, tvGpsCount;
    private ProgressBar pbCam, pbMic, pbGps;
    private TextView tvTrendCam, tvTrendMic, tvTrendGps;
    private RecyclerView rvBgApps;
    private AppAdapter bgAdapter;

    // Views — Apps tab
    private RecyclerView rvApps;
    private AppAdapter   appAdapter;
    private TabLayout    tabFilter;
    private TextView     tvStats, tvEmpty;
    private ProgressBar  pbScan;
    private Button       btnScan;

    // Views — Logs tab
    private RecyclerView rvLogs;
    private LogAdapter   logAdapter;

    private List<AppInfo> allApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupBottomNav();
        setupFilterTabs();
        showTab(vDashboard);

        if (!hasUsagePermission()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.usage_access_title)
                .setMessage(R.string.usage_access_msg)
                .setPositiveButton(R.string.open_settings, (d, w) ->
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                .setNegativeButton(R.string.btn_skip, (d, w) -> startScan())
                .show();
        } else {
            startScan();
            startService(new Intent(this, SensorMonitorService.class));
        }
    }

    private void bindViews() {
        vDashboard = findViewById(R.id.v_dashboard);
        vApps      = findViewById(R.id.v_apps);
        vLogs      = findViewById(R.id.v_logs);

        tvHigh     = findViewById(R.id.tv_high);
        tvBg       = findViewById(R.id.tv_bg);
        tvAvg      = findViewById(R.id.tv_avg);
        tvTotal    = findViewById(R.id.tv_total);
        tvCamCount = findViewById(R.id.tv_cam_count);
        tvMicCount = findViewById(R.id.tv_mic_count);
        tvGpsCount = findViewById(R.id.tv_gps_count);
        pbCam      = findViewById(R.id.pb_cam);
        pbMic      = findViewById(R.id.pb_mic);
        pbGps      = findViewById(R.id.pb_gps);
        tvTrendCam = findViewById(R.id.tv_trend_cam);
        tvTrendMic = findViewById(R.id.tv_trend_mic);
        tvTrendGps = findViewById(R.id.tv_trend_gps);

        rvBgApps = findViewById(R.id.rv_bg_apps);
        bgAdapter = new AppAdapter(this::openDetail);
        rvBgApps.setLayoutManager(new LinearLayoutManager(this));
        rvBgApps.setAdapter(bgAdapter);
        rvBgApps.setNestedScrollingEnabled(false);

        pbScan    = findViewById(R.id.pb_scan);
        tvStats   = findViewById(R.id.tv_stats);
        tvEmpty   = findViewById(R.id.tv_empty);
        btnScan   = findViewById(R.id.btn_scan);
        tabFilter = findViewById(R.id.tab_filter);

        rvApps    = findViewById(R.id.rv_apps);
        appAdapter = new AppAdapter(this::openDetail);
        rvApps.setLayoutManager(new LinearLayoutManager(this));
        rvApps.setAdapter(appAdapter);
        rvApps.setNestedScrollingEnabled(false);

        rvLogs    = findViewById(R.id.rv_logs);
        logAdapter = new LogAdapter();
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        rvLogs.setAdapter(logAdapter);
        rvLogs.setNestedScrollingEnabled(false);

        Button btnClearLog = findViewById(R.id.btn_clear_log);
        btnClearLog.setOnClickListener(v -> {
            ActivityLog.clear(this);
            logAdapter.setEntries(new ArrayList<>());
            findViewById(R.id.tv_no_logs).setVisibility(View.VISIBLE);
        });
        btnScan.setOnClickListener(v -> startScan());
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_dashboard) { showTab(vDashboard); return true; }
            else if (id == R.id.nav_apps)      { showTab(vApps);      applyFilter(tabFilter.getSelectedTabPosition()); return true; }
            else if (id == R.id.nav_logs)      { showTab(vLogs);      loadLogs(); return true; }
            return false;
        });
    }

    private void showTab(View active) {
        for (View v : new View[]{vDashboard, vApps, vLogs}) v.setVisibility(View.GONE);
        active.setVisibility(View.VISIBLE);
    }

    private void setupFilterTabs() {
        tabFilter.addTab(tabFilter.newTab().setText(R.string.tab_all));
        tabFilter.addTab(tabFilter.newTab().setText(R.string.tab_critical));
        tabFilter.addTab(tabFilter.newTab().setText(R.string.tab_background));
        tabFilter.addTab(tabFilter.newTab().setText(R.string.tab_camera));
        tabFilter.addTab(tabFilter.newTab().setText(R.string.tab_mic));
        tabFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t)   { applyFilter(t.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
    }

    private void applyFilter(int pos) {
        List<AppInfo> f;
        switch (pos) {
            case 1: f = allApps.stream().filter(a -> a.riskScore >= 80).collect(Collectors.toList()); break;
            case 2: f = allApps.stream().filter(AppInfo::hasBackgroundActivity).collect(Collectors.toList()); break;
            case 3: f = allApps.stream().filter(a -> a.hasCamera).collect(Collectors.toList()); break;
            case 4: f = allApps.stream().filter(a -> a.hasMic).collect(Collectors.toList()); break;
            default: f = allApps;
        }
        appAdapter.setApps(f);
        tvStats.setText(getString(R.string.stats_format, f.size()));
        tvEmpty.setVisibility(f.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("deprecation")
    private void startScan() {
        pbScan.setVisibility(View.VISIBLE);
        btnScan.setEnabled(false);
        new android.os.AsyncTask<Void, Void, List<AppInfo>>() {
            @Override protected List<AppInfo> doInBackground(Void... v) {
                return new PermissionScanner(MainActivity.this).scanAll();
            }
            @Override protected void onPostExecute(List<AppInfo> r) {
                allApps = r;
                updateDashboard(r);
                appAdapter.setApps(r);
                pbScan.setVisibility(View.GONE);
                btnScan.setEnabled(true);
                tvStats.setText(getString(R.string.stats_scanned_format, r.size()));
                tvEmpty.setVisibility(r.isEmpty() ? View.VISIBLE : View.GONE);
                // Alert on high-risk BG apps
                for (AppInfo a : r)
                    if (a.hasBackgroundActivity() && a.riskScore >= 60)
                        NotificationHelper.sendSensorAlert(MainActivity.this, a);
            }
        }.execute();
    }

    private void updateDashboard(List<AppInfo> apps) {
        long high = apps.stream().filter(a -> a.riskScore >= 70).count();
        long bg   = apps.stream().filter(AppInfo::hasBackgroundActivity).count();
        int  avg  = apps.isEmpty() ? 0 : (int) apps.stream().mapToInt(a -> a.riskScore).average().orElse(0);
        long cam  = apps.stream().filter(a -> a.hasCamera).count();
        long mic  = apps.stream().filter(a -> a.hasMic).count();
        long gps  = apps.stream().filter(a -> a.hasLocation).count();
        int  tot  = apps.size();

        tvHigh.setText(String.valueOf(high));
        tvBg.setText(String.valueOf(bg));
        tvAvg.setText(String.valueOf(avg));
        tvTotal.setText(String.valueOf(tot));
        tvCamCount.setText(getString(R.string.stats_format, (int)cam));
        tvMicCount.setText(getString(R.string.stats_format, (int)mic));
        tvGpsCount.setText(getString(R.string.stats_format, (int)gps));
        if (tot > 0) {
            pbCam.setProgress((int)(cam * 100 / tot));
            pbMic.setProgress((int)(mic * 100 / tot));
            pbGps.setProgress((int)(gps * 100 / tot));
        }

        // Weekly trend from ActivityLog
        Map<String, Integer> trend = ActivityLog.weeklyTrend(this);
        tvTrendCam.setText(getString(R.string.trend_format, trend.getOrDefault("Camera", 0)));
        tvTrendMic.setText(getString(R.string.trend_format, trend.getOrDefault("Mic",    0)));
        tvTrendGps.setText(getString(R.string.trend_format, trend.getOrDefault("GPS",    0)));

        // BG apps list on dashboard
        List<AppInfo> bgApps = apps.stream().filter(AppInfo::hasBackgroundActivity).collect(Collectors.toList());
        bgAdapter.setApps(bgApps);
        findViewById(R.id.tv_no_bg).setVisibility(bgApps.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadLogs() {
        List<ActivityLog.LogEntry> entries = ActivityLog.getAll(this);
        logAdapter.setEntries(entries);
        findViewById(R.id.tv_no_logs).setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openDetail(AppInfo a) {
        Intent i = new Intent(this, AppDetailActivity.class);
        i.putExtra("pkg",        a.packageName); i.putExtra("name",     a.appName);
        i.putExtra("category",   a.category);    i.putExtra("risk",     a.riskScore);
        i.putExtra("hasCamera",  a.hasCamera);   i.putExtra("hasMic",   a.hasMic);
        i.putExtra("hasLocation",a.hasLocation); i.putExtra("hasContacts",a.hasContacts);
        i.putExtra("hasStorage", a.hasStorage);  i.putExtra("bgCamera", a.bgCamera);
        i.putExtra("bgMic",      a.bgMic);       i.putExtra("bgLocation",a.bgLocation);
        i.putExtra("usage7",     a.usageCount7Days);
        startActivity(i);
    }

    private boolean hasUsagePermission() {
        AppOpsManager aom = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
