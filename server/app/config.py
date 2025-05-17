import os

class Config:
    # Configuration de base
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'clé_secrète_par_défaut'
    SQLALCHEMY_DATABASE_URI = os.environ.get('DATABASE_URL') or 'sqlite:///taskapp.db'
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    
    # Liste des groupes disponibles
    AVAILABLE_GROUPS = [
        'Lu03', 'Lu04', 'Lu05', 'Lu12', 'Lu22', 'Lu23', 
        'Lu25', 'Lu26', 'Lu27', 'Lu28', 'Lu30', 'Lu32', 
        'Lu41', 'Gemini'
    ]
    
    # Priorités et leurs codes couleur
    PRIORITIES = {
        1: 'Vert',    # Faible priorité
        2: 'Orange',  # Priorité moyenne
        3: 'Rouge'    # Haute priorité
    }
    
    # Intervalle de mise à jour en secondes
    UPDATE_INTERVAL = 30
