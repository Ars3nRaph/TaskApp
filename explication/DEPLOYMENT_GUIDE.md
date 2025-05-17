# Guide de déploiement de l'application TaskAPP

Ce document explique comment déployer l'application TaskAPP, qui comprend un serveur Flask et une application Android.

## 1. Déploiement du serveur Flask

### Prérequis
- Python 3.10 ou supérieur
- pip (gestionnaire de paquets Python)
- Accès à un serveur Linux (pour un déploiement en production)

### Étapes de déploiement

1. **Cloner le dépôt**
   ```bash
   git clone [url_du_dépôt]
   cd TaskAPP
   ```

2. **Exécuter le script de déploiement**
   ```bash
   chmod +x deploy_server.sh
   ./deploy_server.sh
   ```

3. **Démarrer le serveur**
   
   En mode développement :
   ```bash
   source venv/bin/activate
   python server/app.py
   ```
   
   En mode production avec Gunicorn :
   ```bash
   source venv/bin/activate
   gunicorn --bind 0.0.0.0:5000 wsgi:app
   ```

4. **Configuration du serveur (optionnel)**
   
   Pour modifier la configuration du serveur, éditer le fichier `server/config.py`.

5. **Configuration d'un service systemd (pour un déploiement en production)**
   
   Créer un fichier de service systemd pour assurer que le serveur démarre automatiquement :
   ```bash
   sudo nano /etc/systemd/system/taskapp.service
   ```
   
   Contenu du fichier :
   ```
   [Unit]
   Description=TaskAPP Flask Server
   After=network.target

   [Service]
   User=ubuntu
   WorkingDirectory=/chemin/vers/TaskAPP
   ExecStart=/chemin/vers/TaskAPP/venv/bin/gunicorn --workers 3 --bind 0.0.0.0:5000 wsgi:app
   Restart=always

   [Install]
   WantedBy=multi-user.target
   ```
   
   Activer et démarrer le service :
   ```bash
   sudo systemctl enable taskapp
   sudo systemctl start taskapp
   ```

## 2. Compilation de l'application Android

### Prérequis
- Android Studio
- JDK 11 ou supérieur
- Gradle

### Étapes de compilation

1. **Ouvrir le projet dans Android Studio**
   - Lancer Android Studio
   - Sélectionner "Open an existing project"
   - Naviguer vers le dossier `TaskAPP/android_app` et l'ouvrir

2. **Configurer l'URL du serveur**
   - Ouvrir le fichier `app/src/main/java/com/example/taskapp/api/ApiClient.java`
   - Modifier la constante `BASE_URL` pour pointer vers l'adresse de votre serveur Flask
   ```java
   private static final String BASE_URL = "http://adresse_ip_serveur:5000/api";
   ```

3. **Compiler l'application**
   - Dans Android Studio, sélectionner "Build" > "Build Bundle(s) / APK(s)" > "Build APK(s)"
   - Une fois la compilation terminée, cliquer sur "locate" pour trouver le fichier APK

4. **Distribuer l'application**
   - Copier le fichier APK généré sur les appareils des agents de maintenance
   - Installer l'application en ouvrant le fichier APK sur chaque appareil
   - Note : Il peut être nécessaire d'autoriser l'installation d'applications provenant de sources inconnues dans les paramètres de sécurité de l'appareil

## 3. Utilisation de l'application

### Pour les administrateurs
- Démarrer le serveur Flask comme indiqué ci-dessus
- Surveiller les journaux du serveur pour détecter d'éventuels problèmes
- Sauvegarder régulièrement la base de données SQLite

### Pour les utilisateurs (agents de maintenance)
- Installer l'application Android sur leur appareil
- S'identifier avec leur nom d'utilisateur
- Créer et gérer leurs tâches
- Consulter les tâches de tous les utilisateurs dans la Page 2

## 4. Dépannage

### Problèmes courants du serveur
- **Le serveur ne démarre pas** : Vérifier les logs avec `sudo journalctl -u taskapp`
- **Erreurs de base de données** : Vérifier les permissions du fichier de base de données

### Problèmes courants de l'application Android
- **Impossible de se connecter au serveur** : Vérifier que l'URL du serveur est correcte et que le serveur est accessible depuis l'appareil
- **Synchronisation ne fonctionne pas** : Vérifier la connexion réseau de l'appareil

Pour toute assistance supplémentaire, contacter l'équipe de support technique.
