import threading
import time
from flask import jsonify
from datetime import datetime

class SyncManager:
    """
    Gestionnaire de synchronisation pour maintenir les données à jour entre les clients
    """
    def __init__(self, app=None):
        self.app = app
        self.sync_thread = None
        self.running = False
        
        if app is not None:
            self.init_app(app)
    
    def init_app(self, app):
        """Initialise le gestionnaire avec l'application Flask"""
        self.app = app
        
        # Ajouter une route pour vérifier le statut de synchronisation
        @app.route('/api/sync/status', methods=['GET'])
        def sync_status():
            return jsonify({
                'running': self.running,
                'last_update': datetime.utcnow().isoformat(),
                'update_interval': app.config['UPDATE_INTERVAL']
            })
        
        # Démarrer le thread de synchronisation lors du démarrage de l'application
        with app.app_context():
            self.start_sync_thread()
    
    def start_sync_thread(self):
        """Démarre le thread de synchronisation"""
        if self.sync_thread is None or not self.sync_thread.is_alive():
            self.running = True
            self.sync_thread = threading.Thread(target=self._sync_loop)
            self.sync_thread.daemon = True
            self.sync_thread.start()
    
    def stop_sync_thread(self):
        """Arrête le thread de synchronisation"""
        self.running = False
        if self.sync_thread and self.sync_thread.is_alive():
            self.sync_thread.join(timeout=5)
    
    def _sync_loop(self):
        """Boucle principale du thread de synchronisation"""
        from .models import db, EventLog
        
        while self.running:
            with self.app.app_context():
                try:
                    # Journaliser l'événement de synchronisation
                    event = EventLog(
                        user_id=None,
                        action='system_sync',
                        details='Synchronisation périodique des données'
                    )
                    db.session.add(event)
                    db.session.commit()
                    
                    # Attendre l'intervalle de mise à jour
                    time.sleep(self.app.config['UPDATE_INTERVAL'])
                except Exception as e:
                    print(f"Erreur lors de la synchronisation: {str(e)}")
                    time.sleep(5)  # Attendre un peu en cas d'erreur
