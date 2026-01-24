package com.stayFinder.proyectoFinal.services.publicacionService;

import com.stayFinder.proyectoFinal.dto.inputDTO.PublicacionRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.PublicacionResponseDTO;
import com.stayFinder.proyectoFinal.entity.Publicacion;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.entity.enums.EstadoSolicitudPublicacion;
import com.stayFinder.proyectoFinal.entity.enums.Role;
import com.stayFinder.proyectoFinal.mapper.PublicacionMapper;
import com.stayFinder.proyectoFinal.repository.PublicacionRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.publicacionService.implementations.PublicacionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicacionServiceImplTest {

    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PublicacionMapper mapper;

    @InjectMocks
    private PublicacionServiceImpl publicacionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    //  Test: crear publicación con rol CLIENT
    @Test
    void crearPublicacion_conCliente_deberiaLanzarExcepcion() {
        // given
        PublicacionRequestDTO dto = new PublicacionRequestDTO();
        dto.setUsuarioId(1L);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setRole(Role.CLIENT);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // when
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> publicacionService.crearPublicacion(dto));

        // then
        assertEquals("Un cliente no puede crear publicaciones. Solo OWNER o ADMIN.", ex.getMessage());
        verify(usuarioRepository).findById(1L);
        verifyNoInteractions(publicacionRepository);
    }

    //  Test: crear publicación con rol OWNER → queda PENDIENTE
    @Test
    void crearPublicacion_conOwner_deberiaGuardarConEstadoPendiente() {
        PublicacionRequestDTO dto = new PublicacionRequestDTO();
        dto.setUsuarioId(2L);
        dto.setTitulo("Casa Campestre");
        dto.setDescripcion("Bonita casa con piscina");

        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setRole(Role.OWNER);

        Publicacion publicacion = new Publicacion();
        publicacion.setTitulo(dto.getTitulo());
        publicacion.setDescripcion(dto.getDescripcion());

        Publicacion publicacionGuardada = new Publicacion();
        publicacionGuardada.setId(100L);
        publicacionGuardada.setTitulo(dto.getTitulo());
        publicacionGuardada.setEstado(EstadoSolicitudPublicacion.PENDIENTE);

        PublicacionResponseDTO response = new PublicacionResponseDTO();
        response.setId(100L);
        response.setTitulo("Casa Campestre");
        response.setEstado(EstadoSolicitudPublicacion.PENDIENTE);

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(mapper.toEntity(dto)).thenReturn(publicacion);
        when(publicacionRepository.save(any(Publicacion.class))).thenReturn(publicacionGuardada);
        when(mapper.toDto(publicacionGuardada)).thenReturn(response);

        PublicacionResponseDTO resultado = publicacionService.crearPublicacion(dto);

        assertEquals(EstadoSolicitudPublicacion.PENDIENTE, resultado.getEstado());
        verify(publicacionRepository).save(any(Publicacion.class));
    }

    //  Test: crear publicación con rol ADMIN → queda APROBADA
    @Test
    void crearPublicacion_conAdmin_deberiaGuardarConEstadoAprobada() {
        PublicacionRequestDTO dto = new PublicacionRequestDTO();
        dto.setUsuarioId(3L);

        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setRole(Role.ADMIN);

        Publicacion publicacion = new Publicacion();
        publicacion.setTitulo("Hotel de Lujo");

        Publicacion publicacionGuardada = new Publicacion();
        publicacionGuardada.setId(200L);
        publicacionGuardada.setEstado(EstadoSolicitudPublicacion.APROBADA);

        PublicacionResponseDTO response = new PublicacionResponseDTO();
        response.setId(200L);
        response.setEstado(EstadoSolicitudPublicacion.APROBADA);

        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(mapper.toEntity(dto)).thenReturn(publicacion);
        when(publicacionRepository.save(any(Publicacion.class))).thenReturn(publicacionGuardada);
        when(mapper.toDto(publicacionGuardada)).thenReturn(response);

        PublicacionResponseDTO resultado = publicacionService.crearPublicacion(dto);

        assertEquals(EstadoSolicitudPublicacion.APROBADA, resultado.getEstado());
        verify(publicacionRepository).save(any(Publicacion.class));
    }

    //  Test: listar publicaciones pendientes
    @Test
    void listarPendientes_deberiaRetornarListaDePendientes() {
        Publicacion publicacion = new Publicacion();
        publicacion.setEstado(EstadoSolicitudPublicacion.PENDIENTE);

        PublicacionResponseDTO dto = new PublicacionResponseDTO();
        dto.setEstado(EstadoSolicitudPublicacion.PENDIENTE);

        when(publicacionRepository.findByEstado(EstadoSolicitudPublicacion.PENDIENTE))
                .thenReturn(List.of(publicacion));
        when(mapper.toDto(publicacion)).thenReturn(dto);

        List<PublicacionResponseDTO> resultado = publicacionService.listarPendientes();

        assertEquals(1, resultado.size());
        assertEquals(EstadoSolicitudPublicacion.PENDIENTE, resultado.get(0).getEstado());
    }

    //  Test: aprobar publicación pendiente
    @Test
    void aprobarPublicacion_deberiaCambiarEstadoAAprobada() {
        Publicacion publicacion = new Publicacion();
        publicacion.setEstado(EstadoSolicitudPublicacion.PENDIENTE);

        Publicacion publicacionGuardada = new Publicacion();
        publicacionGuardada.setEstado(EstadoSolicitudPublicacion.APROBADA);

        PublicacionResponseDTO dto = new PublicacionResponseDTO();
        dto.setEstado(EstadoSolicitudPublicacion.APROBADA);

        when(publicacionRepository.findById(1L)).thenReturn(Optional.of(publicacion));
        when(publicacionRepository.save(any(Publicacion.class))).thenReturn(publicacionGuardada);
        when(mapper.toDto(publicacionGuardada)).thenReturn(dto);

        PublicacionResponseDTO resultado = publicacionService.aprobarPublicacion(1L);

        assertEquals(EstadoSolicitudPublicacion.APROBADA, resultado.getEstado());
    }

    //  Test: rechazar publicación pendiente
    @Test
    void rechazarPublicacion_deberiaCambiarEstadoARechazada() {
        Publicacion publicacion = new Publicacion();
        publicacion.setEstado(EstadoSolicitudPublicacion.PENDIENTE);

        Publicacion publicacionGuardada = new Publicacion();
        publicacionGuardada.setEstado(EstadoSolicitudPublicacion.RECHAZADA);

        PublicacionResponseDTO dto = new PublicacionResponseDTO();
        dto.setEstado(EstadoSolicitudPublicacion.RECHAZADA);

        when(publicacionRepository.findById(1L)).thenReturn(Optional.of(publicacion));
        when(publicacionRepository.save(any(Publicacion.class))).thenReturn(publicacionGuardada);
        when(mapper.toDto(publicacionGuardada)).thenReturn(dto);

        PublicacionResponseDTO resultado = publicacionService.rechazarPublicacion(1L);

        assertEquals(EstadoSolicitudPublicacion.RECHAZADA, resultado.getEstado());
    }
}
