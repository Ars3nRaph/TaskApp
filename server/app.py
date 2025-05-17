from flask import Flask
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
import os

# Initialisation de l'application Flask et des extensions
app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY') or 'clé_secrète_par_défaut'
app.config['SQLALCHEMY_DATABASE_URI'] = os.environ.get('DATABASE_URL') or 'sqlite:///taskapp.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

# Liste des groupes disponibles
app.config['AVAILABLE_GROUPS'] = [
    'Lu03', 'Lu04', 'Lu05', 'Lu12', 'Lu22', 'Lu23', 
    'Lu25', 'Lu26', 'Lu27', 'Lu28', 'Lu30', 'Lu32', 
    'Lu41', 'Gemini'
]

# Priorités et leurs codes couleur
app.config['PRIORITIES'] = {
    1: 'Vert',    # Faible priorité
    2: 'Orange',  # Priorité moyenne
    3: 'Rouge'    # Haute priorité
}

# Intervalle de mise à jour en secondes
app.config['UPDATE_INTERVAL'] = 30

# Initialisation des extensions
db = SQLAlchemy(app)
CORS(app)

# Définition des modèles
class User(db.Model):
    __tablename__ = 'users'
    
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(100), unique=True, nullable=False)
    
    tasks = db.relationship('Task', backref='creator', lazy=True)
    events = db.relationship('EventLog', backref='user', lazy=True)
    
    def __repr__(self):
        return f'<User {self.username}>'
    
    def to_dict(self):
        return {
            'id': self.id,
            'username': self.username
        }

class Task(db.Model):
    __tablename__ = 'tasks'
    
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    group_name = db.Column(db.String(50), nullable=False)
    title = db.Column(db.String(200), nullable=False)
    description = db.Column(db.Text, nullable=True)
    priority = db.Column(db.Integer, nullable=False)  # 1, 2, 3
    due_date = db.Column(db.DateTime, nullable=False)
    completed = db.Column(db.Boolean, default=False)
    creation_timestamp = db.Column(db.DateTime, default=datetime.utcnow)
    last_modified_timestamp = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    def __repr__(self):
        return f'<Task {self.title}>'
    
    def to_dict(self):
        return {
            'id': self.id,
            'user_id': self.user_id,
            'username': self.creator.username,
            'group_name': self.group_name,
            'title': self.title,
            'description': self.description,
            'priority': self.priority,
            'due_date': self.due_date.isoformat(),
            'completed': self.completed,
            'creation_timestamp': self.creation_timestamp.isoformat(),
            'last_modified_timestamp': self.last_modified_timestamp.isoformat()
        }

class EventLog(db.Model):
    __tablename__ = 'event_log'
    
    id = db.Column(db.Integer, primary_key=True)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=True)  # Peut être null si action système
    action = db.Column(db.String(50), nullable=False)  # create_task, complete_task, update_task, user_login
    details = db.Column(db.Text, nullable=True)
    
    def __repr__(self):
        return f'<EventLog {self.action}>'
    
    def to_dict(self):
        return {
            'id': self.id,
            'timestamp': self.timestamp.isoformat(),
            'user_id': self.user_id,
            'username': self.user.username if self.user_id else None,
            'action': self.action,
            'details': self.details
        }

# Fonctions utilitaires
def log_event(user_id, action, details):
    """Enregistre un événement dans le journal"""
    event = EventLog(user_id=user_id, action=action, details=details)
    db.session.add(event)
    db.session.commit()
    return event

# Routes API
from flask import Blueprint, request, jsonify
from datetime import datetime

api_bp = Blueprint('api', __name__, url_prefix='/api')

# Routes pour les utilisateurs
@api_bp.route('/users', methods=['GET'])
def get_users():
    """Récupère la liste de tous les utilisateurs"""
    users = User.query.all()
    return jsonify([user.to_dict() for user in users])

@api_bp.route('/users', methods=['POST'])
def create_user():
    """Crée un nouvel utilisateur"""
    data = request.get_json()
    
    if not data or 'username' not in data:
        return jsonify({'error': 'Le nom d\'utilisateur est requis'}), 400
    
    # Vérifier si l'utilisateur existe déjà
    existing_user = User.query.filter_by(username=data['username']).first()
    if existing_user:
        return jsonify({'error': 'Ce nom d\'utilisateur existe déjà'}), 409
    
    # Créer le nouvel utilisateur
    user = User(username=data['username'])
    db.session.add(user)
    db.session.commit()
    
    # Journaliser l'événement
    log_event(user.id, 'user_login', f"Utilisateur '{user.username}' créé/connecté")
    
    return jsonify(user.to_dict()), 201

@api_bp.route('/users/<int:user_id>', methods=['GET'])
def get_user(user_id):
    """Récupère les informations d'un utilisateur spécifique"""
    user = User.query.get_or_404(user_id)
    return jsonify(user.to_dict())

# Routes pour les tâches
@api_bp.route('/tasks', methods=['GET'])
def get_tasks():
    """Récupère la liste de toutes les tâches, avec filtrage optionnel"""
    group = request.args.get('group')
    user_id = request.args.get('user_id', type=int)
    
    query = Task.query
    
    if group:
        query = query.filter_by(group_name=group)
    if user_id:
        query = query.filter_by(user_id=user_id)
    
    tasks = query.all()
    return jsonify([task.to_dict() for task in tasks])

@api_bp.route('/tasks', methods=['POST'])
def create_task():
    """Crée une nouvelle tâche"""
    data = request.get_json()
    
    required_fields = ['user_id', 'group_name', 'title', 'priority', 'due_date']
    for field in required_fields:
        if field not in data:
            return jsonify({'error': f'Le champ {field} est requis'}), 400
    
    # Vérifier si l'utilisateur existe
    user = User.query.get(data['user_id'])
    if not user:
        return jsonify({'error': 'Utilisateur non trouvé'}), 404
    
    # Vérifier si le groupe est valide
    if data['group_name'] not in app.config['AVAILABLE_GROUPS']:
        return jsonify({'error': 'Groupe non valide'}), 400
    
    # Vérifier si la priorité est valide
    if data['priority'] not in [1, 2, 3]:
        return jsonify({'error': 'Priorité non valide (doit être 1, 2 ou 3)'}), 400
    
    # Convertir la date d'échéance
    try:
        due_date = datetime.fromisoformat(data['due_date'])
    except ValueError:
        return jsonify({'error': 'Format de date invalide (utilisez ISO 8601)'}), 400
    
    # Créer la nouvelle tâche
    task = Task(
        user_id=data['user_id'],
        group_name=data['group_name'],
        title=data['title'],
        description=data.get('description', ''),
        priority=data['priority'],
        due_date=due_date,
        completed=False
    )
    
    db.session.add(task)
    db.session.commit()
    
    # Journaliser l'événement
    log_event(user.id, 'create_task', f"Tâche '{task.title}' (ID: {task.id}) créée")
    
    return jsonify(task.to_dict()), 201

@api_bp.route('/tasks/<int:task_id>', methods=['GET'])
def get_task(task_id):
    """Récupère les informations d'une tâche spécifique"""
    task = Task.query.get_or_404(task_id)
    return jsonify(task.to_dict())

@api_bp.route('/tasks/<int:task_id>', methods=['PUT'])
def update_task(task_id):
    """Met à jour une tâche existante"""
    task = Task.query.get_or_404(task_id)
    data = request.get_json()
    
    # Mettre à jour les champs modifiables
    if 'group_name' in data:
        if data['group_name'] in app.config['AVAILABLE_GROUPS']:
            task.group_name = data['group_name']
        else:
            return jsonify({'error': 'Groupe non valide'}), 400
    
    if 'title' in data:
        task.title = data['title']
    
    if 'description' in data:
        task.description = data['description']
    
    if 'priority' in data:
        if data['priority'] in [1, 2, 3]:
            task.priority = data['priority']
        else:
            return jsonify({'error': 'Priorité non valide (doit être 1, 2 ou 3)'}), 400
    
    if 'due_date' in data:
        try:
            task.due_date = datetime.fromisoformat(data['due_date'])
        except ValueError:
            return jsonify({'error': 'Format de date invalide (utilisez ISO 8601)'}), 400
    
    if 'completed' in data:
        task.completed = bool(data['completed'])
        action = 'complete_task' if task.completed else 'uncomplete_task'
        log_event(task.user_id, action, f"Tâche '{task.title}' (ID: {task.id}) marquée comme {'terminée' if task.completed else 'non terminée'}")
    
    db.session.commit()
    
    # Journaliser l'événement de mise à jour (sauf si c'est juste un changement de statut completed)
    if 'completed' not in data or len(data) > 1:
        log_event(task.user_id, 'update_task', f"Tâche '{task.title}' (ID: {task.id}) mise à jour")
    
    return jsonify(task.to_dict())

@api_bp.route('/tasks/<int:task_id>', methods=['DELETE'])
def delete_task(task_id):
    """Supprime une tâche"""
    task = Task.query.get_or_404(task_id)
    user_id = task.user_id
    task_title = task.title
    
    db.session.delete(task)
    db.session.commit()
    
    # Journaliser l'événement
    log_event(user_id, 'delete_task', f"Tâche '{task_title}' (ID: {task_id}) supprimée")
    
    return jsonify({'message': 'Tâche supprimée avec succès'})

# Routes pour le journal des événements
@api_bp.route('/events', methods=['GET'])
def get_events():
    """Récupère le journal des événements, avec filtrage optionnel"""
    user_id = request.args.get('user_id', type=int)
    action = request.args.get('action')
    
    query = EventLog.query.order_by(EventLog.timestamp.desc())
    
    if user_id:
        query = query.filter_by(user_id=user_id)
    if action:
        query = query.filter_by(action=action)
    
    events = query.all()
    return jsonify([event.to_dict() for event in events])

# Route pour obtenir les groupes disponibles
@api_bp.route('/groups', methods=['GET'])
def get_groups():
    """Récupère la liste des groupes disponibles"""
    return jsonify(app.config['AVAILABLE_GROUPS'])

# Route pour obtenir les priorités disponibles
@api_bp.route('/priorities', methods=['GET'])
def get_priorities():
    """Récupère la liste des priorités disponibles avec leurs codes couleur"""
    return jsonify(app.config['PRIORITIES'])

# Enregistrement du blueprint
app.register_blueprint(api_bp)

# Création des tables de la base de données
with app.app_context():
    db.create_all()

# Synchronisation périodique
import threading
import time

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

# Initialisation du gestionnaire de synchronisation
sync_manager = SyncManager(app)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
