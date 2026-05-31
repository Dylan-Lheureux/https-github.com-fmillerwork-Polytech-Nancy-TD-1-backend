package service;

import com.example.todoapp.Task;
import dao.TaskDao;
import dto.CreateTaskRequest;
import dto.TaskResponse;
import dto.UpdateTaskRequest;

import java.util.ArrayList;
import java.util.Optional;


public class TaskService {
    private final TaskDao dao = new TaskDao();

    /**
     * Crée une nouvelle tâche à partir du DTO de création.
     * L'id est généré par SQLite ; {@code done} est initialisé à {@code false}.
     *
     * @param request DTO de création validé.
     * @return DTO de réponse avec l'id auto-généré.
     */
    public TaskResponse create(CreateTaskRequest request) {
        Task created = dao.create(request.title(), request.description());
        return toResponse(created);
    }

    /**
     * Retourne une tâche par son identifiant.
     *
     * @param id identifiant de la tâche.
     * @return DTO de réponse, ou vide si introuvable.
     */
    public Optional<TaskResponse> findById(int id) {
        return dao.findById(id).map(this::toResponse);
    }

    /**
     * Retourne toutes les tâches.
     *
     * @return liste de DTO de réponse.
     */
    public ArrayList<TaskResponse> findAll() {
        ArrayList<TaskResponse> responses = new ArrayList<>();
        for (Task t : dao.findAll()) {
            responses.add(toResponse(t));
        }
        return responses;
    }

    /**
     * Supprime une tâche par son identifiant.
     *
     * @param id identifiant de la tâche à supprimer.
     */
    public void deleteById(int id) {
        dao.deleteById(id);
    }

    /**
     * Met à jour une tâche à partir du DTO de modification.
     *
     * @param id      identifiant de la tâche à modifier.
     * @param request DTO de modification validé.
     */
    public void updateById(int id, UpdateTaskRequest request) {
        dao.updateById(id, request.title(), request.description(), request.done());
    }

    // -------------------------------------------------------------------------
    // Mapping interne model → DTO
    // -------------------------------------------------------------------------

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(task.id(), task.title(), task.description(), task.done());
    }
}