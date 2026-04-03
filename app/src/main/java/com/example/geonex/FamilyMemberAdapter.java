package com.example.geonex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class FamilyMemberAdapter extends RecyclerView.Adapter<FamilyMemberAdapter.ViewHolder> {

    private List<FamilyMember> members;
    private OnFamilyMemberClickListener listener;

    public interface OnFamilyMemberClickListener {
        void onMemberClick(FamilyMember member, int position);
        void onMemberLongClick(FamilyMember member, int position);
        void onAssignReminderClick(FamilyMember member, int position);
    }

    public FamilyMemberAdapter(List<FamilyMember> members, OnFamilyMemberClickListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_family_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FamilyMember member = members.get(position);

        holder.tvName.setText(member.getName());
        holder.tvRelation.setText(member.getRelation());
        holder.tvRole.setText(member.getRole());
        holder.tvReminderCount.setText(member.getReminderCount() + " reminders");

        // Set online status
        if (member.isOnline()) {
            holder.onlineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.onlineIndicator.setVisibility(View.GONE);
        }

        // Set avatar (using first letter if no image)
        holder.tvAvatarInitial.setText(member.getInitials());

        // If you have actual images, use Glide or Picasso
        // Glide.with(holder.itemView.getContext()).load(member.getAvatarUrl()).into(holder.ivAvatar);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemberClick(member, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onMemberLongClick(member, position);
                return true;
            }
            return false;
        });

        holder.btnAssign.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAssignReminderClick(member, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardMember;
        ImageView ivAvatar;
        TextView tvAvatarInitial;
        TextView tvName, tvRelation, tvRole, tvReminderCount;
        View onlineIndicator;
        View btnAssign;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMember = itemView.findViewById(R.id.cardMember);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            tvName = itemView.findViewById(R.id.tvName);
            tvRelation = itemView.findViewById(R.id.tvRelation);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvReminderCount = itemView.findViewById(R.id.tvReminderCount);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            btnAssign = itemView.findViewById(R.id.btnAssign);
        }
    }
}