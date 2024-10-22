package com.microadvanced.order_service.service;

import com.microadvanced.order_service.config.WebClientConfig;
import com.microadvanced.order_service.dto.InventoryResponse;
import com.microadvanced.order_service.dto.OrderLineItemsDto;
import com.microadvanced.order_service.dto.OrderRequest;
import com.microadvanced.order_service.entity.Order;
import com.microadvanced.order_service.entity.OrderLineItems;
import com.microadvanced.order_service.event.OrderPlacedEvent;
import com.microadvanced.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClientConfig webClientConfig;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToEntity)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        List<InventoryResponse> inventoryResponseArray = webClientConfig.isInStock(skuCodes);

        assert inventoryResponseArray != null;
        boolean allProductsInStock = inventoryResponseArray.stream()
                .allMatch(InventoryResponse::isInStock);

        if (allProductsInStock) {
            orderRepository.save(order);
            log.info("Order saved with id : {}",order.getId());
            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
            return "Order placed successfully";
        } else {
            throw new IllegalArgumentException("Product is not is stock, please try later.");
        }
    }

    private OrderLineItems mapToEntity(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
