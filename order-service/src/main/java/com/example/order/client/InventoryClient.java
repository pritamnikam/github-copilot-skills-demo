package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", url = "${clients.inventory-service.url}")
public interface InventoryClient {

    @PostMapping("/api/inventory/reserve")
    ReservationDTO reserve(@RequestBody ReserveRequest request);

    @DeleteMapping("/api/inventory/reserve/{reservationId}")
    void release(@PathVariable String reservationId);

    record ReserveRequest(String productId, Integer quantity, String orderId) {}
    record ReservationDTO(String reservationId, String productId,
                          Integer quantity, String expiresAt) {}
}