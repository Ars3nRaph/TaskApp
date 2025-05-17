package com.example.taskapp.api;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.taskapp.models.Task;
import com.example.taskapp.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://10.213.212.42:5000/api"; // Adresse localhost pour l'émulateur Android
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());

    private RequestQueue requestQueue;

    public ApiClient(Context context) {
        requestQueue = Volley.newRequestQueue(context);
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Interface pour les callbacks
    public interface UserCallback {
        void onSuccess(User user);
        void onError(String error);
    }

    public interface UsersCallback {
        void onSuccess(List<User> users);
        void onError(String error);
    }

    public interface TaskCallback {
        void onSuccess(Task task);
        void onError(String error);
    }

    public interface TasksCallback {
        void onSuccess(List<Task> tasks);
        void onError(String error);
    }

    public interface GroupsCallback {
        void onSuccess(List<String> groups);
        void onError(String error);
    }

    // Méthodes pour les utilisateurs
    public void createUser(String username, final UserCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_URL + "/users",
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                User user = parseUser(response);
                                callback.onSuccess(user);
                            } catch (JSONException e) {
                                callback.onError("Erreur lors du parsing de l'utilisateur: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (error.networkResponse != null && error.networkResponse.statusCode == 409) {
                                // Utilisateur déjà existant → on le récupère depuis la liste
                                getUsers(new UsersCallback() {
                                    @Override
                                    public void onSuccess(List<User> users) {
                                        for (User user : users) {
                                            if (user.getUsername().equalsIgnoreCase(username)) {
                                                callback.onSuccess(user);
                                                return;
                                            }
                                        }
                                        callback.onError("Utilisateur existant non trouvé dans la base.");
                                    }

                                    @Override
                                    public void onError(String errorMsg) {
                                        callback.onError("Erreur lors du chargement des utilisateurs existants : " + errorMsg);
                                    }
                                });
                            } else {
                                callback.onError("Erreur réseau: " + (error.getMessage() != null ? error.getMessage() : "inconnue"));
                            }
                        }
                    }
            );

            requestQueue.add(request);
        } catch (JSONException e) {
            callback.onError("Erreur lors de la création de la requête: " + e.getMessage());
        }
    }

    public void getUsers(final UsersCallback callback) {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                BASE_URL + "/users",
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            List<User> users = new ArrayList<>();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject userJson = response.getJSONObject(i);
                                users.add(parseUser(userJson));
                            }
                            callback.onSuccess(users);
                        } catch (JSONException e) {
                            callback.onError("Erreur lors du parsing des utilisateurs: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            callback.onError("Erreur réseau : Code " + statusCode);
                        } else {
                            callback.onError("Erreur réseau : aucune réponse du serveur");
                        }
                    }
                }
        );

        requestQueue.add(request);
    }

    // Méthodes pour les tâches
    public void createTask(Task task, final TaskCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("user_id", task.getUserId());
            jsonBody.put("group_name", task.getGroupName());
            jsonBody.put("title", task.getTitle());
            jsonBody.put("description", task.getDescription());
            jsonBody.put("priority", task.getPriority());
            jsonBody.put("due_date", ISO_FORMAT.format(task.getDueDate()));

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_URL + "/tasks",
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Task createdTask = parseTask(response);
                                callback.onSuccess(createdTask);
                            } catch (JSONException | ParseException e) {
                                callback.onError("Erreur lors du parsing de la tâche: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            callback.onError("Erreur réseau: " + error.getMessage());
                        }
                    }
            );

            requestQueue.add(request);
        } catch (JSONException e) {
            callback.onError("Erreur lors de la création de la requête: " + e.getMessage());
        }
    }

    public void updateTask(Task task, final TaskCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("group_name", task.getGroupName());
            jsonBody.put("title", task.getTitle());
            jsonBody.put("description", task.getDescription());
            jsonBody.put("priority", task.getPriority());
            jsonBody.put("due_date", ISO_FORMAT.format(task.getDueDate()));
            jsonBody.put("completed", task.isCompleted());
            jsonBody.put("user_id", task.getUserId());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    BASE_URL + "/tasks/" + task.getId(),
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Task updatedTask = parseTask(response);
                                callback.onSuccess(updatedTask);
                            } catch (JSONException | ParseException e) {
                                callback.onError("Erreur lors du parsing de la tâche: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            callback.onError("Erreur réseau: " + error.getMessage());
                        }
                    }
            );

            requestQueue.add(request);
        } catch (JSONException e) {
            callback.onError("Erreur lors de la création de la requête: " + e.getMessage());
        }
    }

    public void getTasks(final TasksCallback callback) {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                BASE_URL + "/tasks",
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            List<Task> tasks = new ArrayList<>();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject taskJson = response.getJSONObject(i);
                                tasks.add(parseTask(taskJson));
                            }
                            callback.onSuccess(tasks);
                        } catch (JSONException | ParseException e) {
                            callback.onError("Erreur lors du parsing des tâches: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onError("Erreur réseau: " + error.getMessage());
                    }
                }
        );

        requestQueue.add(request);
    }

    public void getUserTasks(int userId, final TasksCallback callback) {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                BASE_URL + "/tasks?user_id=" + userId,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            List<Task> tasks = new ArrayList<>();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject taskJson = response.getJSONObject(i);
                                tasks.add(parseTask(taskJson));
                            }
                            callback.onSuccess(tasks);
                        } catch (JSONException | ParseException e) {
                            callback.onError("Erreur lors du parsing des tâches: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // ✅ Ignore entièrement les erreurs système Volley
                        String message = (error.getMessage() != null) ? error.getMessage() : "Erreur réseau inconnue";
                        Log.w("API", "getUserTasks: " + message);
                        callback.onError("Erreur réseau attrapée manuellement");
                    }
                }
        );
        // ✅ Désactiver la mise en cache, optionnel
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    public void getGroups(final GroupsCallback callback) {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                BASE_URL + "/groups",
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            List<String> groups = new ArrayList<>();
                            for (int i = 0; i < response.length(); i++) {
                                groups.add(response.getString(i));
                            }
                            callback.onSuccess(groups);
                        } catch (JSONException e) {
                            callback.onError("Erreur lors du parsing des groupes: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onError("Erreur réseau: " + error.getMessage());
                    }
                }
        );

        requestQueue.add(request);
    }

    // Méthodes utilitaires pour le parsing
    private User parseUser(JSONObject json) throws JSONException {
        int id = json.getInt("id");
        String username = json.getString("username");
        return new User(id, username);
    }

    private Task parseTask(JSONObject json) throws JSONException, ParseException {
        int id = json.getInt("id");
        int userId = json.getInt("user_id");
        String username = json.getString("username");
        String groupName = json.getString("group_name");
        String title = json.getString("title");
        String description = json.getString("description");
        int priority = json.getInt("priority");
        Date dueDate = ISO_FORMAT.parse(json.getString("due_date"));
        boolean completed = json.getBoolean("completed");
        Date creationTimestamp = ISO_FORMAT.parse(json.getString("creation_timestamp"));
        Date lastModifiedTimestamp = ISO_FORMAT.parse(json.getString("last_modified_timestamp"));

        return new Task(id, userId, username, groupName, title, description, priority,
                dueDate, completed, creationTimestamp, lastModifiedTimestamp);
    }
    public void deleteTask(int taskId, final TaskCallback callback) {
        String url = BASE_URL + "/tasks/" + taskId;

        com.android.volley.Request<String> request = new com.android.volley.toolbox.StringRequest(
                Request.Method.DELETE,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Retourne un Task vide ou null, à adapter si besoin
                        callback.onSuccess(null);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            callback.onError("Erreur réseau : Code " + statusCode);
                        } else {
                            callback.onError("Erreur réseau : aucune réponse du serveur");
                        }
                    }
                }
        );

        requestQueue.add(request);
    }
}
