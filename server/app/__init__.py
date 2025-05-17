from flask import Flask
from flask_cors import CORS
#from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate
from app.config import Config
from .extensions import db
from .models import User, Task, EventLog

#db = SQLAlchemy()

def create_app(config_class=Config):
    app = Flask(__name__)
    app.config.from_object(config_class)
    
    # Initialisation des extensions
    db.init_app(app)
    Migrate(app, db)
    CORS(app)
    
    # Importation et enregistrement des routes
    from .routes import api_bp
    app.register_blueprint(api_bp)
    
    # Création des tables de la base de données
    with app.app_context():
        db.create_all()
    
    # Initialisation du gestionnaire de synchronisation
    from .sync import SyncManager
    sync_manager = SyncManager(app)
        
    return app
