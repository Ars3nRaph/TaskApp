package com.example.taskapp.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.taskapp.api.ApiClient;
import com.example.taskapp.models.Task;

import java.util.List;

/**
 * Gestionnaire de synchronisation pour maintenir les données à jour entre l'application Android et le serveur Flask.
 * Cette classe implémente un mécanisme de synchronisation périodique toutes les 30 secondes.
 */
public class SyncManager {
    private static final int SYNC_INTERVAL = 30000; // 30 secondes en millisecondes
    
    private Context context;
    private ApiClient apiClient;
    private Handler syncHandler;
    private Runnable syncRunnable;
    private SyncListener syncListener;
    private boolean isRunning = false;
    
    public interface SyncListener {
        void onSyncStarted();
        void onSyncCompleted(List<Task> tasks);
        void onSyncError(String error);
    }
    
    public SyncManager(Context context, SyncListener listener) {
        this.context = context;
        this.apiClient = new ApiClient(context);
        this.syncListener = listener;
        this.syncHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Démarre la synchronisation périodique
     */
    public void startSync() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                performSync();
                // Planifier la prochaine synchronisation
                if (isRunning) {
                    syncHandler.postDelayed(this, SYNC_INTERVAL);
                }
            }
        };
        
        // Exécuter immédiatement la première synchronisation
        syncHandler.post(syncRunnable);
    }
    
    /**
     * Arrête la synchronisation périodique
     */
    public void stopSync() {
        isRunning = false;
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
    }
    
    /**
     * Force une synchronisation immédiate
     */
    public void forceSync() {
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
            syncHandler.post(syncRunnable);
        }
    }
    
    /**
     * Effectue la synchronisation avec le serveur
     */
    private void performSync() {
        if (syncListener != null) {
            syncListener.onSyncStarted();
        }
        
        apiClient.getTasks(new ApiClient.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                if (syncListener != null) {
                    syncListener.onSyncCompleted(tasks);
                }
            }
            
            @Override
            public void onError(String error) {
                if (syncListener != null) {
                    syncListener.onSyncError(error);
                }
            }
        });
    }
    
    /**
     * Vérifie si la synchronisation est en cours d'exécution
     */
    public boolean isRunning() {
        return isRunning;
    }
}
