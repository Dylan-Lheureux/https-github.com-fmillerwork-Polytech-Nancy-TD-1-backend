package dao;

import com.example.todoapp.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Data Access Object for {@link Task} model.
 */
public class TaskDao {

    private static final Logger log = LoggerFactory.getLogger(TaskDao.class);
    private static final String DB_URL = "jdbc:sqlite:tasks.db";

    public TaskDao() {
        initTable();
        seedData();
    }

    /**
     * Crée la table TASK si elle n'existe pas encore.
     */
    private void initTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS task (
                    id          INTEGER PRIMARY KEY,
                    title       TEXT    NOT NULL,
                    description TEXT,
                    done        INTEGER NOT NULL DEFAULT 0
                )
                """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Table 'task' initialisée.");
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'initialiser la table 'task'", e);
        }
    }

    /**
     * Insère les données de démo uniquement si la table est vide.
     */
    private void seedData() {
        if (count() > 0) return;
        save(new Task(1, "Réviser DS de maths",      "Séries numériques et probabilités.", false));
        save(new Task(2, "Valider mon PIVE",          "PIVE Club Poker.",                  true));
        save(new Task(3, "Choisir mon parcours de 4A","SIR ou SIA ?",                      false));
        log.info("Données de démo insérées.");
    }

    /**
     * Insère ou remplace une Task (INSERT OR REPLACE).
     *
     * @param task tâche à persister.
     * @return la tâche telle qu'elle est stockée.
     */
    public Task save(Task task) {
        String sql = "INSERT OR REPLACE INTO task (id, title, description, done) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt    (1, task.id());
            ps.setString (2, task.title());
            ps.setString (3, task.description());
            ps.setInt    (4, task.done() ? 1 : 0);
            ps.executeUpdate();
            return task;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde de la tâche id=" + task.id(), e);
        }
    }

    /**
     * Recherche une Task par son identifiant.
     *
     * @param id identifiant de la tâche.
     * @return un Optional contenant la tâche, ou vide si introuvable.
     */
    public Optional<Task> findById(int id) {
        String sql = "SELECT * FROM task WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de la tâche id=" + id, e);
        }
        return Optional.empty();
    }

    /**
     * Retourne toutes les Task stockées.
     *
     * @return liste de toutes les tâches.
     */
    public ArrayList<Task> findAll() {
        String sql = "SELECT id, title, description, done FROM task";
        ArrayList<Task> tasks = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tasks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de toutes les tâches", e);
        }
        return tasks;
    }

    /**
     * Supprime une Task par son identifiant.
     *
     * @param id identifiant de la tâche à supprimer.
     */
    public void deleteById(int id) {
        String sql = "DELETE FROM task WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la tâche id=" + id, e);
        }
    }

    /**
     * Remplace une Task existante par une nouvelle version.
     *
     * @param id      identifiant de la tâche à modifier.
     * @param newTask nouvelles valeurs.
     */
    public void changeById(int id, Task newTask) {
        String sql = "UPDATE task SET title = ?, description = ?, done = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString (1, newTask.title());
            ps.setString (2, newTask.description());
            ps.setInt    (3, newTask.done() ? 1 : 0);
            ps.setInt    (4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification de la tâche id=" + id, e);
        }
    }

    /** Ouvre une connexion JDBC vers la base SQLite. */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /** Construit un objet Task depuis la ligne courante d'un ResultSet */
    private Task mapRow(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt    ("id"),
                rs.getString ("title"),
                rs.getString ("description"),
                rs.getInt    ("done") == 1
        );
    }

    /** Compte le nombre de lignes présentes dans la table. */
    private int count() {
        String sql = "SELECT COUNT(*) FROM task";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
}