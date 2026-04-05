package com.privacy.guardian;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {
    public interface OnClick { void on(AppInfo app); }
    private List<AppInfo> list = new ArrayList<>();
    private final OnClick listener;

    public AppAdapter(OnClick l) { listener = l; }

    public void setApps(List<AppInfo> l) {
        list = l;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_app, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AppInfo a = list.get(pos);
        Context ctx = h.itemView.getContext();
        h.tvName.setText(a.appName);

        if (a.lastUsedMs > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String time = sdf.format(new Date(a.lastUsedMs));
            h.tvCat.setText(ctx.getString(R.string.category_time_format, a.category, time));
        } else {
            h.tvCat.setText(a.category);
        }

        if (a.icon != null) h.ivIcon.setImageDrawable(a.icon);
        else h.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon);

        int color = a.getRiskColor();
        h.tvScore.setText(String.valueOf(a.riskScore));
        h.tvScore.setTextColor(color);
        h.tvLabel.setText(a.getRiskLabelRes());
        h.tvLabel.setTextColor(color);

        // Permission chips
        h.llChips.removeAllViews();
        addChip(ctx, h.llChips, ctx.getString(R.string.label_cam),  a.hasCamera,   a.bgCamera,   0xFFF97316);
        addChip(ctx, h.llChips, ctx.getString(R.string.label_mic),  a.hasMic,      a.bgMic,      0xFFEF4444);
        addChip(ctx, h.llChips, ctx.getString(R.string.label_gps),  a.hasLocation, a.bgLocation, 0xFFEAB308);
        addChip(ctx, h.llChips, ctx.getString(R.string.label_contacts), a.hasContacts, false,        0xFF8B5CF6);
        addChip(ctx, h.llChips, ctx.getString(R.string.label_storage), a.hasStorage,  false,        0xFF06B6D4);

        if (a.hasBackgroundActivity()) {
            h.tvBg.setVisibility(View.VISIBLE);
        } else {
            h.tvBg.setVisibility(View.GONE);
        }

        if      (a.riskScore >= 80) h.card.setCardBackgroundColor(0xFF180808);
        else if (a.riskScore >= 60) h.card.setCardBackgroundColor(0xFF181208);
        else                        h.card.setCardBackgroundColor(0xFF0D1117);

        h.itemView.setOnClickListener(v -> listener.on(a));
    }

    private void addChip(Context ctx, LinearLayout p, String label,
                         boolean granted, boolean bg, int color) {
        if (!granted) return;
        TextView tv = new TextView(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(5);
        tv.setLayoutParams(lp);
        String bullet = ctx.getString(R.string.bg_indicator_bullet);
        tv.setText(bg ? label + bullet : label);
        tv.setTextSize(9.5f);
        tv.setTextColor(color);
        tv.setPadding(10, 3, 10, 3);
        tv.setBackgroundResource(R.drawable.bg_chip);
        p.addView(tv);
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        CardView card; ImageView ivIcon;
        TextView tvName, tvCat, tvScore, tvLabel, tvBg;
        LinearLayout llChips;
        VH(View v) {
            super(v);
            card    = v.findViewById(R.id.card);
            ivIcon  = v.findViewById(R.id.iv_icon);
            tvName  = v.findViewById(R.id.tv_name);
            tvCat   = v.findViewById(R.id.tv_cat);
            tvScore = v.findViewById(R.id.tv_score);
            tvLabel = v.findViewById(R.id.tv_label);
            tvBg    = v.findViewById(R.id.tv_bg);
            llChips = v.findViewById(R.id.ll_chips);
        }
    }
}
