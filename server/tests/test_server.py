#!/usr/bin/env python3

import os
import sys
import unittest
import json
import tempfile
from datetime import datetime, timedelta

# Ajout du chemin du projet au PYTHONPATH
sys.path.insert(0, '/home/ubuntu/TaskAPP')

from server import create_app
from server.models import db, User, Task, EventLog

class TaskAppServerTestCase(unittest.TestCase):
    def setUp(self):
        # Création d'une base de données temporaire pour les tests
        self.db_fd, self.db_path = tempfile.mkstemp()
        self.app = create_app()
        self.app.config['TESTING'] = True
        self.app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{self.db_path}'
        
        self.client = self.app.test_client()
        
        with self.app.app_context():
            db.create_all()
            self._create_test_data()
    
    def tearDown(self):
        with self.app.app_context():
            db.session.remove()
            db.drop_all()
        os.close(self.db_fd)
        os.unlink(self.db_path)
    
    def _create_test_data(self):
        # Créer des utilisateurs de test
        user1 = User(username='testuser1')
        user2 = User(username='testuser2')
        db.session.add(user1)
        db.session.add(user2)
        db.session.commit()
        
        # Créer des tâches de test
        task1 = Task(
            user_id=user1.id,
            group_name='Lu03',
            title='Tâche de test 1',
            description='Description de la tâche 1',
            priority=1,
            due_date=datetime.utcnow() + timedelta(days=1),
            completed=False
        )
        
        task2 = Task(
            user_id=user2.id,
            group_name='Lu04',
            title='Tâche de test 2',
            description='Description de la tâche 2',
            priority=2,
            due_date=datetime.utcnow() + timedelta(days=2),
            completed=True
        )
        
        db.session.add(task1)
        db.session.add(task2)
        db.session.commit()
    
    def test_get_users(self):
        """Test de récupération des utilisateurs"""
        response = self.client.get('/api/users')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(len(data), 2)
        self.assertEqual(data[0]['username'], 'testuser1')
        self.assertEqual(data[1]['username'], 'testuser2')
    
    def test_create_user(self):
        """Test de création d'un utilisateur"""
        response = self.client.post('/api/users', 
                                   json={'username': 'newuser'})
        self.assertEqual(response.status_code, 201)
        data = json.loads(response.data)
        self.assertEqual(data['username'], 'newuser')
        
        # Vérifier que l'utilisateur a bien été créé
        response = self.client.get('/api/users')
        data = json.loads(response.data)
        self.assertEqual(len(data), 3)
    
    def test_get_tasks(self):
        """Test de récupération des tâches"""
        response = self.client.get('/api/tasks')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(len(data), 2)
        self.assertEqual(data[0]['title'], 'Tâche de test 1')
        self.assertEqual(data[1]['title'], 'Tâche de test 2')
    
    def test_create_task(self):
        """Test de création d'une tâche"""
        with self.app.app_context():
            user = User.query.filter_by(username='testuser1').first()
            
        response = self.client.post('/api/tasks', 
                                   json={
                                       'user_id': user.id,
                                       'group_name': 'Lu05',
                                       'title': 'Nouvelle tâche',
                                       'description': 'Description de la nouvelle tâche',
                                       'priority': 3,
                                       'due_date': (datetime.utcnow() + timedelta(days=3)).isoformat()
                                   })
        self.assertEqual(response.status_code, 201)
        data = json.loads(response.data)
        self.assertEqual(data['title'], 'Nouvelle tâche')
        self.assertEqual(data['priority'], 3)
        
        # Vérifier que la tâche a bien été créée
        response = self.client.get('/api/tasks')
        data = json.loads(response.data)
        self.assertEqual(len(data), 3)
    
    def test_update_task(self):
        """Test de mise à jour d'une tâche"""
        with self.app.app_context():
            task = Task.query.filter_by(title='Tâche de test 1').first()
            
        response = self.client.put(f'/api/tasks/{task.id}', 
                                  json={
                                      'title': 'Tâche modifiée',
                                      'completed': True
                                  })
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(data['title'], 'Tâche modifiée')
        self.assertTrue(data['completed'])
    
    def test_filter_tasks_by_group(self):
        """Test de filtrage des tâches par groupe"""
        response = self.client.get('/api/tasks?group=Lu03')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]['group_name'], 'Lu03')
    
    def test_filter_tasks_by_user(self):
        """Test de filtrage des tâches par utilisateur"""
        with self.app.app_context():
            user = User.query.filter_by(username='testuser1').first()
            
        response = self.client.get(f'/api/tasks?user_id={user.id}')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]['username'], 'testuser1')
    
    def test_get_groups(self):
        """Test de récupération des groupes disponibles"""
        response = self.client.get('/api/groups')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertIn('Lu03', data)
        self.assertIn('Lu04', data)
    
    def test_get_priorities(self):
        """Test de récupération des priorités disponibles"""
        response = self.client.get('/api/priorities')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(data['1'], 'Vert')
        self.assertEqual(data['2'], 'Orange')
        self.assertEqual(data['3'], 'Rouge')
    
    def test_events_logging(self):
        """Test de journalisation des événements"""
        with self.app.app_context():
            user = User.query.filter_by(username='testuser1').first()
            
            # Créer une tâche pour générer un événement
            response = self.client.post('/api/tasks', 
                                       json={
                                           'user_id': user.id,
                                           'group_name': 'Lu05',
                                           'title': 'Tâche pour événement',
                                           'description': 'Description',
                                           'priority': 1,
                                           'due_date': (datetime.utcnow() + timedelta(days=1)).isoformat()
                                       })
            
            # Vérifier que l'événement a été journalisé
            events = EventLog.query.filter_by(action='create_task').all()
            self.assertGreaterEqual(len(events), 1)
            
            # Vérifier l'API des événements
            response = self.client.get('/api/events')
            self.assertEqual(response.status_code, 200)
            data = json.loads(response.data)
            self.assertGreaterEqual(len(data), 1)
            
            # Vérifier le filtrage des événements
            response = self.client.get(f'/api/events?user_id={user.id}')
            self.assertEqual(response.status_code, 200)
            data = json.loads(response.data)
            for event in data:
                self.assertEqual(event['user_id'], user.id)

if __name__ == '__main__':
    unittest.main()
