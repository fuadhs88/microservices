package com.codingsaint.microservices.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codingsaint.microservices.model.Category;
import com.codingsaint.microservices.model.Task;
import com.codingsaint.microservices.repository.CategoryRepository;
import com.codingsaint.microservices.repository.TaskRepository;

@RestController
@RefreshScope
public class TaskController {

	private final Logger logger = LoggerFactory.getLogger(TaskController.class);

	private TaskRepository taskRepository;
	private CategoryRepository categoryRepository;
	private KafkaTemplate<String,String> kafkaTemplate;
	

	public TaskController(TaskRepository taskRepository, CategoryRepository categoryRepository,KafkaTemplate<String,String> kafkaTemplate) {
		this.taskRepository = taskRepository;
		this.categoryRepository = categoryRepository;
		this.kafkaTemplate = kafkaTemplate;
	}
	@Value("${application.version}")
	private String applicationVersion;
	
	@GetMapping("application/version")
	public String applicationVersion() {
		return applicationVersion;
	}
	@PostMapping("task")
	@Transactional
	public ResponseEntity<Task> addTasks(@RequestBody Task task){
		logger.info("Task {}" ,task);
		Set<Category> categories=task.getCategories();
		Set<Category> taskCategories= new HashSet<>();
		
		categories.stream().forEach(category->{
			Category existingCategory= categoryRepository.findByName(category.getName());
			if(existingCategory==null) {
				existingCategory=categoryRepository.save(category);
				taskCategories.add(existingCategory);
			}
			existingCategory.setTasks(new HashSet<>());
			taskCategories.add(existingCategory);
		});
		task.setCategories(taskCategories);
		Task savedTask= taskRepository.save(task);
		return new ResponseEntity<Task>(savedTask,HttpStatus.CREATED);
	}

	@GetMapping("user/{id}/tasks")
	public List<Task> userTasks(@PathVariable ("id") Long userId){
		logger.info("Task for user id {}" ,userId);		
		return taskRepository.findByUserId(userId);
	}
	
	@PostMapping("send")
	public ResponseEntity<?> send(@RequestParam ("message") String message){
		kafkaTemplate.send("tasks",message);
		return new ResponseEntity<>(HttpStatus.OK);
		
	}
		
}
