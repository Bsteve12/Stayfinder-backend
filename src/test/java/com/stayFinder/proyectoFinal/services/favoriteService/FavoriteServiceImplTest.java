package com.stayFinder.proyectoFinal.services.favoriteService;

import com.stayFinder.proyectoFinal.dto.inputDTO.FavoriteRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.FavoriteResponseDTO;
import com.stayFinder.proyectoFinal.entity.Alojamiento;
import com.stayFinder.proyectoFinal.entity.Favorite;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.mapper.FavoriteMapper;
import com.stayFinder.proyectoFinal.repository.AlojamientoRepository;
import com.stayFinder.proyectoFinal.repository.FavoriteRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.favoriteService.implementations.FavoriteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FavoriteServiceImplTest {

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlojamientoRepository alojamientoRepository;
    @Mock
    private FavoriteMapper favoriteMapper;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    private Usuario usuario;
    private Alojamiento alojamiento;
    private Favorite favorite;
    private FavoriteRequestDTO requestDTO;
    private FavoriteResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Allison");

        alojamiento = new Alojamiento();
        alojamiento.setId(2L);
        alojamiento.setNombre("Casa Café");

        favorite = Favorite.builder()
                .id(10L)
                .usuario(usuario)
                .alojamiento(alojamiento)
                .build();

        requestDTO = new FavoriteRequestDTO(1L, 2L);

        responseDTO = new FavoriteResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setUsuarioId(1L);
        responseDTO.setAlojamientoId(2L);
    }

    // TEST 1: Agregar favorito correctamente
    @Test
    void addFavorite_deberiaGuardarYRetornarDTO() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(alojamientoRepository.findById(2L)).thenReturn(Optional.of(alojamiento));
        when(favoriteRepository.findByUsuarioAndAlojamiento(usuario, alojamiento)).thenReturn(Optional.empty());
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(favorite);
        when(favoriteMapper.toDto(favorite)).thenReturn(responseDTO);

        FavoriteResponseDTO resultado = favoriteService.addFavorite(requestDTO);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getUsuarioId());
        assertEquals(2L, resultado.getAlojamientoId());
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
    }

    // TEST 2: Agregar favorito duplicado
    @Test
    void addFavorite_cuandoYaExiste_deberiaLanzarExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(alojamientoRepository.findById(2L)).thenReturn(Optional.of(alojamiento));
        when(favoriteRepository.findByUsuarioAndAlojamiento(usuario, alojamiento))
                .thenReturn(Optional.of(favorite));

        Exception ex = assertThrows(Exception.class, () -> favoriteService.addFavorite(requestDTO));
        assertEquals("Alojamiento ya está en favoritos", ex.getMessage());
    }

    //  TEST 3: Eliminar favorito con usuario no autorizado
    @Test
    void removeFavorite_conUsuarioIncorrecto_deberiaLanzarExcepcion() {
        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(99L);
        favorite.setUsuario(usuario);

        when(favoriteRepository.findById(10L)).thenReturn(Optional.of(favorite));

        Exception ex = assertThrows(Exception.class, () -> favoriteService.removeFavorite(10L, 99L));
        assertEquals("No tienes permisos para eliminar este favorito", ex.getMessage());
    }

    // TEST 4: Eliminar favorito correctamente
    @Test
    void removeFavorite_correctamente() throws Exception {
        favorite.setUsuario(usuario);
        when(favoriteRepository.findById(10L)).thenReturn(Optional.of(favorite));

        favoriteService.removeFavorite(10L, 1L);

        verify(favoriteRepository, times(1)).delete(favorite);
    }

    // TEST 5: Listar favoritos de un usuario
    @Test
    void listFavoritesByUsuario_deberiaRetornarLista() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(favoriteRepository.findByUsuario(usuario)).thenReturn(List.of(favorite));
        when(favoriteMapper.toDto(favorite)).thenReturn(responseDTO);

        List<FavoriteResponseDTO> lista = favoriteService.listFavoritesByUsuario(1L);

        assertEquals(1, lista.size());
        assertEquals(2L, lista.get(0).getAlojamientoId());
    }

    // TEST 6: Validar si un alojamiento es favorito
    @Test
    void isFavorito_cuandoExiste_deberiaRetornarTrue() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(alojamientoRepository.findById(2L)).thenReturn(Optional.of(alojamiento));
        when(favoriteRepository.findByUsuarioAndAlojamiento(usuario, alojamiento))
                .thenReturn(Optional.of(favorite));

        boolean resultado = favoriteService.isFavorito(1L, 2L);

        assertTrue(resultado);
    }

    // TEST 7: Validar si no es favorito
    @Test
    void isFavorito_cuandoNoExiste_deberiaRetornarFalse() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(alojamientoRepository.findById(2L)).thenReturn(Optional.of(alojamiento));
        when(favoriteRepository.findByUsuarioAndAlojamiento(usuario, alojamiento))
                .thenReturn(Optional.empty());

        boolean resultado = favoriteService.isFavorito(1L, 2L);

        assertFalse(resultado);
    }
}
