package dto;

/**
 * DTO retourné avec un statut HTTP 400 lorsqu'un champ du corps de la requête
 * ne respecte pas les contraintes de validation.
 *
 * @param field   nom du champ en erreur (ex. {@code "title"})
 * @param message description lisible de l'erreur (ex. {@code "Ne doit pas dépasser 50 caractères"})
 */
public record ValidationErrorResponse(String field, String message) {
}
