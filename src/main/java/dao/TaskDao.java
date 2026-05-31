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
        create("Réviser DS de maths",       "Séries numériques et probabilités.");
        create("Valider mon PIVE",           "PIVE Club Poker.");
        create("Choisir mon parcours de 4A", "SIR ou SIA ?");
        // Marque la 2e tâche comme terminée
        String sql = "UPDATE task SET done = 1 WHERE title = 'Valider mon PIVE'";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            log.warn("Impossible de marquer la tâche de démo comme terminée.", e);
        }
        log.info("Données de démo insérées.");
    }

    /**
     * Insère une nouvelle tâche et laisse SQLite générer l'id.
     *
     * @param title       titre de la nouvelle tâche.
     * @param description description de la nouvelle tâche.
     * @return la tâche créée avec son id auto-généré.
     */
    public Task create(String title, String description) {
        String sql = "INSERT INTO task (title, description, done) VALUES (?, ?, 0)";
        try (Connection conn = getConnection() ; PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    return new Task(generatedId, title, description, false);
                }
            }
            throw new RuntimeException("Aucun id généré après l'insertion de la tâche.");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création de la tâche.", e);
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
     * Met à jour le titre, la description et le statut d'une {@link Task} existante.
     *
     * @param id          identifiant de la tâche à modifier.
     * @param title       nouveau titre.
     * @param description nouvelle description.
     * @param done        nouveau statut.
     */
    public void updateById(int id, String title, String description, boolean done) {
        String sql = "UPDATE task SET title = ?, description = ?, done = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setInt   (3, done ? 1 : 0);
            ps.setInt   (4, id);
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
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
}