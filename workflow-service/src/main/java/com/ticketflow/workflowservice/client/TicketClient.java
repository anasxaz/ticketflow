package com.ticketflow.workflowservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "ticket-service")
public interface TicketClient {
    @PatchMapping("/api/tickets/{id}/assign")
    void assign(@PathVariable("id") Long id, @RequestBody Map<String, Long> body);

    @PatchMapping("/api/tickets/{id}/status")
    void updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> body);

    @PatchMapping("/api/tickets/{id}/triage")
    Map<String, Object> triage(@PathVariable("id") Long id, @RequestBody Map<String, Object> body);
}