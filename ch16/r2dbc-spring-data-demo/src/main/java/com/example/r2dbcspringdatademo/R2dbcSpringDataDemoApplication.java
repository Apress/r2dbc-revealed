package com.example.r2dbcspringdatademo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableR2dbcRepositories
public class R2dbcSpringDataDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(R2dbcSpringDataDemoApplication.class, args);
	}

}

@RestController
@RequestMapping("/tasks")
class TasksController {

    @Autowired
    private TaskService service;

    @GetMapping()
    public ResponseEntity<Flux<Task>> get() {
        return ResponseEntity.ok(this.service.getAllTasks());
    }

    @PostMapping()
    public ResponseEntity<Mono<Task>> post(@RequestBody Task task) {
        if (service.isValid(task)) {
            return ResponseEntity.ok(this.service.createTask(task));
        }
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
    }

    @PutMapping()
    public ResponseEntity<Mono<Task>> put(@RequestBody Task task) {
        if (service.isValid(task)) {
            return ResponseEntity.ok(this.service.updateTask(task));
        }
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
    }

    @PutMapping("/updatestatus")
    public ResponseEntity<Mono<Integer>> updateStatus(@RequestParam int id, @RequestParam Boolean completed) {
        if (id > 0) {
            return ResponseEntity.ok(this.service.updateTaskStatusById(id,completed));
        }
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
    }

    @DeleteMapping()
    public ResponseEntity<Mono<Void>> delete(@RequestParam int id) {
        if (id > 0) {
            return ResponseEntity.ok(this.service.deleteTask(id));
        }
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
    }
}

@Service
class TaskService {

    @Autowired
    private TasksRepository repository;

    public Boolean isValid(final Task task) {
        if (task != null && !task.getDescription().isEmpty()) {
            return true;
        }
        return false;
	}
	
    public Flux<Task> getAllTasks() {
        return this.repository.findAll();
    }
	
    public Mono<Task> createTask(final Task task) {
        return this.repository.save(task);
    }

    @Transactional
    public Mono<Task> updateTask(final Task task) {

        return this.repository.findById(task.getId())
                .flatMap(t -> {
                    t.setDescription(task.getDescription());
                    t.setCompleted(task.getCompleted());
                    return this.repository.save(t);
                });
	}
    
	public Mono<Integer> updateTaskStatusById(Integer id, Boolean completed) {
		return this.repository.updateStatus(id, completed);
	}
	
    @Transactional
    public Mono<Void> deleteTask(final int id){
        return this.repository.findById(id)
                .flatMap(this.repository::delete);
    }
}

interface TasksRepository extends ReactiveCrudRepository<Task, Integer> {	
	@Modifying
	@Query("UPDATE tasks SET completed = :completed WHERE id = :id")
	Mono<Integer> updateStatus(Integer id, Boolean completed);
}

@Data
@RequiredArgsConstructor
@Table("tasks")
class Task {
	@Id
	private Integer id;
	@NonNull
	private String description;
	private Boolean completed;
}