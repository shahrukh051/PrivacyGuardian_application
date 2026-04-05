package com.privacy.guardian;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
    private List<ActivityLog.LogEntry> list = new ArrayList<>();

    public void setEntries(List<ActivityLog.LogEntry> l) {
        list = l;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ActivityLog.LogEntry e = list.get(pos);
        h.tvApp.setText(e.app);
        h.tvSensor.setText(e.sensor);
        h.tvTime.setText(e.tsStr);
        
        int color;
        if ("Mic".equals(e.sensor)) {
            color = 0xFFEF4444;
        } else if ("Camera".equals(e.sensor)) {
            color = 0xFFF97316;
        } else {
            color = 0xFFEAB308;
        }
        h.tvSensor.setTextColor(color);
        h.tvBg.setVisibility(e.bg ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvApp, tvSensor, tvTime, tvBg;
        VH(View v) {
            super(v);
            tvApp    = v.findViewById(R.id.tv_log_app);
            tvSensor = v.findViewById(R.id.tv_log_sensor);
            tvTime   = v.findViewById(R.id.tv_log_time);
            tvBg     = v.findViewById(R.id.tv_log_bg);
        }
    }
}
