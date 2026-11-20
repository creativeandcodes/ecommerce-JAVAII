package com.parquesoft.ecommerce.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parquesoft.ecommerce.entity.OrderEntity;
import com.parquesoft.ecommerce.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("createOrder: Debe guardar el pedido con estado PENDING cuando el monto es positivo")
    void testCreateOrder_Success() {

        // https://java-design-patterns.com/es/patterns/arrange-act-assert/
        // Arrange
        OrderEntity inputOrder = new OrderEntity();
        inputOrder.setCustomerId(1L);
        inputOrder.setTotalAmount(185000.50);
        inputOrder.setStatus("PAID"); // Intentamos forzar otro estado para probar Regla 2

        // Simulamos la base de datos

        /*Para no guardar basura en la base de datos real durante esta prueba,
         se usa Mockito (when... thenAnswer). Al intentar llamar al servicio mediante 
         orderRepository.save(...) con cualquier pedido, se intercepta esa llamada, 
         se finge que se guarda asignándo el ID 1L como en una base de datos real, 
         y devuélve lo creado. */

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act -> Para ejecutar la accion y probar el metodo createOrder
        OrderEntity result = orderService.createOrder(inputOrder);

        // Assert -> 
        // validar que no sea nulo
        assertNotNull(result);
        // Comprobar que el pedido devuelto tenga el ID 1L que se simuló en el repositorio
        assertEquals(1L, result.getId());
        //Verificar que aunque se puso al principio "PAID", el método createOrder aplica la regla de negocio correctamente y lo cambia obligatoriamente a "PENDING".
        assertEquals("PENDING", result.getStatus());
        //Verifica que el método haya asignado automáticamente una fecha de creación al pedido y no sea nulo.
        assertNotNull(result.getOrderDate());
        //Verificando que el monto del pedido no se haya alterado y sea positivo.
        assertEquals(185000.50, result.getTotalAmount());
        //Verificando que el servicio realmente intentó guardar el pedido en el repositorio exactamente 1 vez no mas de una vez por error.
        verify(orderRepository, times(1)).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("createOrder: Debe lanzar IllegalArgumentException si el monto es cero o negativo")
    void testCreateOrder_ThrowsException_WhenTotalIsZeroOrNegative() {
        // Arrange -> preparamos el escenario negativo con monto negativo
        OrderEntity invalidOrder = new OrderEntity();
        invalidOrder.setCustomerId(1L);
        invalidOrder.setTotalAmount(-10.0);


        // Act & Assert

        // Como se espera que el codigo falle antes de llegar a la base de datos no simulamos esa parte
        // Si el método createOrder no lanza un error y deja pasar el monto negativo la prueba falla.
	    // sí lo lanza, assertThrows atrapa el error, no se detiene la prueba y lo guarda en la variable exception para que poder revisarlo.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(invalidOrder);
        });
        //Comprobando que el código muestre exactamente el mensaje de error específico que le definimos, y no un error genérico.
        assertEquals("El monto total del pedido debe ser mayor a cero", exception.getMessage());
        //Se usa never(), para verificar que el método save del repositorio nunca fue llamado.
        //Confirmamos que la validación detuvo el proceso a tiempo y que el pedido de monto negativo nunca se guardo.
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("getOrderById: Debe retornar la orden cuando el ID existe")
    void testGetOrderById_Success() {
        // Arrange - > preparar el escenario
        //Simular acceso a la base de datos y returneme la orden como existente
        OrderEntity existingOrder = new OrderEntity(1L, 1L, LocalDateTime.now(), 250000.0, "PENDING");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));

        // Act -> probamos el metodo getOrderId() y guardamos el retorno en result
        Optional<OrderEntity> result = orderService.getOrderById(1L);

        // Assert
        //Verifica que result no esté vacío isPresent() devuelve verdadero si hay un objeto adentro. Si esta vacío, la prueba falla.
        assertTrue(result.isPresent());
        //Saca el pedido de result usando .get() y comprueba que su ID sea exactamente 1L.
        assertEquals(1L, result.get().getId());
        //Comprueba que el monto del pedido que obtuvimos sea igual que 250000.0 que se ingreso al principio de la prueba.
        assertEquals(250000.0, result.get().getTotalAmount());
        //Verificar que el servicio realmente consultó el repositorio buscando el ID 1L solo una unica vez.
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getOrderById: Debe retornar Optional.empty cuando el ID no existe")
    void testGetOrderById_NotFound() {
        // Arrange
        // Simulamos conexiona base de datos buscando un id con valor alto simulando que no existe y retornamos un dato vacio 
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        // Se llama al metodo getOrderById y guardamos retorno en result
        Optional<OrderEntity> result = orderService.getOrderById(99L);

        // Assert
        //Verificamos que result este vacio, si el servicio devolviera datos o algo por error, la prueba falla.
        assertTrue(result.isEmpty());
        //Confirma que el servicio le pregunto al repositorio por el ID 99L exactamente una vez y devolver el resultado vacío.
        verify(orderRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("cancelOrder: Debe lanzar IllegalStateException si la orden ya esta en estado SHIPPED")
    void testCancelOrder_ThrowsException_WhenAlreadyShipped() {
        // Arrange
        // Simulamos que un pedido en status "SHIPPED" fue encontrado 
        OrderEntity shippedOrder = new OrderEntity(1L, 1L, LocalDateTime.now(), 120000.0, "SHIPPED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(shippedOrder));

        // Act & Assert
        //se exige que el código detenga la operación y muestre exactamente una IllegalStateException "esta acción no está permitida en el estado actual del objeto".
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(1L));
        // se usa never(), para confirmar que el método save del repositorio nunca fue ejecutado.
	    //Garantizando que el pedido quedó intacto en la base de datos como "SHIPPED" y no se actualizo por error fallando la prueba.
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }
}