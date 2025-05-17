from flask import Blueprint, request, jsonify
from datetime import datetime
from app.models import db, User, Task, EventLog
from app.config import Config

api_bp = Blueprint('api', __name__, url_prefix='/api')

# Fonctions utilitaires
def log_event(user_id, action, details):
    """Enregistre un événement dans le journal"""
    event = EventLog(user_id=user_id, action=action, details=details)
    db.session.add(event)
    db.session.commit()
    return event

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
    if data['group_name'] not in Config.AVAILABLE_GROUPS:
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
        if data['group_name'] in Config.AVAILABLE_GROUPS:
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
    
    if 'user_id' in data:
        user = User.query.get(data['user_id'])
        if not user:
            return jsonify({'error': 'Utilisateur introuvable'}), 404
        task.user_id = user.id
        task.creator = user     #Peut-etre a supprimer
        
    task.last_modified_timestamp = datetime.utcnow()
    db.session.commit()
    db.session.refresh(task)    #Peut-etre a supprimer
    
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
    return jsonify(Config.AVAILABLE_GROUPS)

# Route pour obtenir les priorités disponibles
@api_bp.route('/priorities', methods=['GET'])
def get_priorities():
    """Récupère la liste des priorités disponibles avec leurs codes couleur"""
    return jsonify(Config.PRIORITIES)
