package com.example.taskapp.sync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.taskapp.api.ApiClient;
import com.example.taskapp.models.User;

import androidx.appcompat.app.AppCompatActivity;

import com.example.taskapp.R;

public class SettingsActivity extends AppCompatActivity {

    private EditText editUsername, editServerIp;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editUsername = findViewById(R.id.edit_username);
        editServerIp = findViewById(R.id.edit_server_ip);
        btnSave = findViewById(R.id.btn_save_settings);

        SharedPreferences prefs = getSharedPreferences("TaskAppPrefs", MODE_PRIVATE);
        editUsername.setText(prefs.getString("username", ""));
        editServerIp.setText(prefs.getString("server_ip", ""));

        btnSave.setOnClickListener(v -> {
            String newUsername = editUsername.getText().toString();
            String serverIp = editServerIp.getText().toString();

            if (!newUsername.isEmpty()) {
                ApiClient apiClient = new ApiClient(SettingsActivity.this);
                apiClient.createUser(newUsername, new ApiClient.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        // Enregistrer le nouvel utilisateur complet
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("userId", user.getId()); // 👈 très important !!
                        editor.putString("username", user.getUsername());
                        editor.putString("server_ip", serverIp);
                        editor.apply();

                        Toast.makeText(SettingsActivity.this, "Paramètres enregistrés", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(SettingsActivity.this, "Erreur : " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(SettingsActivity.this, "Veuillez entrer un nom", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
