package com.ticketflow.userservice.controller;

import com.ticketflow.userservice.model.Role;
import com.ticketflow.userservice.model.User;
import com.ticketflow.userservice.repository.UserRepository;
import com.ticketflow.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping
    public List<User> getAll(){
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id){
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@Valid @RequestBody User user){
        return userService.create(user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        userService.delete(id);
    }

    @GetMapping("/by-email")
    public User getByEmail(@RequestParam String email) {
        return userService.findByEmail(email);
    }

    @GetMapping("/agents")
    public List<User> getAgents(){
        return userRepository.findByRole(Role.AGENT);
    }

}
