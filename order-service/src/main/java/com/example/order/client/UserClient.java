package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${clients.user-service.url}")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserDTO getUser(@PathVariable String id);

    record UserDTO(String id, String email, String name, String status) {}
}