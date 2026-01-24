package com.stayFinder.proyectoFinal.services.mensajeService;

import com.stayFinder.proyectoFinal.dto.inputDTO.MensajeRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.MensajeResponseDTO;
import com.stayFinder.proyectoFinal.entity.Chat;
import com.stayFinder.proyectoFinal.entity.Mensaje;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.mapper.MensajeMapper;
import com.stayFinder.proyectoFinal.repository.ChatRepository;
import com.stayFinder.proyectoFinal.repository.MensajeRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.mensajeService.implementations.MensajeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MensajeServiceImplTest {

    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MensajeMapper mensajeMapper;

    @InjectMocks
    private MensajeServiceImpl mensajeService;

    private Usuario usuario;
    private Usuario anfitrion;
    private Chat chat;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        usuario = new Usuario();
        usuario.setId(1L);

        anfitrion = new Usuario();
        anfitrion.setId(2L);

        chat = new Chat();
        chat.setId(10L);
        chat.setUsuario(usuario);
        chat.setAnfitrion(anfitrion);
    }

    //  Caso 1: Enviar mensaje exitosamente
    @Test
    void enviarMensaje_deberiaGuardarYRetornarDTO() {
        MensajeRequestDTO dto = MensajeRequestDTO.builder()
                .chatId(10L)
                .remitenteId(1L)
                .contenido("Hola, ¿a qué hora es el check-in?")
                .build();

        Mensaje mensaje = Mensaje.builder()
                .id(5L)
                .contenido(dto.getContenido())
                .chat(chat)
                .remitente(usuario)
                .fechaEnvio(LocalDateTime.now())
                .build();

        MensajeResponseDTO response = MensajeResponseDTO.builder()
                .id(5L)
                .chatId(10L)
                .remitenteId(1L)
                .contenido("Hola, ¿a qué hora es el check-in?")
                .fechaEnvio(mensaje.getFechaEnvio())
                .build();

        when(chatRepository.findById(10L)).thenReturn(Optional.of(chat));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(mensaje);
        when(mensajeMapper.toDto(any(Mensaje.class))).thenReturn(response);

        MensajeResponseDTO resultado = mensajeService.enviarMensaje(dto);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getChatId());
        assertEquals(1L, resultado.getRemitenteId());
        assertEquals("Hola, ¿a qué hora es el check-in?", resultado.getContenido());
        verify(mensajeRepository, times(1)).save(any(Mensaje.class));
    }

    // Caso 2: Chat no encontrado
    @Test
    void enviarMensaje_chatNoEncontrado_deberiaLanzarExcepcion() {
        MensajeRequestDTO dto = new MensajeRequestDTO(99L, 1L, "Mensaje de prueba");

        when(chatRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mensajeService.enviarMensaje(dto));

        assertEquals("Chat no encontrado", ex.getMessage());
        verify(chatRepository).findById(99L);
    }

    // Caso 3: Remitente no encontrado
    @Test
    void enviarMensaje_remitenteNoEncontrado_deberiaLanzarExcepcion() {
        MensajeRequestDTO dto = new MensajeRequestDTO(10L, 3L, "Mensaje");

        when(chatRepository.findById(10L)).thenReturn(Optional.of(chat));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mensajeService.enviarMensaje(dto));

        assertEquals("Remitente no encontrado", ex.getMessage());
    }

    //  Caso 4: Remitente no pertenece al chat
    @Test
    void enviarMensaje_remitenteNoPerteneceAlChat_deberiaLanzarExcepcion() {
        Usuario otro = new Usuario();
        otro.setId(5L);

        MensajeRequestDTO dto = new MensajeRequestDTO(10L, 5L, "Hola!");

        when(chatRepository.findById(10L)).thenReturn(Optional.of(chat));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(otro));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mensajeService.enviarMensaje(dto));

        assertEquals("El remitente no pertenece a este chat", ex.getMessage());
    }

    //  Caso 5: Listar mensajes por chat
    @Test
    void listarMensajesPorChat_deberiaRetornarMensajesDelChat() {
        Mensaje mensaje1 = Mensaje.builder()
                .id(1L)
                .contenido("Hola")
                .chat(chat)
                .remitente(usuario)
                .fechaEnvio(LocalDateTime.now())
                .build();

        Mensaje mensaje2 = Mensaje.builder()
                .id(2L)
                .contenido("Buenas tardes")
                .chat(chat)
                .remitente(anfitrion)
                .fechaEnvio(LocalDateTime.now())
                .build();

        MensajeResponseDTO dto1 = MensajeResponseDTO.builder()
                .id(1L)
                .chatId(10L)
                .remitenteId(1L)
                .contenido("Hola")
                .build();

        MensajeResponseDTO dto2 = MensajeResponseDTO.builder()
                .id(2L)
                .chatId(10L)
                .remitenteId(2L)
                .contenido("Buenas tardes")
                .build();

        when(mensajeRepository.findAll()).thenReturn(List.of(mensaje1, mensaje2));
        when(mensajeMapper.toDto(mensaje1)).thenReturn(dto1);
        when(mensajeMapper.toDto(mensaje2)).thenReturn(dto2);

        List<MensajeResponseDTO> resultado = mensajeService.listarMensajesPorChat(10L);

        assertEquals(2, resultado.size());
        assertEquals("Hola", resultado.get(0).getContenido());
        assertEquals("Buenas tardes", resultado.get(1).getContenido());
    }
}
