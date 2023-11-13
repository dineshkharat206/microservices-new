package com.microservices.orderservice.service;

import com.microservices.orderservice.dto.InventoryResponse;
import com.microservices.orderservice.dto.OrderLineItemsDTO;
import com.microservices.orderservice.dto.OrderRequest;
import com.microservices.orderservice.model.Order;
import com.microservices.orderservice.model.OrderLineItems;
import com.microservices.orderservice.repository.IOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final IOrderRepository orderRepository;

    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest)
    {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapOrderLineItemsToOrderLineItemsDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

        // Call Inventory Service and place order if the product is in Stock.
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                                        .uri("http://inventory-service/api/inventory",
                                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                                        .retrieve()
                                        .bodyToMono(InventoryResponse[].class).block();

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);

        if(allProductsInStock)
        {
            orderRepository.save(order);
        }
        else
        {
            throw new IllegalArgumentException("Product is not in stock, Please try again later.");
        }


    }

    private OrderLineItems mapOrderLineItemsToOrderLineItemsDto(OrderLineItemsDTO orderLineItemsDTO) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDTO.getPrice());
        orderLineItems.setQuantity(orderLineItemsDTO.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDTO.getSkuCode());

        return orderLineItems;
    }
}
