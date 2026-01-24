package com.stayFinder.proyectoFinal.services.alojamientoService;

import com.stayFinder.proyectoFinal.dto.inputDTO.AlojamientoRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.AlojamientoResponseDTO;
import com.stayFinder.proyectoFinal.entity.Alojamiento;
import com.stayFinder.proyectoFinal.entity.Reserva;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.entity.enums.EstadoReserva;
import com.stayFinder.proyectoFinal.entity.enums.Role;
import com.stayFinder.proyectoFinal.repository.AlojamientoRepository;
import com.stayFinder.proyectoFinal.repository.ReservaRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.alojamientoService.implementations.AlojamientoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class AlojamientoServiceImplTest {

    @Mock
    private AlojamientoRepository alojamientoRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private ReservaRepository reservaRepo;

    @InjectMocks
    private AlojamientoServiceImpl alojamientoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Test para el método crear
    @Test
    void testCrear_Success() {
        // Arrange
        Long ownerId = 1L;
        AlojamientoRequestDTO request = new AlojamientoRequestDTO("Nombre", "Direccion", 100.0, "Descripcion", 4);
        Usuario owner = new Usuario();
        owner.setId(ownerId);
        owner.setRole(Role.OWNER);

        when(usuarioRepo.findById(ownerId)).thenReturn(Optional.of(owner));
        when(alojamientoRepo.save(any(Alojamiento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AlojamientoResponseDTO result = alojamientoService.crear(request, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals("Nombre", result.nombre());
        verify(usuarioRepo, times(1)).findById(ownerId);
        verify(alojamientoRepo, times(1)).save(any(Alojamiento.class));
    }

    @Test
    void testCrear_UserNotFound() {
        // Arrange
        Long ownerId = 1L;
        AlojamientoRequestDTO request = new AlojamientoRequestDTO("Nombre", "Direccion", 100.0, "Descripcion", 4);

        when(usuarioRepo.findById(ownerId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> alojamientoService.crear(request, ownerId));
        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(usuarioRepo, times(1)).findById(ownerId);
        verify(alojamientoRepo, never()).save(any(Alojamiento.class));
    }

    // Test para el método obtenerPorId
    @Test
    void testObtenerPorId_Success() {
        // Arrange
        Long alojamientoId = 1L;
        Long ownerId = 2L;
        Alojamiento alojamiento = new Alojamiento();
        alojamiento.setId(alojamientoId);
        alojamiento.setEliminado(false);

        Usuario owner = new Usuario();
        owner.setId(ownerId);
        alojamiento.setOwner(owner);

        when(alojamientoRepo.findById(alojamientoId)).thenReturn(Optional.of(alojamiento));

        // Act
        AlojamientoResponseDTO result = alojamientoService.obtenerPorId(alojamientoId);

        // Assert
        assertNotNull(result);
        assertEquals(alojamientoId, result.id());
        assertEquals(ownerId, result.ownerId());
        verify(alojamientoRepo, times(1)).findById(alojamientoId);
    }

    @Test
    void testObtenerPorId_NotFound() {
        // Arrange
        Long alojamientoId = 1L;

        when(alojamientoRepo.findById(alojamientoId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> alojamientoService.obtenerPorId(alojamientoId));
        assertEquals("Alojamiento no encontrado", exception.getMessage());
        verify(alojamientoRepo, times(1)).findById(alojamientoId);
    }

    // Test para el método listarAlojamientosActivos
    @Test
    void testListarAlojamientosActivos_Success() {
        // Arrange
        Alojamiento alojamiento = new Alojamiento();
        alojamiento.setId(1L);
        alojamiento.setEliminado(false);

        // Crear un owner para el alojamiento
        Usuario owner = new Usuario();
        owner.setId(2L);
        alojamiento.setOwner(owner);

        when(alojamientoRepo.findByEliminadoFalse()).thenReturn(List.of(alojamiento));

        // Act
        List<AlojamientoResponseDTO> result = alojamientoService.listarAlojamientosActivos();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).ownerId());
        verify(alojamientoRepo, times(1)).findByEliminadoFalse();
    }

    @Test
    void testListarAlojamientosActivos_Empty() {
        when(alojamientoRepo.findByEliminadoFalse()).thenReturn(Collections.emptyList());
        List<AlojamientoResponseDTO> result = alojamientoService.listarAlojamientosActivos();
        assertTrue(result.isEmpty());
    }


    // Test para el método eliminar
    @Test
    void testEliminar_Success() {
        // Arrange
        Long alojamientoId = 1L;
        Long ownerId = 2L;
        Alojamiento alojamiento = new Alojamiento();
        alojamiento.setId(alojamientoId);

        Usuario owner = new Usuario();
        owner.setId(ownerId);
        alojamiento.setOwner(owner);

        when(alojamientoRepo.findById(alojamientoId)).thenReturn(Optional.of(alojamiento));
        when(reservaRepo.findByAlojamientoIdAndEstado(alojamientoId, EstadoReserva.CONFIRMADA)).thenReturn(Collections.emptyList());

        // Act
        alojamientoService.eliminar(alojamientoId, ownerId);

        // Assert
        verify(alojamientoRepo, times(1)).findById(alojamientoId);
        verify(alojamientoRepo, times(1)).save(alojamiento);
        assertTrue(alojamiento.isEliminado());
    }

    @Test
    void testEliminar_ReservasFuturas() {
        // Arrange
        Long alojamientoId = 1L;
        Long ownerId = 2L;
        Alojamiento alojamiento = new Alojamiento();
        alojamiento.setId(alojamientoId);

        Usuario owner = new Usuario();
        owner.setId(ownerId);
        alojamiento.setOwner(owner);

        // Crear una reserva simulada
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setAlojamiento(alojamiento);
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        when(alojamientoRepo.findById(alojamientoId)).thenReturn(Optional.of(alojamiento));
        when(reservaRepo.findByAlojamientoIdAndEstado(alojamientoId, EstadoReserva.CONFIRMADA)).thenReturn(List.of(reserva));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> alojamientoService.eliminar(alojamientoId, ownerId));
        assertEquals("No puedes eliminar un alojamiento con reservas confirmadas futuras", exception.getMessage());
        verify(alojamientoRepo, times(1)).findById(alojamientoId);
        verify(alojamientoRepo, never()).save(any(Alojamiento.class));
    }

    @Test
    void testCrear_RolNoPermitido() {
        // Arrange
        Long ownerId = 1L;
        AlojamientoRequestDTO request = new AlojamientoRequestDTO("Nombre", "Direccion", 100.0, "Descripcion", 4);
        Usuario owner = new Usuario();
        owner.setId(ownerId);
        owner.setRole(Role.CLIENT); // Rol no permitido

        when(usuarioRepo.findById(ownerId)).thenReturn(Optional.of(owner));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> alojamientoService.crear(request, ownerId));
        assertEquals("Solo OWNERS o ADMIN pueden crear alojamientos", exception.getMessage());
        verify(usuarioRepo, times(1)).findById(ownerId);
        verify(alojamientoRepo, never()).save(any(Alojamiento.class));
    }
}