package com.privacy.guardian;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Per-app detail screen:
 *   - Risk score + label
 *   - Full permissions audit table
 *   - Context-aware recommendations (action-oriented)
 *   - Direct link to Android App Settings for permission management
 */
public class AppDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent in = getIntent();
        String pkg      = in.getStringExtra("pkg");
        String name     = in.getStringExtra("name");
        String category = in.getStringExtra("category");
        int    risk     = in.getIntExtra("risk", 0);
        boolean hasCamera   = in.getBooleanExtra("hasCamera",   false);
        boolean hasMic      = in.getBooleanExtra("hasMic",      false);
        boolean hasLocation = in.getBooleanExtra("hasLocation", false);
        boolean hasContacts = in.getBooleanExtra("hasContacts", false);
        boolean hasStorage  = in.getBooleanExtra("hasStorage",  false);
        boolean bgCamera    = in.getBooleanExtra("bgCamera",    false);
        boolean bgMic       = in.getBooleanExtra("bgMic",       false);
        boolean bgLocation  = in.getBooleanExtra("bgLocation",  false);
        int     usage7      = in.getIntExtra("usage7", 0);

        // App icon
        try {
            ((ImageView) findViewById(R.id.iv_icon))
                .setImageDrawable(getPackageManager().getApplicationIcon(pkg));
        } catch (Exception ignored) {}

        setText(R.id.tv_name, name);
        setText(R.id.tv_pkg,  pkg);
        setText(R.id.tv_cat,  getString(R.string.usage_week_format, category, usage7));

        // Risk score
        AppInfo tmp = new AppInfo(); tmp.riskScore = risk;
        TextView tvRisk = findViewById(R.id.tv_risk);
        tvRisk.setText(getString(R.string.risk_score_format, risk, getString(tmp.getRiskLabelRes())));
        tvRisk.setTextColor(tmp.getRiskColor());

        // Risk description
        int descRes;
        if      (risk >= 80) descRes = R.string.risk_desc_critical;
        else if (risk >= 60) descRes = R.string.risk_desc_high;
        else if (risk >= 40) descRes = R.string.risk_desc_medium;
        else                 descRes = R.string.risk_desc_low;
        setText(R.id.tv_risk_desc, getString(descRes));

        // Permissions table
        bindRow(R.id.tv_camera_status,   hasCamera,   bgCamera,   getString(R.string.perm_camera));
        bindRow(R.id.tv_mic_status,      hasMic,      bgMic,      getString(R.string.perm_mic));
        bindRow(R.id.tv_location_status, hasLocation, bgLocation, getString(R.string.perm_gps));
        bindRow(R.id.tv_contacts_status, hasContacts, false,      getString(R.string.perm_contacts));
        bindRow(R.id.tv_storage_status,  hasStorage,  false,      getString(R.string.perm_storage));

        // Action-oriented recommendations
        StringBuilder rec = new StringBuilder();
        if (bgMic)      rec.append(getString(R.string.rec_mic_bg)).append("\n\n");
        if (bgCamera)   rec.append(getString(R.string.rec_cam_bg)).append("\n\n");
        if (bgLocation) rec.append(getString(R.string.rec_gps_bg)).append("\n\n");
        if (hasContacts && ("Social".equals(category)||"Entertainment".equals(category)))
                        rec.append(getString(R.string.rec_contacts_social)).append("\n\n");
        if (hasCamera && hasMic && hasLocation && hasContacts)
                        rec.append(getString(R.string.rec_all_sensitive)).append("\n\n");
        if (usage7 > 100) rec.append(getString(R.string.rec_high_usage, usage7)).append("\n\n");
        if (rec.length() == 0) rec.append(getString(R.string.rec_safe)).append("\n");
        setText(R.id.tv_recs, rec.toString().trim());

        // Buttons
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.fromParts("package", pkg, null));
            startActivity(i);
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(name);
            getSupportActionBar().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(0xFF060A14));
        }
    }

    private void setText(int id, String s) { ((TextView) findViewById(id)).setText(s); }

    private void bindRow(int tvId, boolean granted, boolean bg, String label) {
        TextView tv = findViewById(tvId);
        if (tv == null) return;
        if (granted && bg)   { tv.setText(R.string.status_granted_bg); tv.setTextColor(0xFFEF4444); }
        else if (granted)    { tv.setText(R.string.status_granted);    tv.setTextColor(0xFFF97316); }
        else                 { tv.setText(R.string.status_not_granted); tv.setTextColor(0xFF22C55E); }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
