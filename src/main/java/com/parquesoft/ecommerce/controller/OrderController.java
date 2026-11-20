package com.parquesoft.ecommerce.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parquesoft.ecommerce.entity.OrderEntity;
import com.parquesoft.ecommerce.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Gestión de pedidos de clientes")
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Crear una nueva orden de compra", description = "Registra un nuevo pedido forzando el estado inicial a PENDING")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orden creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o monto menor o igual a cero")
    })
    public ResponseEntity<?> createOrder(@RequestBody OrderEntity order) {
        try {
            OrderEntity created = orderService.createOrder(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Listar todas las órdenes registradas")
    @ApiResponse(responseCode = "200", description = "Lista de órdenes obtenida correctamente")
    public ResponseEntity<List<OrderEntity>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener el detalle de una orden por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden encontrada"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    public ResponseEntity<OrderEntity> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Consultar pedidos de un cliente específico")
    @ApiResponse(responseCode = "200", description = "Historial de pedidos del cliente obtenido correctamente")
    public ResponseEntity<List<OrderEntity>> getOrdersByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filtrar pedidos según su estado")
    @ApiResponse(responseCode = "200", description = "Órdenes filtradas por estado obtenidas correctamente")
    public ResponseEntity<List<OrderEntity>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar el estado del pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado de la orden actualizado"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    public ResponseEntity<OrderEntity> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            OrderEntity updated = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar una orden de compra", description = "Cambia el estado de la orden a CANCELLED si no ha sido enviada (SHIPPED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Orden cancelada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada"),
            @ApiResponse(responseCode = "409", description = "No se puede cancelar una orden en estado SHIPPED")
    })

    // Retornar un tipo de objeto comodin con ? porque puede devolver tres tipos de respuesta
    // https://stackoverflow.com/questions/30797805/understanding-a-captured-type-in-java-symbol
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            orderService.cancelOrder(id);
            // retorna un cuerpo de respuesta completamente vacío un código 204 No Content o sin contenido como se especifico en el primer responseCode. 
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // Retorna un código 404 Not Found, sin embargo el cuerpo tambien va vacío.
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // https://contabo.com/blog/es/codigos-de-respuesta-http-y-estados-del-servidor-referencia-completa/?gad_source=1&gad_campaignid=24377674930&gbraid=0AAAAAD_Qy-eRkzg2W_Frib627n1TwRLwR&gclid=Cj0KCQiA2frXBhC_ARIsAPTFcdMyPzbcEb7OwbGag8aR3fWGC4K7fBaT7x5HxD8uwjNgb7VQ6-uCDTUaAvGSEALw_wcB
            // Retorna un codigo de conflicto 409 Conflict y envia un mensaje de error en el cuerpo con e.getMessage() que es String.
            // por que si esta en estado SHIPPED debe ser capturado ese caso
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}