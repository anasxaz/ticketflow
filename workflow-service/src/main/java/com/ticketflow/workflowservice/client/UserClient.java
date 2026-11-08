package com.ticketflow.workflowservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {
    record UserResponse(Long id, String name, String email, String role) {}

    @GetMapping("/api/users/agents")
    List<UserResponse> getAgents();
}