package com.parquesoft.ecommerce.service;

import com.parquesoft.ecommerce.entity.OrderEntity;
import com.parquesoft.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderEntity createOrder(OrderEntity order) {
        // Regla 1: Validación de monto positivo
        if (order.getTotalAmount() == null || order.getTotalAmount() <= 0) {
            throw new IllegalArgumentException("El monto total del pedido debe ser mayor a cero");
        }

        // Regla 2: Asignación de estado inicial forzado a PENDING
        order.setStatus("PENDING");

        // Regla 3: Fecha de registro automatizada si no es provista
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<OrderEntity> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional
    public OrderEntity updateOrderStatus(Long id, String newStatus) {
        OrderEntity existing = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + id));

        existing.setStatus(newStatus.trim().toUpperCase());
        return orderRepository.save(existing);
    }

    @Transactional
    public void cancelOrder(Long id) {
        // Regla 5: Validar existencia y que no esté en estado SHIPPED
        OrderEntity existing = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + id));

        if ("SHIPPED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("No se puede cancelar una orden que ya fue enviada (SHIPPED)");
        }

        existing.setStatus("CANCELLED");
        orderRepository.save(existing);
    }
}