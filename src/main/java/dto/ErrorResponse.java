package dto;

/**
 * DTO retourné avec un statut HTTP 500 lorsqu'une erreur inattendue se produit
 * côté serveur.
 *
 * @param message description générique de l'erreur.
 */
public record ErrorResponse(String message) {
}
