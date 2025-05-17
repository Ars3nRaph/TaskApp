package com.example.taskapp.sync;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.example.taskapp.R;
import com.example.taskapp.adapters.TaskAdapter;
import com.example.taskapp.api.ApiClient;
import com.example.taskapp.models.Task;
import com.example.taskapp.models.User;

import java.util.ArrayList;
import java.util.List;

public class AllTasksActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener, TaskAdapter.OnTaskCompletedListener {

    private Spinner groupFilterSpinner;
    private RecyclerView allTasksRecyclerView;
    private TextView syncStatusTextView;
    private Button goToPage1Button;

    private ApiClient apiClient;
    private TaskAdapter taskAdapter;
    private List<Task> allTasks;
    private List<String> availableGroups;
    private Handler syncHandler;
    private Runnable syncRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_tasks);

        // Initialisation des vues
        groupFilterSpinner = findViewById(R.id.spinner_group_filter_all);
        allTasksRecyclerView = findViewById(R.id.recycler_all_tasks);
        syncStatusTextView = findViewById(R.id.text_sync_status);
        goToPage1Button = findViewById(R.id.btn_go_to_page1);
        Button btnSettings = findViewById(R.id.btn_open_settings);
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(AllTasksActivity.this, SettingsActivity.class));
        });

        // Initialisation de l'API client
        apiClient = new ApiClient(this);

        // Initialisation de la liste des tâches
        allTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, allTasks, this, this, true);
        allTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        allTasksRecyclerView.setAdapter(taskAdapter);

        // Chargement des groupes disponibles
        loadGroups();

        // Chargement de toutes les tâches
        loadAllTasks();

        // Configuration des listeners
        setupListeners();

        // Configuration de la synchronisation périodique
        setupSyncHandler();
    }

    private void loadGroups() {
        apiClient.getGroups(new ApiClient.GroupsCallback() {
            @Override
            public void onSuccess(List<String> groups) {
                availableGroups = groups;
                setupGroupSpinner();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AllTasksActivity.this, "Erreur lors du chargement des groupes: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupGroupSpinner() {
        // Ajouter une option "Tous les groupes" au début
        List<String> spinnerGroups = new ArrayList<>();
        spinnerGroups.add("Tous les groupes");
        spinnerGroups.addAll(availableGroups);

        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerGroups);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupFilterSpinner.setAdapter(groupAdapter);
    }

    private void loadAllTasks() {
        syncStatusTextView.setText(R.string.sync_in_progress);
        
        apiClient.getTasks(new ApiClient.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                allTasks.clear();
                allTasks.addAll(tasks);
                filterTasks();
                syncStatusTextView.setText(R.string.sync_completed);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AllTasksActivity.this, "Erreur lors du chargement des tâches: " + error, Toast.LENGTH_SHORT).show();
                syncStatusTextView.setText(R.string.error_loading_tasks);
            }
        });
    }

    private void setupListeners() {
        goToPage1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Retour à la page 1 (MainActivity)
            }
        });

        groupFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterTasks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ne rien faire
            }
        });
    }

    private void setupSyncHandler() {
        syncHandler = new Handler(Looper.getMainLooper());
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                loadAllTasks();
                // Planifier la prochaine synchronisation dans 10 secondes
                syncHandler.postDelayed(this, 5000);
            }
        };
        // Démarrer la synchronisation périodique
        syncHandler.postDelayed(syncRunnable, 5000);
    }

    private void filterTasks() {
        if (allTasks.isEmpty()) {
            taskAdapter.updateTasks(new ArrayList<>());
            return;
        }

        List<Task> filteredTasks = new ArrayList<>(allTasks);
        
        // Filtre par groupe
        int groupPosition = groupFilterSpinner.getSelectedItemPosition();
        if (groupPosition > 0 && availableGroups != null && !availableGroups.isEmpty()) {
            String selectedGroup = availableGroups.get(groupPosition - 1);
            List<Task> groupFiltered = new ArrayList<>();
            for (Task task : filteredTasks) {
                if (task.getGroupName().equals(selectedGroup)) {
                    groupFiltered.add(task);
                }
            }
            filteredTasks = groupFiltered;
        }
        
        taskAdapter.updateTasks(filteredTasks);
    }

    @Override
    public void onTaskClick(Task task, int position) {
        // Dans cette vue, on ne permet pas de modifier les tâches
        // On affiche juste les détails
        Toast.makeText(this, "Tâche créée par: " + task.getUsername(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskCompleted(Task task, int position, boolean isCompleted) {
        apiClient.updateTask(task, new ApiClient.TaskCallback() {
            @Override
            public void onSuccess(Task updatedTask) {
                // Si la tâche est terminée, la supprimer côté serveur
                apiClient.deleteTask(task.getId(), new ApiClient.TaskCallback() {
                    @Override
                    public void onSuccess(Task deletedTask) {
                        Toast.makeText(AllTasksActivity.this, "Tâche terminée et supprimée", Toast.LENGTH_SHORT).show();
                        allTasks.remove(task);
                        filterTasks(); // mettre à jour l'affichage
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(AllTasksActivity.this, "Erreur lors de la suppression : " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AllTasksActivity.this, "Erreur lors de la mise à jour : " + error, Toast.LENGTH_SHORT).show();
                task.setCompleted(!isCompleted);
                taskAdapter.notifyItemChanged(position);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Arrêter la synchronisation périodique
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
    }
    public void showUserReassignDialog(Task task, int position) {
        apiClient.getUsers(new ApiClient.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                String[] usernames = new String[users.size()];
                for (int i = 0; i < users.size(); i++) {
                    usernames[i] = users.get(i).getUsername();
                }

                new AlertDialog.Builder(AllTasksActivity.this)
                        .setTitle("Réassigner à :")
                        .setItems(usernames, (dialog, which) -> {
                            User selectedUser = users.get(which);
                            task.setUserId(selectedUser.getId());
                            task.setUsername(selectedUser.getUsername());
                            task.updateLastModified();

                            apiClient.updateTask(task, new ApiClient.TaskCallback() {
                                @Override
                                public void onSuccess(Task updatedTask) {
                                    Toast.makeText(AllTasksActivity.this, "Tâche réassignée à " + selectedUser.getUsername(), Toast.LENGTH_SHORT).show();
                                    allTasks.set(position, updatedTask);
                                    taskAdapter.notifyItemChanged(position);
                                    loadAllTasks();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(AllTasksActivity.this, "Erreur : " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AllTasksActivity.this, "Impossible de charger les utilisateurs : " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
