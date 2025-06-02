package com.ugb.cuadrasmart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SupervisorAdapter extends RecyclerView.Adapter<SupervisorAdapter.SupervisorViewHolder> {

    private List<Supervisor> supervisorList;
    private OnSupervisorClickListener listener;

    public interface OnSupervisorClickListener {
        void onSupervisorClick(Supervisor supervisor);
    }

    public SupervisorAdapter(List<Supervisor> supervisorList, OnSupervisorClickListener listener) {
        this.supervisorList = (supervisorList != null) ? supervisorList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public SupervisorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_supervisor_selectable, parent, false);
        return new SupervisorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SupervisorViewHolder holder, int position) {
        Supervisor supervisor = supervisorList.get(position);
        holder.tvName.setText(supervisor.getName());
        holder.tvEmail.setText(supervisor.getEmail());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSupervisorClick(supervisor);
            }
        });
    }

    @Override
    public int getItemCount() {
        return supervisorList.size();
    }

    public void updateData(List<Supervisor> newSupervisors) {
        this.supervisorList.clear();
        if (newSupervisors != null) {
            this.supervisorList.addAll(newSupervisors);
        }
        notifyDataSetChanged();
    }

    static class SupervisorViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;

        public SupervisorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSupervisorNameSelectable);
            tvEmail = itemView.findViewById(R.id.tvSupervisorEmailSelectable);
        }
    }
}