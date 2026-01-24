package com.stayFinder.proyectoFinal.services.chatService;

import com.stayFinder.proyectoFinal.dto.inputDTO.ChatRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.ChatResponseDTO;
import com.stayFinder.proyectoFinal.entity.Chat;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.entity.enums.EstadoReserva;
import com.stayFinder.proyectoFinal.mapper.ChatMapper;
import com.stayFinder.proyectoFinal.repository.ChatRepository;
import com.stayFinder.proyectoFinal.repository.ReservaRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.chatService.implementations.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceImplTest {

    @Mock
    private ChatRepository chatRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private ReservaRepository reservaRepo;
    @Mock
    private ChatMapper chatMapper;

    @InjectMocks
    private ChatServiceImpl chatService;

    private Usuario usuario;
    private Usuario anfitrion;
    private Chat chat;
    private ChatRequestDTO dto;
    private ChatResponseDTO chatResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        usuario = Usuario.builder().id(1L).nombre("Cliente").build();
        anfitrion = Usuario.builder().id(2L).nombre("Anfitrión").build();

        dto = ChatRequestDTO.builder()
                .usuarioId(usuario.getId())
                .anfitrionId(anfitrion.getId())
                .build();

        chat = Chat.builder()
                .id(10L)
                .usuario(usuario)
                .anfitrion(anfitrion)
                .build();

        chatResponseDTO = ChatResponseDTO.builder()
                .id(chat.getId())
                .usuarioId(usuario.getId())
                .anfitrionId(anfitrion.getId())
                .build();
    }

    // ✅ Caso exitoso: se crea un chat con reserva confirmada
    @Test
    void crearChat_conReservaConfirmada_deberiaCrearChat() {
        when(usuarioRepo.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarioRepo.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(reservaRepo.existsByUsuarioAndAlojamientoOwnerAndEstado(usuario, anfitrion, EstadoReserva.CONFIRMADA))
                .thenReturn(true);
        when(chatMapper.toDto(any(Chat.class))).thenReturn(chatResponseDTO);

        ChatResponseDTO resultado = chatService.crearChat(dto);

        assertNotNull(resultado);
        assertEquals(chatResponseDTO.getUsuarioId(), resultado.getUsuarioId());
        assertEquals(chatResponseDTO.getAnfitrionId(), resultado.getAnfitrionId());
        verify(chatRepo, times(1)).save(any(Chat.class));
    }

    // ❌ Caso de error: usuario no encontrado
    @Test
    void crearChat_usuarioNoEncontrado_deberiaLanzarExcepcion() {
        when(usuarioRepo.findById(usuario.getId())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chatService.crearChat(dto));
        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(chatRepo, never()).save(any());
    }

    // ❌ Caso de error: anfitrión no encontrado
    @Test
    void crearChat_anfitrionNoEncontrado_deberiaLanzarExcepcion() {
        when(usuarioRepo.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarioRepo.findById(anfitrion.getId())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chatService.crearChat(dto));
        assertEquals("Anfitrión no encontrado", ex.getMessage());
        verify(chatRepo, never()).save(any());
    }

    // ❌ Caso de error: no hay reserva confirmada entre el usuario y el anfitrión
    @Test
    void crearChat_sinReservaConfirmada_deberiaLanzarExcepcion() {
        when(usuarioRepo.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarioRepo.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(reservaRepo.existsByUsuarioAndAlojamientoOwnerAndEstado(usuario, anfitrion, EstadoReserva.CONFIRMADA))
                .thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chatService.crearChat(dto));
        assertEquals("No se puede abrir chat sin reserva confirmada", ex.getMessage());
        verify(chatRepo, never()).save(any());
    }

    // ✅ Caso listarChats: debe devolver lista de chats mapeados
    @Test
    void listarChats_deberiaDevolverListaDeChats() {
        when(chatRepo.findAll()).thenReturn(List.of(chat));
        when(chatMapper.toDto(chat)).thenReturn(chatResponseDTO);

        List<ChatResponseDTO> resultado = chatService.listarChats();

        assertEquals(1, resultado.size());
        assertEquals(chatResponseDTO.getUsuarioId(), resultado.get(0).getUsuarioId());
        verify(chatRepo, times(1)).findAll();
    }
}
