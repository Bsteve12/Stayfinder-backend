package com.stayFinder.proyectoFinal.services.solictudPublicacionService;

import com.stayFinder.proyectoFinal.dto.inputDTO.SolicitudPublicacionRequestDTO;
import com.stayFinder.proyectoFinal.dto.inputDTO.SolicitudPublicacionRespuestaRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.SolicitudPublicacionResponseDTO;
import com.stayFinder.proyectoFinal.entity.*;
import com.stayFinder.proyectoFinal.entity.enums.EstadoSolicitudPublicacion;
import com.stayFinder.proyectoFinal.entity.enums.Role;
import com.stayFinder.proyectoFinal.mapper.SolicitudPublicacionMapper;
import com.stayFinder.proyectoFinal.repository.*;
import com.stayFinder.proyectoFinal.services.solicitudPublicacionService.implementations.SolicitudPublicacionServiceImpl;
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

class SolicitudPublicacionImplTest {

    @Mock
    private SolicitudPublicacionRepository solicitudRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private AlojamientoRepository alojamientoRepo;

    @Mock
    private PublicacionRepository publicacionRepo;

    @Mock
    private SolicitudPublicacionMapper mapper;

    @InjectMocks
    private SolicitudPublicacionServiceImpl solicitudService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -----------------------------------------------------
    // TEST 1: Crear solicitud exitosamente
    // -----------------------------------------------------
    @Test
    void testCrearSolicitud_Success() {
        // Arrange
        SolicitudPublicacionRequestDTO request = new SolicitudPublicacionRequestDTO(1L, 2L, "Publicar mi alojamiento");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Carlos");
        usuario.setRole(Role.OWNER);

        Alojamiento alojamiento = new Alojamiento();
        alojamiento.setId(2L);
        alojamiento.setNombre("Casa Bonita");
        alojamiento.setDescripcion("Cómoda casa para estudiantes");

        SolicitudPublicacion solicitud = SolicitudPublicacion.builder()
                .id(10L)
                .usuario(usuario)
                .alojamiento(alojamiento)
                .comentario("Publicar mi alojamiento")
                .estado(EstadoSolicitudPublicacion.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .build();

        SolicitudPublicacionResponseDTO responseDTO = new SolicitudPublicacionResponseDTO(
                10L,
                "Carlos",
                "Casa Bonita",
                EstadoSolicitudPublicacion.PENDIENTE,
                "Publicar mi alojamiento",
                solicitud.getFechaSolicitud(),
                null
        );

        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuario));
        when(alojamientoRepo.findById(2L)).thenReturn(Optional.of(alojamiento));
        when(solicitudRepo.save(any(SolicitudPublicacion.class))).thenReturn(solicitud);
        when(mapper.toDto(solicitud)).thenReturn(responseDTO);

        // Act
        SolicitudPublicacionResponseDTO result = solicitudService.crearSolicitud(request);

        // Assert
        assertNotNull(result);
        assertEquals("Carlos", result.nombreUsuario());
        assertEquals(EstadoSolicitudPublicacion.PENDIENTE, result.estado());
        verify(solicitudRepo, times(1)).save(any(SolicitudPublicacion.class));
    }

    // -----------------------------------------------------
    // TEST 2: Error - Usuario no encontrado
    // -----------------------------------------------------
    @Test
    void testCrearSolicitud_UsuarioNoEncontrado() {
        SolicitudPublicacionRequestDTO request = new SolicitudPublicacionRequestDTO(99L, 2L, "Comentario");
        when(usuarioRepo.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> solicitudService.crearSolicitud(request));
        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    // -----------------------------------------------------
    // TEST 3: Error - Usuario no es OWNER
    // -----------------------------------------------------
    @Test
    void testCrearSolicitud_UsuarioNoEsOwner() {
        SolicitudPublicacionRequestDTO request = new SolicitudPublicacionRequestDTO(1L, 2L, "Comentario");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setRole(Role.CLIENT); //

        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuario));

        Exception ex = assertThrows(RuntimeException.class, () -> solicitudService.crearSolicitud(request));
        assertEquals("Solo un OWNER puede solicitar publicación", ex.getMessage());
    }

    // -----------------------------------------------------
    // TEST 4: Responder solicitud - aprobada por admin
    // -----------------------------------------------------
    @Test
    void testResponderSolicitud_Aprobada() {
        SolicitudPublicacionRespuestaRequestDTO request = new SolicitudPublicacionRespuestaRequestDTO(
                10L, 5L, true, "Aprobada correctamente"
        );

        Usuario admin = new Usuario();
        admin.setId(5L);
        admin.setRole(Role.ADMIN);

        Usuario owner = new Usuario();
        owner.setId(2L);
        owner.setRole(Role.OWNER);

        Alojamiento alojamiento = new Alojamiento();
        alojamiento.setId(3L);
        alojamiento.setNombre("Casa Bonita");
        alojamiento.setDescripcion("Cómoda casa para estudiantes");

        SolicitudPublicacion solicitud = new SolicitudPublicacion();
        solicitud.setId(10L);
        solicitud.setUsuario(owner);
        solicitud.setAlojamiento(alojamiento);
        solicitud.setEstado(EstadoSolicitudPublicacion.PENDIENTE);

        SolicitudPublicacionResponseDTO responseDTO = new SolicitudPublicacionResponseDTO(
                10L,
                "Carlos",
                "Casa Bonita",
                EstadoSolicitudPublicacion.APROBADA,
                "Aprobada correctamente",
                solicitud.getFechaSolicitud(),
                LocalDateTime.now()
        );

        when(solicitudRepo.findById(10L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepo.findById(5L)).thenReturn(Optional.of(admin));
        when(publicacionRepo.save(any(Publicacion.class))).thenReturn(new Publicacion());
        when(solicitudRepo.save(any(SolicitudPublicacion.class))).thenReturn(solicitud);
        when(mapper.toDto(solicitud)).thenReturn(responseDTO);

        SolicitudPublicacionResponseDTO result = solicitudService.responderSolicitud(request);

        assertNotNull(result);
        assertEquals(EstadoSolicitudPublicacion.APROBADA, result.estado());
        verify(publicacionRepo, times(1)).save(any(Publicacion.class));
    }

    // -----------------------------------------------------
    // TEST 5: Listar solicitudes pendientes
    // -----------------------------------------------------
    @Test
    void testListarPendientes_Success() {
        SolicitudPublicacion solicitud = new SolicitudPublicacion();
        solicitud.setId(1L);
        solicitud.setEstado(EstadoSolicitudPublicacion.PENDIENTE);

        SolicitudPublicacionResponseDTO response = new SolicitudPublicacionResponseDTO(
                1L,
                "Carlos",
                "Casa Bonita",
                EstadoSolicitudPublicacion.PENDIENTE,
                "Comentario",
                LocalDateTime.now(),
                null
        );

        when(solicitudRepo.findByEstado(EstadoSolicitudPublicacion.PENDIENTE)).thenReturn(List.of(solicitud));
        when(mapper.toDto(solicitud)).thenReturn(response);

        List<SolicitudPublicacionResponseDTO> result = solicitudService.listarPendientes();

        assertEquals(1, result.size());
        assertEquals(EstadoSolicitudPublicacion.PENDIENTE, result.get(0).estado());
        verify(solicitudRepo, times(1)).findByEstado(EstadoSolicitudPublicacion.PENDIENTE);
    }
}
