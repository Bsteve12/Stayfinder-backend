package com.stayFinder.proyectoFinal.services.pagoService;

import com.stayFinder.proyectoFinal.dto.inputDTO.PagoRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.PagoResponseDTO;
import com.stayFinder.proyectoFinal.entity.Pago;
import com.stayFinder.proyectoFinal.entity.Reserva;
import com.stayFinder.proyectoFinal.mapper.PagoMapper;
import com.stayFinder.proyectoFinal.repository.PagoRepository;
import com.stayFinder.proyectoFinal.repository.ReservaRepository;
import com.stayFinder.proyectoFinal.services.pagoService.implementations.PagoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private PagoMapper mapper;

    @InjectMocks
    private PagoServiceImpl pagoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -----------------------------------------------------
    // TEST 1: Registrar pago exitosamente
    // -----------------------------------------------------
    @Test
    void testRegistrarPago_Success() {
        // Arrange
        PagoRequestDTO request = new PagoRequestDTO();
        request.setReservaId(1L);
        request.setMetodoPago("Tarjeta");
        request.setMonto(100.0);

        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setPrecioTotal(100.0);

        Pago pagoGuardado = Pago.builder()
                .id(1L)
                .reserva(reserva)
                .metodo("Tarjeta")
                .monto(100.0)
                .estado("PAGADO")
                .fecha(LocalDateTime.now())
                .build();

        PagoResponseDTO pagoResponseDTO = new PagoResponseDTO(
                1L,
                1L,
                "Tarjeta",
                100.0,
                "PAGADO",
                LocalDateTime.now()
        );

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoGuardado);
        when(mapper.toDto(pagoGuardado)).thenReturn(pagoResponseDTO);

        // Act
        PagoResponseDTO result = pagoService.registrarPago(request);

        // Assert
        assertNotNull(result);
        assertEquals(100.0, result.getMonto());
        assertEquals("Tarjeta", result.getMetodoPago());
        assertEquals("PAGADO", result.getEstado());
        verify(pagoRepository, times(1)).save(any(Pago.class));
    }

    // -----------------------------------------------------
    // TEST 2: Error - Reserva no encontrada
    // -----------------------------------------------------
    @Test
    void testRegistrarPago_ReservaNoEncontrada() {
        // Arrange
        PagoRequestDTO request = new PagoRequestDTO();
        request.setReservaId(99L);
        request.setMonto(100.0);

        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> pagoService.registrarPago(request));
        assertEquals("Reserva no encontrada", exception.getMessage());
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    // -----------------------------------------------------
    // TEST 3: Error - Monto no coincide
    // -----------------------------------------------------
    @Test
    void testRegistrarPago_MontoNoCoincide() {
        // Arrange
        PagoRequestDTO request = new PagoRequestDTO();
        request.setReservaId(1L);
        request.setMonto(80.0);

        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setPrecioTotal(100.0);

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> pagoService.registrarPago(request));
        assertEquals("El monto del pago no coincide con el precio de la reserva", exception.getMessage());
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    // -----------------------------------------------------
    // TEST 4: Listar todos los pagos
    // -----------------------------------------------------
    @Test
    void testListarPagos_Success() {
        // Arrange
        Pago pago = Pago.builder()
                .id(1L)
                .metodo("Tarjeta")
                .monto(150.0)
                .estado("PAGADO")
                .fecha(LocalDateTime.now())
                .build();

        PagoResponseDTO response = new PagoResponseDTO(
                1L,
                2L,
                "Tarjeta",
                150.0,
                "PAGADO",
                LocalDateTime.now()
        );

        when(pagoRepository.findAll()).thenReturn(List.of(pago));
        when(mapper.toDto(pago)).thenReturn(response);

        // Act
        List<PagoResponseDTO> result = pagoService.listarPagos();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Tarjeta", result.get(0).getMetodoPago());
        verify(pagoRepository, times(1)).findAll();
    }

    // -----------------------------------------------------
    // TEST 5: Obtener pago por ID exitosamente
    // -----------------------------------------------------
    @Test
    void testObtenerPagoPorId_Success() {
        // Arrange
        Pago pago = Pago.builder()
                .id(1L)
                .metodo("Efectivo")
                .monto(200.0)
                .estado("PAGADO")
                .fecha(LocalDateTime.now())
                .build();

        PagoResponseDTO response = new PagoResponseDTO(
                1L,
                3L,
                "Efectivo",
                200.0,
                "PAGADO",
                LocalDateTime.now()
        );

        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));
        when(mapper.toDto(pago)).thenReturn(response);

        // Act
        PagoResponseDTO result = pagoService.obtenerPagoPorId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(200.0, result.getMonto());
        verify(pagoRepository, times(1)).findById(1L);
    }

    // -----------------------------------------------------
    // TEST 6: Pago no encontrado por ID
    // -----------------------------------------------------
    @Test
    void testObtenerPagoPorId_NotFound() {
        // Arrange
        when(pagoRepository.findById(10L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> pagoService.obtenerPagoPorId(10L));
        assertEquals("Pago no encontrado", exception.getMessage());
    }
}
