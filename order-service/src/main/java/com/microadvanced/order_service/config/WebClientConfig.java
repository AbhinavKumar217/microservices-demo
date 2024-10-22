package com.microadvanced.order_service.config;

import com.microadvanced.order_service.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
@FeignClient(name = "inventory-service")
public interface WebClientConfig {
    @GetMapping("/api/inventory")
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode);
}
