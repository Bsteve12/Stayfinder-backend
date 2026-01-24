package com.stayFinder.proyectoFinal.services.reservaService;

import com.stayFinder.proyectoFinal.dto.inputDTO.ReservaRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.ReservaResponseDTO;
import com.stayFinder.proyectoFinal.entity.Alojamiento;
import com.stayFinder.proyectoFinal.entity.Reserva;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.entity.enums.EstadoReserva;
import com.stayFinder.proyectoFinal.entity.enums.TipoReserva;
import com.stayFinder.proyectoFinal.entity.enums.Role; // Importado
import com.stayFinder.proyectoFinal.mapper.ReservaMapper;
import com.stayFinder.proyectoFinal.repository.AlojamientoRepository;
import com.stayFinder.proyectoFinal.repository.ReservaRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.emailService.interfaces.EmailServiceInterface;
import com.stayFinder.proyectoFinal.services.reservaService.implementations.ReservaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservaServiceImplTest {

    // Constantes para IDs de Negocio/Token
    private static final Long USUARIO_ID_NEGOCIO = 100L;
    private static final Long ADMIN_ID_NEGOCIO = 999L;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AlojamientoRepository alojamientoRepository;

    @Mock
    private ReservaMapper reservaMapper;

    @Mock
    private EmailServiceInterface emailService;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Usuario cliente; // Cambiado a 'cliente' para mayor claridad
    private Usuario admin;   // Nuevo objeto Admin
    private Alojamiento alojamiento;
    private Reserva reserva;
    private ReservaRequestDTO requestDTO;
    private ReservaResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // CONFIGURACIÓN DEL CLIENTE (Actor principal en las pruebas)
        cliente = new Usuario();
        cliente.setId(1L); // PK de DB (usado en el DTO de salida)
        cliente.setUsuarioId(USUARIO_ID_NEGOCIO); // ID de Negocio (usado por el servicio en findByUsuarioId)
        cliente.setNombre("Allison");
        cliente.setEmail("allison@test.com");
        cliente.setRole(Role.CLIENT); // Rol requerido por obtenerReservaValida

        // CONFIGURACIÓN DEL ADMINISTRADOR
        admin = new Usuario();
        admin.setUsuarioId(ADMIN_ID_NEGOCIO);
        admin.setRole(Role.ADMIN);

        alojamiento = new Alojamiento();
        alojamiento.setId(2L);
        alojamiento.setNombre("Casa Café");
        alojamiento.setCapacidadMaxima(4);
        alojamiento.setPrecio(100000.0);

        reserva = Reserva.builder()
                .id(10L)
                .usuario(cliente) // Usamos cliente
                .alojamiento(alojamiento)
                .fechaInicio(LocalDate.now().plusDays(3))
                .fechaFin(LocalDate.now().plusDays(5))
                .numeroHuespedes(2)
                .estado(EstadoReserva.PENDIENTE)
                .tipoReserva(TipoReserva.SENCILLA)
                .precioTotal(190000.0)
                .build();

        // El DTO de entrada debe usar el ID de negocio/token si tu servicio lo espera.
        requestDTO = new ReservaRequestDTO(
                cliente.getUsuarioId(), // *** USAMOS EL ID DE NEGOCIO ***
                alojamiento.getId(),
                reserva.getFechaInicio(),
                reserva.getFechaFin(),
                reserva.getNumeroHuespedes(),
                reserva.getTipoReserva()
        );

        responseDTO = new ReservaResponseDTO(
                reserva.getId(),
                cliente.getId(), // Usamos la PK de DB para el DTO de salida (asumiendo que es lo que mapea el mapper)
                alojamiento.getId(),
                reserva.getFechaInicio(),
                reserva.getFechaFin(),
                reserva.getNumeroHuespedes(),
                reserva.getPrecioTotal(),
                reserva.getEstado()
        );
    }

    @Test
    void createReserva_deberiaCrearReservaConExito() throws Exception {
        // CORREGIDO: El servicio usa findByUsuarioId (ID de negocio)
        when(usuarioRepository.findByUsuarioId(USUARIO_ID_NEGOCIO)).thenReturn(Optional.of(cliente));
        when(alojamientoRepository.findById(alojamiento.getId())).thenReturn(Optional.of(alojamiento));
        when(reservaMapper.toEntity(requestDTO)).thenReturn(reserva);
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);
        when(reservaMapper.toDto(reserva)).thenReturn(responseDTO);

        ReservaResponseDTO result = reservaService.createReserva(requestDTO, USUARIO_ID_NEGOCIO); // ID de negocio

        assertNotNull(result);
        assertEquals(cliente.getId(), result.usuarioId()); // El DTO de salida retorna la PK de DB
        verify(emailService, times(1)).sendReservationConfirmation(
                eq(cliente.getEmail()), anyString(), anyString()
        );
    }

    @Test
    void createReserva_deberiaLanzarExcepcionSiUsuarioNoExiste() {
        // CORREGIDO: El servicio usa findByUsuarioId (ID de negocio)
        when(usuarioRepository.findByUsuarioId(USUARIO_ID_NEGOCIO)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () ->
                reservaService.createReserva(requestDTO, USUARIO_ID_NEGOCIO)
        );

        assertEquals("Usuario no existe", exception.getMessage());
    }

    @Test
    void confirmarReserva_deberiaCambiarEstadoYEnviarCorreo() throws Exception {
        reserva.setEstado(EstadoReserva.PENDIENTE);
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        // CORREGIDO: Se debe simular la búsqueda del actor (cliente) por su ID de negocio
        when(usuarioRepository.findByUsuarioId(USUARIO_ID_NEGOCIO)).thenReturn(Optional.of(cliente));

        when(reservaRepository.save(reserva)).thenReturn(reserva);

        reservaService.confirmarReserva(reserva.getId(), USUARIO_ID_NEGOCIO); // ID de negocio

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        verify(emailService, times(1)).sendReservationConfirmation(
                eq(cliente.getEmail()), anyString(), anyString()
        );
    }

    // El método deleteReserva sin userId no usa obtenerReservaValida, por lo que pasa si existe.
    @Test
    void deleteReserva_deberiaEliminarReservaExistente() throws Exception {
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        reservaService.deleteReserva(reserva.getId());

        verify(reservaRepository, times(1)).delete(reserva);
    }

    @Test
    void obtenerReservasUsuario_deberiaRetornarListaDeReservas() throws Exception {
        // CORREGIDO: El servicio usa findByUsuarioId (ID de negocio)
        when(usuarioRepository.findByUsuarioId(USUARIO_ID_NEGOCIO)).thenReturn(Optional.of(cliente));

        // El repositorio busca por el ID de negocio según la implementación
        when(reservaRepository.findByUsuarioId(USUARIO_ID_NEGOCIO)).thenReturn(List.of(reserva));
        when(reservaMapper.toDto(reserva)).thenReturn(responseDTO);

        List<ReservaResponseDTO> result = reservaService.obtenerReservasUsuario(USUARIO_ID_NEGOCIO);

        assertEquals(1, result.size());
        assertEquals(reserva.getId(), result.get(0).id());
    }

    // Pruebas findById no cambian porque solo dependen de la PK de la Reserva.

    @Test
    void findById_deberiaRetornarReservaSiExiste() {
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(reservaMapper.toDto(reserva)).thenReturn(responseDTO);

        Optional<ReservaResponseDTO> result = reservaService.findById(reserva.getId());

        assertTrue(result.isPresent());
        assertEquals(reserva.getId(), result.get().id());
    }

    @Test
    void findById_deberiaRetornarVacioSiNoExiste() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<ReservaResponseDTO> result = reservaService.findById(99L);

        assertTrue(result.isEmpty());
    }

    // Nueva prueba de seguridad/permisos para validar obtenerReservaValida
    @Test
    void confirmarReserva_deberiaPermitirAccesoAAdmin() throws Exception {
        reserva.setEstado(EstadoReserva.PENDIENTE);
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        // Simular que el actor es el ADMIN (ID de negocio)
        when(usuarioRepository.findByUsuarioId(ADMIN_ID_NEGOCIO)).thenReturn(Optional.of(admin));

        when(reservaRepository.save(reserva)).thenReturn(reserva);

        // Intentar confirmar usando el ID del ADMIN
        reservaService.confirmarReserva(reserva.getId(), ADMIN_ID_NEGOCIO);

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        verify(emailService, times(1)).sendReservationConfirmation(
                eq(cliente.getEmail()), anyString(), anyString()
        );
    }
}