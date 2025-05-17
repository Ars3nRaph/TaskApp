# Documentation utilisateur - TaskAPP

## Introduction

TaskAPP est une application de gestion de tâches conçue spécifiquement pour coordonner le travail d'une équipe d'agents de maintenance dans une usine. L'application permet de créer, modifier, suivre et synchroniser des tâches entre tous les utilisateurs.

## Fonctionnalités principales

### Pour les agents de maintenance

1. **Identification personnelle**
   - Chaque utilisateur s'identifie avec son nom d'utilisateur
   - L'identifiant est enregistré localement pour les connexions futures

2. **Gestion des tâches personnelles (Page 1)**
   - Création de nouvelles tâches avec :
     - Groupe (Lu03, Lu04, Lu05, etc.)
     - Titre
     - Description
     - Priorité (1 à 3)
     - Date de fin
   - Modification des tâches existantes
   - Marquage des tâches comme terminées
   - Filtrage des tâches par groupe, date ou priorité

3. **Vue globale des tâches (Page 2)**
   - Visualisation de toutes les tâches créées par tous les utilisateurs
   - Tri par groupe
   - Mise à jour automatique toutes les 30 secondes

### Codes couleur des priorités

- Priorité 1 (Faible) : **Vert**
- Priorité 2 (Moyenne) : **Orange**
- Priorité 3 (Haute) : **Rouge**

## Guide d'utilisation

### Première utilisation

1. Installez l'application sur votre appareil Android
2. Lancez l'application
3. Entrez votre nom d'utilisateur et validez
4. Vous êtes maintenant sur la Page 1 (vos tâches personnelles)

### Créer une tâche

1. Sur la Page 1, appuyez sur le bouton "Créer une tâche"
2. Remplissez les champs :
   - Sélectionnez un groupe dans la liste déroulante
   - Entrez un titre pour la tâche
   - Ajoutez une description (optionnel)
   - Sélectionnez une priorité (1, 2 ou 3)
   - Définissez une date de fin
3. Appuyez sur "Enregistrer"
4. La tâche apparaît maintenant dans votre liste et est visible par tous les utilisateurs dans la Page 2

### Modifier une tâche

1. Sur la Page 1, appuyez sur une tâche dans la liste
2. Modifiez les champs souhaités
3. Appuyez sur "Enregistrer"
4. Les modifications sont synchronisées avec tous les utilisateurs

### Marquer une tâche comme terminée

1. Sur la Page 1, cochez la case à côté de la tâche
2. La tâche est automatiquement marquée comme terminée pour tous les utilisateurs

### Filtrer les tâches

1. Sur la Page 1, utilisez les listes déroulantes en haut de l'écran pour filtrer par :
   - Groupe
   - Priorité
2. La liste des tâches est mise à jour en fonction des filtres sélectionnés

### Consulter toutes les tâches

1. Appuyez sur le bouton "Page 2" en bas de l'écran
2. Vous voyez maintenant toutes les tâches de tous les utilisateurs, triées par groupe
3. Vous pouvez filtrer par groupe en utilisant la liste déroulante en haut de l'écran
4. Pour revenir à vos tâches personnelles, appuyez sur le bouton "Page 1"

## Synchronisation

L'application se synchronise automatiquement avec le serveur toutes les 30 secondes. Cela garantit que :
- Les nouvelles tâches créées par d'autres utilisateurs sont visibles
- Les modifications apportées par d'autres utilisateurs sont reflétées
- Les tâches marquées comme terminées sont mises à jour pour tous

## Groupes disponibles

L'application prend en charge les groupes suivants :
Lu03, Lu04, Lu05, Lu12, Lu22, Lu23, Lu25, Lu26, Lu27, Lu28, Lu30, Lu32, Lu41, Gemini

## Support technique

En cas de problème avec l'application, veuillez contacter votre administrateur système ou l'équipe de support technique.
