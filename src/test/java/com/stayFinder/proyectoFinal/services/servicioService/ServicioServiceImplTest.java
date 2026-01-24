package com.stayFinder.proyectoFinal.services.servicioService;

import com.stayFinder.proyectoFinal.dto.inputDTO.ServicioRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.ServicioResponseDTO;
import com.stayFinder.proyectoFinal.entity.Servicio;
import com.stayFinder.proyectoFinal.mapper.ServicioMapper;
import com.stayFinder.proyectoFinal.repository.ServicioRepository;
import com.stayFinder.proyectoFinal.services.servicioService.implementations.ServicioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioServiceImplTest {

    @Mock
    private ServicioRepository servicioRepo;

    @Mock
    private ServicioMapper mapper;

    @InjectMocks
    private ServicioServiceImpl servicioService;

    private Servicio servicio;
    private ServicioRequestDTO requestDTO;
    private ServicioResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        servicio = Servicio.builder()
                .id(1L)
                .nombre("WiFi")
                .precio(50000.0)
                .build();

        requestDTO = new ServicioRequestDTO("WiFi", 50000.0);
        responseDTO = ServicioResponseDTO.builder()
                .id(1L)
                .nombre("WiFi")
                .precio(50000.0)
                .build();
    }

    @Test
    void crear_deberiaCrearServicioCuandoNoExiste() {
        when(servicioRepo.existsByNombre(requestDTO.nombre())).thenReturn(false);
        when(mapper.toEntity(requestDTO)).thenReturn(servicio);
        when(servicioRepo.save(servicio)).thenReturn(servicio);
        when(mapper.toDto(servicio)).thenReturn(responseDTO);

        ServicioResponseDTO result = servicioService.crear(requestDTO);

        assertNotNull(result);
        assertEquals("WiFi", result.getNombre());
        verify(servicioRepo, times(1)).save(servicio);
    }

    @Test
    void crear_deberiaLanzarExcepcionSiYaExiste() {
        when(servicioRepo.existsByNombre(requestDTO.nombre())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            servicioService.crear(requestDTO);
        });

        assertEquals("Ya existe un servicio con ese nombre", exception.getMessage());
    }

    @Test
    void editar_deberiaActualizarServicioExistente() {
        when(servicioRepo.findById(1L)).thenReturn(Optional.of(servicio));
        when(servicioRepo.save(servicio)).thenReturn(servicio);
        when(mapper.toDto(servicio)).thenReturn(responseDTO);

        ServicioResponseDTO result = servicioService.editar(1L, requestDTO);

        assertNotNull(result);
        assertEquals("WiFi", result.getNombre());
        verify(servicioRepo, times(1)).save(servicio);
    }

    @Test
    void editar_deberiaLanzarExcepcionSiNoExiste() {
        when(servicioRepo.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            servicioService.editar(1L, requestDTO);
        });

        assertEquals("Servicio no encontrado", exception.getMessage());
    }

    @Test
    void eliminar_deberiaEliminarServicioSiExiste() {
        when(servicioRepo.findById(1L)).thenReturn(Optional.of(servicio));

        servicioService.eliminar(1L);

        verify(servicioRepo, times(1)).delete(servicio);
    }

    @Test
    void listar_deberiaRetornarListaDeServicios() {
        when(servicioRepo.findAll()).thenReturn(List.of(servicio));
        when(mapper.toDto(servicio)).thenReturn(responseDTO);

        List<ServicioResponseDTO> result = servicioService.listar();

        assertEquals(1, result.size());
        assertEquals("WiFi", result.get(0).getNombre());
    }

    @Test
    void obtenerPorId_deberiaRetornarServicioSiExiste() {
        when(servicioRepo.findById(1L)).thenReturn(Optional.of(servicio));
        when(mapper.toDto(servicio)).thenReturn(responseDTO);

        ServicioResponseDTO result = servicioService.obtenerPorId(1L);

        assertNotNull(result);
        assertEquals("WiFi", result.getNombre());
    }

    @Test
    void obtenerPorId_deberiaLanzarExcepcionSiNoExiste() {
        when(servicioRepo.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            servicioService.obtenerPorId(1L);
        });

        assertEquals("Servicio no encontrado", exception.getMessage());
    }
}
