package dto;

/**
 * DTO reçu lors de la création d'une tâche (POST /tasks).
 * <ul>
 *   <li>L'id est autogénéré par SQLite : il n'est pas attendu dans le corps de la requête.</li>
 *   <li>{@code done} est toujours initialisé à {@code false} à la création.</li>
 * </ul>
 *
 * @param title       titre de la tâche (obligatoire, 1–50 caractères)
 * @param description description de la tâche (obligatoire, 1–255 caractères)
 */
public record CreateTaskRequest(String title, String description) {
}
