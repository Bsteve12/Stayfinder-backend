package com.stayFinder.proyectoFinal.services.solictudOwnerService;

import com.stayFinder.proyectoFinal.dto.inputDTO.RespuestaSolicitudRequestDTO;
import com.stayFinder.proyectoFinal.dto.inputDTO.SolicitudOwnerRequestDTO;
import com.stayFinder.proyectoFinal.dto.inputDTO.RespuestaSolicitudRequestDTO;
import com.stayFinder.proyectoFinal.dto.inputDTO.SolicitudOwnerRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.SolicitudOwnerResponseDTO;
import com.stayFinder.proyectoFinal.entity.SolicitudOwner;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.entity.enums.EstadoSolicitud;
import com.stayFinder.proyectoFinal.entity.enums.Role;
import com.stayFinder.proyectoFinal.mapper.SolicitudOwnerMapper;
import com.stayFinder.proyectoFinal.repository.SolicitudOwnerRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.solicitudOwnerService.implementations.SolicitudOwnerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolicitudOwnerServiceImplTest {

    @Mock
    private SolicitudOwnerRepository solicitudRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private SolicitudOwnerMapper mapper;

    @Mock
    private MultipartFile archivoMock;

    @InjectMocks
    private SolicitudOwnerServiceImpl service;

    private Usuario usuarioCliente;
    private Usuario usuarioAdmin;
    private SolicitudOwner solicitud;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        usuarioCliente = Usuario.builder()
                .id(1L)
                .nombre("Juan Pérez")
                .role(Role.CLIENT)
                .build();

        usuarioAdmin = Usuario.builder()
                .id(2L)
                .nombre("Admin")
                .role(Role.ADMIN)
                .build();

        solicitud = SolicitudOwner.builder()
                .id(1L)
                .usuario(usuarioCliente)
                .estado(EstadoSolicitud.PENDIENTE)
                .comentario("Solicitud para ser owner")
                .fechaSolicitud(LocalDateTime.now())
                .build();
    }

    @Test
    void crearSolicitud_DeberiaCrearCorrectamente() throws Exception {
        // Arrange
        SolicitudOwnerRequestDTO dto = new SolicitudOwnerRequestDTO(usuarioCliente.getId(), "Quiero ser propietario");
        when(usuarioRepo.findById(usuarioCliente.getId())).thenReturn(Optional.of(usuarioCliente));
        when(solicitudRepo.findByEstado(EstadoSolicitud.PENDIENTE)).thenReturn(List.of());
        when(solicitudRepo.save(any())).thenReturn(solicitud);
        when(mapper.toDto(any())).thenReturn(new SolicitudOwnerResponseDTO(
                1L, "Juan Pérez", EstadoSolicitud.PENDIENTE, "Solicitud para ser owner",
                null, LocalDateTime.now(), null
        ));

        // Act
        SolicitudOwnerResponseDTO result = service.crearSolicitud(dto, archivoMock);

        // Assert
        assertNotNull(result);
        assertEquals("Juan Pérez", result.nombreUsuario());
        verify(solicitudRepo).save(any(SolicitudOwner.class));
    }

    @Test
    void crearSolicitud_DeberiaFallarSiUsuarioNoEsCliente() {
        // Arrange
        Usuario noCliente = Usuario.builder()
                .id(3L)
                .role(Role.OWNER)
                .build();

        SolicitudOwnerRequestDTO dto = new SolicitudOwnerRequestDTO(noCliente.getId(), "No debería poder");
        when(usuarioRepo.findById(noCliente.getId())).thenReturn(Optional.of(noCliente));

        // Act & Assert
        Exception ex = assertThrows(Exception.class, () -> service.crearSolicitud(dto, archivoMock));
        assertEquals("Solo usuarios con rol CLIENT pueden enviar solicitudes", ex.getMessage());
    }

    @Test
    void responderSolicitud_DeberiaAprobarCorrectamente() throws Exception {
        // Arrange
        solicitud.setFechaSolicitud(LocalDateTime.now().minusDays(1));
        RespuestaSolicitudRequestDTO dto = new RespuestaSolicitudRequestDTO(
                solicitud.getId(), usuarioAdmin.getId(), true , "Aprobado"
        );

        when(solicitudRepo.findById(solicitud.getId())).thenReturn(Optional.of(solicitud));
        when(usuarioRepo.findById(usuarioAdmin.getId())).thenReturn(Optional.of(usuarioAdmin));
        when(mapper.toDto(any())).thenReturn(new SolicitudOwnerResponseDTO(
                1L, "Juan Pérez", EstadoSolicitud.APROBADA, "Aprobado", null,
                solicitud.getFechaSolicitud(), LocalDateTime.now()
        ));

        // Act
        SolicitudOwnerResponseDTO result = service.responderSolicitud(dto);

        // Assert
        assertEquals(EstadoSolicitud.APROBADA, result.estado());
        verify(solicitudRepo).save(any(SolicitudOwner.class));
        verify(usuarioRepo).save(any(Usuario.class));
    }

    @Test
    void responderSolicitud_DeberiaRechazarPorVencimiento() throws Exception {
        // Arrange
        solicitud.setFechaSolicitud(LocalDateTime.now().minusDays(5));
        RespuestaSolicitudRequestDTO dto = new RespuestaSolicitudRequestDTO(
                solicitud.getId(), usuarioAdmin.getId(), true, "fuera de plazo"
        );

        when(solicitudRepo.findById(solicitud.getId())).thenReturn(Optional.of(solicitud));
        when(usuarioRepo.findById(usuarioAdmin.getId())).thenReturn(Optional.of(usuarioAdmin));
        when(mapper.toDto(any())).thenReturn(new SolicitudOwnerResponseDTO(
                1L, "Juan Pérez", EstadoSolicitud.RECHAZADA, "Fuera de plazo", null,
                solicitud.getFechaSolicitud(), LocalDateTime.now()
        ));

        // Act
        SolicitudOwnerResponseDTO result = service.responderSolicitud(dto);

        // Assert
        assertEquals(EstadoSolicitud.RECHAZADA, result.estado());
        verify(solicitudRepo).save(any(SolicitudOwner.class));
    }

    @Test
    void listarSolicitudesPendientes_DeberiaRetornarLista() throws Exception {
        // Arrange
        when(solicitudRepo.findByEstado(EstadoSolicitud.PENDIENTE))
                .thenReturn(List.of(solicitud));
        when(mapper.toDto(solicitud))
                .thenReturn(new SolicitudOwnerResponseDTO(1L, "Juan Pérez",
                        EstadoSolicitud.PENDIENTE, "Solicitud pendiente", null,
                        LocalDateTime.now(), null));

        // Act
        List<SolicitudOwnerResponseDTO> result = service.listarSolicitudesPendientes();

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(EstadoSolicitud.PENDIENTE, result.get(0).estado());
    }
}
