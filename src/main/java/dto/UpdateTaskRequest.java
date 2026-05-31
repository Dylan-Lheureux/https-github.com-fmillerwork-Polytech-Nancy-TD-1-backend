package dto;

/**
 * DTO reçu lors de la modification d'une tâche (PUT /tasks/{id}).
 *
 * @param title       nouveau titre (obligatoire, 1–50 caractères)
 * @param description nouvelle description (obligatoire, 1–255 caractères)
 * @param done        nouveau statut d'accomplissement
 */
public record UpdateTaskRequest(String title, String description, Boolean done) {
}
