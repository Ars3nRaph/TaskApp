# Guide d'installation et de déploiement

Ce document explique comment installer et déployer l'application TaskAPP.

## Structure de l'archive

Cette archive contient trois dossiers principaux :

1. **Android** : Contient le projet Android Studio complet pour l'application mobile
2. **server** : Contient le code du serveur Flask et la base de données SQLite
3. **explication** : Contient les guides d'utilisation et de déploiement détaillés

## Installation du serveur

1. Accédez au dossier `server`
2. Rendez le script de déploiement exécutable :
   ```bash
   chmod +x deploy_server.sh
   ```
3. Exécutez le script de déploiement :
   ```bash
   ./deploy_server.sh
   ```
4. Suivez les instructions affichées à l'écran pour démarrer le serveur

Pour plus de détails sur la configuration et le déploiement du serveur en production, consultez le guide de déploiement complet dans le dossier `explication`.

## Installation de l'application Android

1. Ouvrez Android Studio
2. Sélectionnez "Open an existing project"
3. Naviguez vers le dossier `Android` et ouvrez-le
4. Configurez l'URL du serveur dans le fichier `app/src/main/java/com/example/taskapp/api/ApiClient.java`
5. Compilez l'application en sélectionnant "Build" > "Build Bundle(s) / APK(s)" > "Build APK(s)"
6. Installez l'APK généré sur les appareils des agents de maintenance

Pour plus de détails sur l'utilisation de l'application, consultez le guide utilisateur dans le dossier `explication`.

## Support technique

En cas de problème lors de l'installation ou de l'utilisation de l'application, consultez les guides détaillés dans le dossier `explication` ou contactez l'équipe de support technique.
