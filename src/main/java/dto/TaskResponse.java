package dto;

/**
 * DTO retourné par tous les endpoints de consultation de tâches.
 *
 * @param id          identifiant de la tâche
 * @param title       titre de la tâche
 * @param description description de la tâche
 * @param done        statut d'accomplissement
 */
public record TaskResponse(Integer id, String title, String description, boolean done) {
}
