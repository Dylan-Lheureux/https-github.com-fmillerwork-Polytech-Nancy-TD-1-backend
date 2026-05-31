package presentation;

import com.example.todoapp.JsonUtils;
import dto.CreateTaskRequest;
import dto.ErrorResponse;
import dto.TaskResponse;
import dto.UpdateTaskRequest;
import dto.ValidationErrorResponse;
import service.TaskService;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private static final Pattern ID_PATH = Pattern.compile("^/tasks/([0-9]+)$");
    private static final TaskService service = new TaskService();

    private static final int MAX_TITLE_LENGTH       = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    public static void handleTasks(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (Exception e) {
            log.error("Erreur inattendue lors du traitement de {} {}", 
                exchange.getRequestMethod(), 
                exchange.getRequestURI().getPath(), 
                e);
            try {
                ErrorResponse error = new ErrorResponse("Une erreur interne est survenue. Veuillez réessayer ultérieurement.");
                sendResponse(exchange, 500, JsonUtils.serialize(error));
            } catch (Exception ignored) {
                // En dernier recours : on ferme proprement la connexion sans corps
                exchange.sendResponseHeaders(500, 0);
                exchange.close();
            }
        }
    }

    /**
     * Contient toute la logique de routage et de traitement des requêtes.
     * Les exceptions remontent librement vers {@link #handleTasks} qui les attrape.
     */
    private static void route(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        //region Manage POST /tasks
        if ("POST".equals(method) && "/tasks".equals(path)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            CreateTaskRequest request = JsonUtils.deserialize(body, CreateTaskRequest.class);

            List<ValidationErrorResponse> errors = validateCreateRequest(request);
            if (!errors.isEmpty()) {
                sendResponse(exchange, 400, JsonUtils.serialize(errors));
                return;
            }

            TaskResponse created = service.create(request);
            exchange.getResponseHeaders().add("Location", "/tasks/" + created.id());
            sendResponse(exchange, 201, JsonUtils.serialize(created));
            return;
        }
        //endregion

        Matcher m = ID_PATH.matcher(path);

        //region Manage GET /tasks/{id}
        if ("GET".equals(method) && m.matches()) {
            int id = Integer.parseInt(m.group(1));
            Optional<TaskResponse> task = service.findById(id);

            if (task.isPresent()) {
                sendResponse(exchange, 200, JsonUtils.serialize(task.get()));
            } else {
                sendResponse(exchange, 404, null);
            }
            return;
        }
        //endregion

        //region Manage GET /tasks
        if ("GET".equals(method) && "/tasks".equals(path)) {
            ArrayList<TaskResponse> tasks = service.findAll();

            if (!tasks.isEmpty()) {
                sendResponse(exchange, 200, JsonUtils.serialize(tasks));
            } else {
                sendResponse(exchange, 204, null);
            }
            return;
        }
        //endregion

        // Recompute matcher for id-based routes below (consumed by GET branch)
        m = ID_PATH.matcher(path);

        //region Manage DELETE /tasks/{id}
        if ("DELETE".equals(method) && m.matches()) {
            int id = Integer.parseInt(m.group(1));
            Optional<TaskResponse> task = service.findById(id);

            if (task.isPresent()) {
                service.deleteById(id);
                sendResponse(exchange, 204, null);
            } else {
                sendResponse(exchange, 404, null);
            }
            return;
        }
        //endregion

        //region Manage PUT /tasks/{id}
        if ("PUT".equals(method) && m.matches()) {
            int id = Integer.parseInt(m.group(1));
            Optional<TaskResponse> existing = service.findById(id);

            if (existing.isEmpty()) {
                sendResponse(exchange, 404, null);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            UpdateTaskRequest request = JsonUtils.deserialize(body, UpdateTaskRequest.class);

            List<ValidationErrorResponse> errors = validateUpdateRequest(request);
            if (!errors.isEmpty()) {
                sendResponse(exchange, 400, JsonUtils.serialize(errors));
                return;
            }

            service.updateById(id, request);
            sendResponse(exchange, 204, null);
            return;
        }
        //endregion

        // Otherwise → 404
        sendResponse(exchange, 404, null);
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Valide le DTO de création d'une tâche.
     * Règles :
     * <ul>
     *   <li>{@code title} : non nul, non vide, ≤ 50 caractères.</li>
     *   <li>{@code description} : non nulle, non vide, ≤ 255 caractères.</li>
     * </ul>
     *
     * @param request DTO à valider.
     * @return liste d'erreurs (vide si tout est valide).
     */
    private static List<ValidationErrorResponse> validateCreateRequest(CreateTaskRequest request) {
        List<ValidationErrorResponse> errors = new ArrayList<>();

        if (request == null) {
            errors.add(new ValidationErrorResponse("body", "Le corps de la requête est manquant ou invalide."));
            return errors;
        }

        errors.addAll(validateTitle(request.title()));
        errors.addAll(validateDescription(request.description()));

        return errors;
    }

    /**
     * Valide le DTO de modification d'une tâche.
     * Règles :
     * <ul>
     *   <li>{@code title} : non nul, non vide, ≤ 50 caractères.</li>
     *   <li>{@code description} : non nulle, non vide, ≤ 255 caractères.</li>
     *   <li>{@code done} : non nul.</li>
     * </ul>
     *
     * @param request DTO à valider.
     * @return liste d'erreurs (vide si tout est valide).
     */
    private static List<ValidationErrorResponse> validateUpdateRequest(UpdateTaskRequest request) {
        List<ValidationErrorResponse> errors = new ArrayList<>();

        if (request == null) {
            errors.add(new ValidationErrorResponse("body", "Le corps de la requête est manquant ou invalide."));
            return errors;
        }

        errors.addAll(validateTitle(request.title()));
        errors.addAll(validateDescription(request.description()));

        if (request.done() == null) {
            errors.add(new ValidationErrorResponse("done", "Le champ 'done' est obligatoire."));
        }

        return errors;
    }

    /** Valide le champ {@code title} et retourne les erreurs éventuelles. */
    private static List<ValidationErrorResponse> validateTitle(String title) {
        List<ValidationErrorResponse> errors = new ArrayList<>();
        if (title == null || title.isBlank()) {
            errors.add(new ValidationErrorResponse("title", "Le champ 'title' est obligatoire et ne peut pas être vide."));
        } else if (title.length() > MAX_TITLE_LENGTH) {
            errors.add(new ValidationErrorResponse("title",
                    "Le champ 'title' ne doit pas dépasser " + MAX_TITLE_LENGTH + " caractères (reçu : " + title.length() + ")."));
        }
        return errors;
    }

    /** Valide le champ {@code description} et retourne les erreurs éventuelles. */
    private static List<ValidationErrorResponse> validateDescription(String description) {
        List<ValidationErrorResponse> errors = new ArrayList<>();
        if (description == null || description.isBlank()) {
            errors.add(new ValidationErrorResponse("description", "Le champ 'description' est obligatoire et ne peut pas être vide."));
        } else if (description.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add(new ValidationErrorResponse("description",
                    "Le champ 'description' ne doit pas dépasser " + MAX_DESCRIPTION_LENGTH + " caractères (reçu : " + description.length() + ")."));
        }
        return errors;
    }

    // -------------------------------------------------------------------------
    // Réponse HTTP
    // -------------------------------------------------------------------------

    private static void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        if (nonNull(json)) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = json.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(status, 0);
            exchange.close();
        }
    }
}
