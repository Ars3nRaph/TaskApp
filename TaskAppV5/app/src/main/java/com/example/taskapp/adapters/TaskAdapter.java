package com.example.taskapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskapp.R;
import com.example.taskapp.models.Task;
import com.example.taskapp.sync.AllTasksActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private Context context;
    private OnTaskClickListener taskClickListener;
    private OnTaskCompletedListener taskCompletedListener;
    private SimpleDateFormat dateFormat;
    private boolean showDeleteOnlyIfCompleted = false;

    public interface OnTaskClickListener {
        void onTaskClick(Task task, int position);
    }

    public interface OnTaskCompletedListener {
        void onTaskCompleted(Task task, int position, boolean isCompleted);
    }

    // Constructeur standard
    public TaskAdapter(Context context, List<Task> tasks, OnTaskClickListener clickListener,
                       OnTaskCompletedListener completedListener) {
        this(context, tasks, clickListener, completedListener, false);
    }

    // Constructeur avec mode suppression activé
    public TaskAdapter(Context context, List<Task> tasks, OnTaskClickListener clickListener,
                       OnTaskCompletedListener completedListener, boolean showDeleteOnlyIfCompleted) {
        this.context = context;
        this.tasks = tasks;
        this.taskClickListener = clickListener;
        this.taskCompletedListener = completedListener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.showDeleteOnlyIfCompleted = showDeleteOnlyIfCompleted;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.titleTextView.setText(task.getTitle());
        holder.descriptionTextView.setText(task.getDescription());
        holder.groupTextView.setText(task.getGroupName());
        holder.dueDateTextView.setText(dateFormat.format(task.getDueDate()));
        holder.completedCheckBox.setChecked(task.isCompleted());
        holder.creatorTextView.setText(task.getUsername());
        holder.creatorTextView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task selectedTask = tasks.get(adapterPosition);

                // Vérifie que le context est bien une activité avec l'API
                if (context instanceof AllTasksActivity) {
                    ((AllTasksActivity) context).showUserReassignDialog(selectedTask, adapterPosition);
                }
            }
        });

        // Couleur de priorité (et bande gauche)
        int priorityColor;
        switch (task.getPriority()) {
            case 1:
                priorityColor = ContextCompat.getColor(context, R.color.priority_1);
                break;
            case 2:
                priorityColor = ContextCompat.getColor(context, R.color.priority_2);
                break;
            case 3:
                priorityColor = ContextCompat.getColor(context, R.color.priority_3);
                break;
            default:
                priorityColor = Color.GRAY;
        }

        if (holder.priorityView != null) {
            holder.priorityView.setBackgroundColor(priorityColor);
        }

        // Affichage conditionnel de la case à cocher
        if (showDeleteOnlyIfCompleted) {
            if (task.isCompleted()) {
                holder.completedCheckBox.setVisibility(View.VISIBLE);
                holder.completedCheckBox.setText("Supprimer");
            } else {
                holder.completedCheckBox.setVisibility(View.GONE);
            }
        } else {
            holder.completedCheckBox.setVisibility(View.VISIBLE);
            holder.completedCheckBox.setText("Terminée");
        }

        // Click sur la tâche (affichage d’infos)
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                taskClickListener.onTaskClick(tasks.get(adapterPosition), adapterPosition);
            }
        });

        // Click sur la case à cocher
        holder.completedCheckBox.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task t = tasks.get(adapterPosition);

                if (showDeleteOnlyIfCompleted) {
                    // Confirmation suppression
                    new android.app.AlertDialog.Builder(context)
                            .setTitle("Confirmer la suppression")
                            .setMessage("Voulez-vous vraiment supprimer cette tâche ?")
                            .setPositiveButton("Oui", (dialog, which) -> {
                                taskCompletedListener.onTaskCompleted(t, adapterPosition, true);
                            })
                            .setNegativeButton("Annuler", (dialog, which) -> {
                                holder.completedCheckBox.setChecked(true);
                            })
                            .setOnCancelListener(dialog -> {
                                holder.completedCheckBox.setChecked(true);
                            })
                            .show();
                } else {
                    boolean isChecked = holder.completedCheckBox.isChecked();
                    t.setCompleted(isChecked);
                    taskCompletedListener.onTaskCompleted(t, adapterPosition, isChecked);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView groupTextView;
        TextView dueDateTextView;
        TextView creatorTextView;
        View priorityView;
        CheckBox completedCheckBox;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_task_title);
            descriptionTextView = itemView.findViewById(R.id.text_task_description);
            groupTextView = itemView.findViewById(R.id.text_task_group);
            dueDateTextView = itemView.findViewById(R.id.text_task_due_date);
            completedCheckBox = itemView.findViewById(R.id.checkbox_task_completed);
            creatorTextView = itemView.findViewById(R.id.text_task_creator);
            priorityView = itemView.findViewById(R.id.view_priority);
        }
    }
}
