package service;

import com.example.todoapp.Task;
import dao.TaskDao;

import java.util.ArrayList;
import java.util.Optional;


public class TaskService {
    private final TaskDao dao = new TaskDao();

    public Task save(Task task) {
        return dao.save(task);
    }

    public Optional<Task> findById(int id) {
        return dao.findById(id);
    }

    public ArrayList<Task> findAll() {
        return dao.findAll();
    }

    public void deleteById(int id) {
        dao.deleteById(id);
    }

    public void changeById(int id, Task newTask) {
        dao.changeById(id, newTask);
    }    
}