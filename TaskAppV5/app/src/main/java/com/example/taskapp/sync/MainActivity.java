package com.example.taskapp.sync;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskapp.R;
import com.example.taskapp.adapters.TaskAdapter;
import com.example.taskapp.api.ApiClient;
import com.example.taskapp.models.Task;
import com.example.taskapp.models.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.view.ViewGroup;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener, TaskAdapter.OnTaskCompletedListener {

    private static final String PREFS_NAME = "TaskAppPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";

    private TextView usernameTextView;
    private Button validateUserButton;
    private Button createTaskButton;
    private Spinner groupFilterSpinner;
    private Spinner priorityFilterSpinner;
    private RecyclerView userTasksRecyclerView;
    private Button goToPage2Button;

    private ApiClient apiClient;
    private TaskAdapter taskAdapter;
    private List<Task> userTasks;
    private List<String> availableGroups;
    private User currentUser;
    private Handler syncHandler;
    private Runnable syncRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        usernameTextView = findViewById(R.id.text_username); // nouveau TextView non modifiable
        createTaskButton = findViewById(R.id.btn_create_task);
        groupFilterSpinner = findViewById(R.id.spinner_group_filter);
        priorityFilterSpinner = findViewById(R.id.spinner_priority_filter);
        userTasksRecyclerView = findViewById(R.id.recycler_user_tasks);
        goToPage2Button = findViewById(R.id.btn_go_to_page2);

        // Initialisation de l'API client
        apiClient = new ApiClient(this);

        // Initialisation de la liste des tâches
        userTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, userTasks, this, this);
        userTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userTasksRecyclerView.setAdapter(taskAdapter);

        // Chargement des groupes disponibles
        loadGroups();

        // Configuration des filtres
        setupFilters();

        // Vérification si l'utilisateur est déjà identifié
        checkUserLogin();

        // Configuration des listeners
        setupListeners();

        // Configuration de la synchronisation périodique
        setupSyncHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger l'utilisateur depuis les SharedPreferences
        SharedPreferences prefs = getSharedPreferences("TaskAppPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        String username = prefs.getString("username", "");

        if (userId != -1 && !username.isEmpty()) {
            currentUser = new User(userId, username); // ➔ Met à jour currentUser
            usernameTextView.setText(username);       // ➔ Met à jour l'affichage du nom

            loadUserTasks();                          // ➔ Recharge les tâches du nouvel utilisateur
        }
        checkUserLogin(); // Recharge currentUser avec les prefs mises à jour
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            String newUsername = data.getStringExtra("new_username");
            if (newUsername != null && !newUsername.isEmpty()) {
                loginUser(newUsername); // crée un nouvel utilisateur sur le serveur
                usernameTextView.setText(newUsername); // affiche le nouveau nom
            }
        }
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
                if (error != null && !error.contains("null")) {
                    Log.w("API", "Erreur lors du chargement des groupes: " + error);
                }
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

    private void setupFilters() {
        // Configuration du filtre de priorité
        String[] priorities = {"Toutes les priorités", "Priorité 1 (Faible)", "Priorité 2 (Moyenne)", "Priorité 3 (Haute)"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priorityFilterSpinner.setAdapter(priorityAdapter);
    }

    private void checkUserLogin() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int userId = prefs.getInt(KEY_USER_ID, -1);
        String username = prefs.getString(KEY_USERNAME, "");

        if (userId != -1 && !username.isEmpty()) {
            currentUser = new User(userId, username);
            usernameTextView.setText(username); // ✅ CORRIGÉ ICI
            loadUserTasks();
        }
    }

    private void setupListeners() {

        goToPage2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AllTasksActivity.class);
                startActivity(intent);
            }
        });

        createTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DEBUG", "Créer une tâche cliqué"); // Log pour vérif
                if (currentUser != null) {
                    showTaskDialog(null);
                } else {
                    Toast.makeText(MainActivity.this, "Veuillez vous identifier d'abord", Toast.LENGTH_SHORT).show();
                }
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

        priorityFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                if (currentUser != null) {
                    loadUserTasks();
                }
                // Planifier la prochaine synchronisation dans 30 secondes
                syncHandler.postDelayed(this, 30000);
            }
        };
        // Démarrer la synchronisation périodique
        syncHandler.postDelayed(syncRunnable, 30000);
    }

    private void loginUser(String username) {
        apiClient.createUser(username, new ApiClient.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                
                // Sauvegarder les informations de l'utilisateur
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(KEY_USER_ID, user.getId());
                editor.putString(KEY_USERNAME, user.getUsername());
                editor.apply();
                
                Toast.makeText(MainActivity.this, "Bienvenue, " + user.getUsername(), Toast.LENGTH_SHORT).show();
                
                // Charger les tâches de l'utilisateur
                loadUserTasks();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Erreur d'identification: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserTasks() {
        if (currentUser != null) {
            apiClient.getUserTasks(currentUser.getId(), new ApiClient.TasksCallback() {
                @Override
                public void onSuccess(List<Task> tasks) {
                    userTasks.clear();
                    userTasks.addAll(tasks);
                    filterTasks();
                }

                @Override
                public void onError(String error) {
                    if (error != null && !error.contains("null")) {
                        Log.w("API", "Erreur lors du chargement des tâches: " + error);
                    }
                }
            });
        }
    }

    private void filterTasks() {
        if (userTasks.isEmpty()) {
            taskAdapter.updateTasks(new ArrayList<>());
            return;
        }

        List<Task> filteredTasks = new ArrayList<>(userTasks);
        
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
        
        // Filtre par priorité
        int priorityPosition = priorityFilterSpinner.getSelectedItemPosition();
        if (priorityPosition > 0) {
            int selectedPriority = priorityPosition;
            List<Task> priorityFiltered = new ArrayList<>();
            for (Task task : filteredTasks) {
                if (task.getPriority() == selectedPriority) {
                    priorityFiltered.add(task);
                }
            }
            filteredTasks = priorityFiltered;
        }
        
        taskAdapter.updateTasks(filteredTasks);
    }

    private void showTaskDialog(final Task task) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_task_edit);
        dialog.setTitle(task == null ? "Créer une tâche" : "Modifier une tâche");

        // Initialiser les vues du dialogue
        final Spinner groupSpinner = dialog.findViewById(R.id.spinner_task_group);
        final EditText titleEditText = dialog.findViewById(R.id.edit_task_title);
        final EditText descriptionEditText = dialog.findViewById(R.id.edit_task_description);
        final RadioGroup priorityRadioGroup = dialog.findViewById(R.id.radio_group_priority);
        final Button selectDateButton = dialog.findViewById(R.id.btn_select_date);
        Button cancelButton = dialog.findViewById(R.id.btn_cancel_task);
        Button saveButton = dialog.findViewById(R.id.btn_save_task);

        // Configurer le spinner de groupe
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableGroups);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupAdapter);

        // Variables pour stocker la date sélectionnée
        final Calendar calendar = Calendar.getInstance();
        final Date[] selectedDate = {calendar.getTime()};

        // Si on modifie une tâche existante, remplir les champs
        if (task != null) {
            int groupIndex = availableGroups.indexOf(task.getGroupName());
            if (groupIndex >= 0) {
                groupSpinner.setSelection(groupIndex);
            }

            titleEditText.setText(task.getTitle());
            descriptionEditText.setText(task.getDescription());

            RadioButton priorityRadio;
            switch (task.getPriority()) {
                case 1:
                    priorityRadio = dialog.findViewById(R.id.radio_priority_1);
                    break;
                case 2:
                    priorityRadio = dialog.findViewById(R.id.radio_priority_2);
                    break;
                case 3:
                    priorityRadio = dialog.findViewById(R.id.radio_priority_3);
                    break;
                default:
                    priorityRadio = dialog.findViewById(R.id.radio_priority_1);
            }
            priorityRadio.setChecked(true);

            calendar.setTime(task.getDueDate());
            selectedDate[0] = task.getDueDate();
            selectDateButton.setText(String.format("%02d/%02d/%d", calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)));
        }

        // Configurer le sélecteur de date
        selectDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month);
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                selectedDate[0] = calendar.getTime();
                                selectDateButton.setText(String.format("%02d/%02d/%d", dayOfMonth, month + 1, year));
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.show();
            }
        });

        // Configurer les boutons d'action
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Récupérer les valeurs
                String title = titleEditText.getText().toString().trim();
                String description = descriptionEditText.getText().toString().trim();
                int selectedGroupPosition = groupSpinner.getSelectedItemPosition();
                if (selectedGroupPosition == Spinner.INVALID_POSITION || availableGroups.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Veuillez sélectionner un groupe.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String groupName = availableGroups.get(selectedGroupPosition);

                int priority;
                int selectedRadioId = priorityRadioGroup.getCheckedRadioButtonId();
                if (selectedRadioId == R.id.radio_priority_1) {
                    priority = 1;
                } else if (selectedRadioId == R.id.radio_priority_2) {
                    priority = 2;
                } else {
                    priority = 3;
                }

                // Validation
                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Le titre est requis", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (task == null) {
                    // Création d'une nouvelle tâche
                    Task newTask = new Task(
                            currentUser.getId(),
                            groupName,
                            title,
                            description,
                            priority,
                            selectedDate[0]
                    );

                    apiClient.createTask(newTask, new ApiClient.TaskCallback() {
                        @Override
                        public void onSuccess(Task createdTask) {
                            Toast.makeText(MainActivity.this, "Tâche créée avec succès", Toast.LENGTH_SHORT).show();
                            loadUserTasks();
                            dialog.dismiss();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(MainActivity.this, "Erreur lors de la création de la tâche: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Mise à jour d'une tâche existante
                    task.setGroupName(groupName);
                    task.setTitle(title);
                    task.setDescription(description);
                    task.setPriority(priority);
                    task.setDueDate(selectedDate[0]);
                    task.updateLastModified();

                    apiClient.updateTask(task, new ApiClient.TaskCallback() {
                        @Override
                        public void onSuccess(Task updatedTask) {
                            Toast.makeText(MainActivity.this, "Tâche mise à jour avec succès", Toast.LENGTH_SHORT).show();
                            loadUserTasks();
                            dialog.dismiss();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(MainActivity.this, "Erreur lors de la mise à jour de la tâche: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        dialog.show();
        // Forcer la boîte de dialogue à occuper toute la largeur
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Override
    public void onTaskClick(Task task, int position) {
        Log.d("DEBUG", "Créer une tâche cliqué");
        showTaskDialog(task);
    }

    @Override
    public void onTaskCompleted(Task task, int position, boolean isCompleted) {
        apiClient.updateTask(task, new ApiClient.TaskCallback() {
            @Override
            public void onSuccess(Task updatedTask) {
                Toast.makeText(MainActivity.this, "Statut de la tâche mis à jour", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Erreur lors de la mise à jour du statut: " + error, Toast.LENGTH_SHORT).show();

                // Annuler le changement en cas d'erreur
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
}
