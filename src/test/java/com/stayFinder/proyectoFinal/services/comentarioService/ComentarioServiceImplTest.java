package com.stayFinder.proyectoFinal.services.comentarioService;

import com.stayFinder.proyectoFinal.dto.inputDTO.ComentarioRequestDTO;
import com.stayFinder.proyectoFinal.dto.inputDTO.RespuestaRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.ComentarioResponseDTO;
import com.stayFinder.proyectoFinal.entity.*;
import com.stayFinder.proyectoFinal.exceptionHandling.advices.ForbiddenException;
import com.stayFinder.proyectoFinal.exceptionHandling.advices.ResourceNotFoundException;
import com.stayFinder.proyectoFinal.repository.ComentarioRepository;
import com.stayFinder.proyectoFinal.repository.ReservaRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.comentarioService.implementations.ComentarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComentarioServiceImplTest {

    @Mock
    private ComentarioRepository comentarioRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ComentarioServiceImpl comentarioService;

    private Usuario usuario;
    private Usuario anfitrion;
    private Alojamiento alojamiento;
    private Reserva reserva;
    private Comentario comentario;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        usuario = Usuario.builder()
                .id(1L)
                .nombre("Cliente")
                .build();

        anfitrion = Usuario.builder()
                .id(2L)
                .nombre("Anfitrión")
                .build();

        alojamiento = Alojamiento.builder()
                .id(5L)
                .nombre("Casa Cafetera")
                .owner(anfitrion)
                .build();

        reserva = Reserva.builder()
                .id(3L)
                .usuario(usuario)
                .alojamiento(alojamiento)
                .fechaFin(LocalDate.now().minusDays(1)) // ya terminó
                .build();

        comentario = Comentario.builder()
                .id(10L)
                .usuario(usuario)
                .alojamiento(alojamiento)
                .reserva(reserva)
                .mensaje("Excelente estancia")
                .calificacion(5)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    //  Caso exitoso: crear comentario correctamente
    @Test
    void crearComentario_correcto_deberiaGuardarYRetornarDTO() {
        ComentarioRequestDTO dto = ComentarioRequestDTO.builder()
                .reservaId(reserva.getId())
                .mensaje("Muy buena atención")
                .calificacion(5)
                .build();

        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(comentarioRepository.findByReserva(reserva)).thenReturn(Optional.empty());

        ComentarioResponseDTO resultado = comentarioService.crearComentario(usuario.getId(), dto);

        assertNotNull(resultado);
        assertEquals("Cliente", resultado.nombreUsuario());
        assertEquals("Anfitrión", resultado.nombreAnfitrion());
        verify(comentarioRepository, times(1)).save(any(Comentario.class));
    }

    //  Error: reserva no encontrada
    @Test
    void crearComentario_reservaNoEncontrada_deberiaLanzarExcepcion() {
        ComentarioRequestDTO dto = ComentarioRequestDTO.builder()
                .reservaId(99L)
                .mensaje("Nada")
                .calificacion(4)
                .build();

        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> comentarioService.crearComentario(usuario.getId(), dto));
        verify(comentarioRepository, never()).save(any());
    }

    //  Error: otro usuario intenta comentar una reserva ajena
    @Test
    void crearComentario_reservaDeOtroUsuario_deberiaLanzarForbidden() {
        ComentarioRequestDTO dto = ComentarioRequestDTO.builder()
                .reservaId(reserva.getId())
                .mensaje("Buen sitio")
                .calificacion(5)
                .build();

        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        assertThrows(ForbiddenException.class,
                () -> comentarioService.crearComentario(999L, dto)); // otro user
        verify(comentarioRepository, never()).save(any());
    }

    //  Error: reserva aún no finalizada
    @Test
    void crearComentario_reservaNoFinalizada_deberiaLanzarForbidden() {
        reserva.setFechaFin(LocalDate.now().plusDays(2));

        ComentarioRequestDTO dto = ComentarioRequestDTO.builder()
                .reservaId(reserva.getId())
                .mensaje("Buen sitio")
                .calificacion(5)
                .build();

        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        assertThrows(ForbiddenException.class,
                () -> comentarioService.crearComentario(usuario.getId(), dto));
        verify(comentarioRepository, never()).save(any());
    }

    //  Error: ya existe un comentario para esa reserva
    @Test
    void crearComentario_yaExisteComentario_deberiaLanzarForbidden() {
        ComentarioRequestDTO dto = ComentarioRequestDTO.builder()
                .reservaId(reserva.getId())
                .mensaje("Repetido")
                .calificacion(3)
                .build();

        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(comentarioRepository.findByReserva(reserva)).thenReturn(Optional.of(comentario));

        assertThrows(ForbiddenException.class,
                () -> comentarioService.crearComentario(usuario.getId(), dto));
        verify(comentarioRepository, never()).save(any());
    }

    //  Caso exitoso: responder comentario
    @Test
    void responderComentario_correcto_deberiaGuardarRespuesta() {
        RespuestaRequestDTO dto = RespuestaRequestDTO.builder()
                .comentarioId(comentario.getId())
                .mensajeRespuesta("Gracias por su opinión")
                .build();

        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));

        ComentarioResponseDTO respuesta = comentarioService.responderComentario(anfitrion.getId(), dto);

        assertNotNull(respuesta);
        assertEquals("Gracias por su opinión", comentario.getRespuestaAnfitrion());
        verify(comentarioRepository, times(1)).save(comentario);
    }

    //  Error: comentario no encontrado
    @Test
    void responderComentario_comentarioNoEncontrado_deberiaLanzarExcepcion() {
        RespuestaRequestDTO dto = RespuestaRequestDTO.builder()
                .comentarioId(999L)
                .mensajeRespuesta("Hola")
                .build();

        when(comentarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> comentarioService.responderComentario(anfitrion.getId(), dto));
        verify(comentarioRepository, never()).save(any());
    }

    //  Error: usuario no encontrado al responder
    @Test
    void responderComentario_usuarioNoEncontrado_deberiaLanzarExcepcion() {
        RespuestaRequestDTO dto = RespuestaRequestDTO.builder()
                .comentarioId(comentario.getId())
                .mensajeRespuesta("Gracias")
                .build();

        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> comentarioService.responderComentario(anfitrion.getId(), dto));
        verify(comentarioRepository, never()).save(any());
    }

    //  Error: un usuario que no es el anfitrión intenta responder
    @Test
    void responderComentario_noEsAnfitrion_deberiaLanzarForbidden() {
        RespuestaRequestDTO dto = RespuestaRequestDTO.builder()
                .comentarioId(comentario.getId())
                .mensajeRespuesta("Soy intruso")
                .build();

        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.of(Usuario.builder().id(999L).build()));

        assertThrows(ForbiddenException.class,
                () -> comentarioService.responderComentario(999L, dto));
        verify(comentarioRepository, never()).save(any());
    }

    //  Eliminar comentario correctamente
    @Test
    void eliminarComentario_correcto_deberiaEliminar() {
        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));

        comentarioService.eliminarComentario(comentario.getId(), usuario.getId());

        verify(comentarioRepository, times(1)).delete(comentario);
    }

    //  Error: comentario no encontrado al eliminar
    @Test
    void eliminarComentario_noEncontrado_deberiaLanzarExcepcion() {
        when(comentarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> comentarioService.eliminarComentario(999L, usuario.getId()));
        verify(comentarioRepository, never()).delete(any());
    }

    //  Error: otro usuario intenta eliminar comentario
    @Test
    void eliminarComentario_otroUsuario_deberiaLanzarForbidden() {
        when(comentarioRepository.findById(comentario.getId())).thenReturn(Optional.of(comentario));

        assertThrows(ForbiddenException.class,
                () -> comentarioService.eliminarComentario(comentario.getId(), 999L));
        verify(comentarioRepository, never()).delete(any());
    }
}
