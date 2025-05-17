from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
from .extensions import db

#db = SQLAlchemy()

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
            'username': self.creator.username if self.creator else None,
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
