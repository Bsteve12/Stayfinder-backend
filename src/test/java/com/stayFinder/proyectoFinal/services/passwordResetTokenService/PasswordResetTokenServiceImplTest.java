package com.stayFinder.proyectoFinal.services.passwordResetTokenService;

import com.stayFinder.proyectoFinal.dto.inputDTO.PasswordResetTokenRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.PasswordResetTokenResponseDTO;
import com.stayFinder.proyectoFinal.entity.PasswordResetToken;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.mapper.PasswordResetTokenMapper;
import com.stayFinder.proyectoFinal.repository.PasswordResetTokenRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.passwordTokenService.implementations.PasswordResetTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordResetTokenServiceImplTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetTokenMapper mapper;

    @InjectMocks
    private PasswordResetTokenServiceImpl service;

    private Usuario usuario;
    private PasswordResetToken tokenEntity;
    private PasswordResetTokenResponseDTO tokenResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        usuario = Usuario.builder()
                .id(15L)
                .nombre("Allison")
                .email("allison@example.com")
                .build();

        tokenEntity = PasswordResetToken.builder()
                .id(1L)
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .usado(false)
                .build();

        tokenResponse = PasswordResetTokenResponseDTO.builder()
                .id(1L)
                .usuarioId(usuario.getId())
                .token(tokenEntity.getToken())
                .expiresAt(tokenEntity.getExpiresAt())
                .usado(false)
                .build();
    }

    // ✅ Test para listarTokens()
    @Test
    void listarTokens_debeRetornarListaDeTokens() {
        when(tokenRepository.findAll()).thenReturn(List.of(tokenEntity));
        when(mapper.toDto(tokenEntity)).thenReturn(tokenResponse);

        List<PasswordResetTokenResponseDTO> resultado = service.listarTokens();

        assertEquals(1, resultado.size());
        assertEquals(tokenEntity.getToken(), resultado.get(0).getToken());
        verify(tokenRepository, times(1)).findAll();
    }

    // ✅ Test para crearToken()
    @Test
    void crearToken_debeCrearTokenCorrectamente() throws Exception {
        PasswordResetTokenRequestDTO request = PasswordResetTokenRequestDTO.builder()
                .usuarioId(usuario.getId())
                .build();

        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(tokenEntity);
        when(mapper.toDto(tokenEntity)).thenReturn(tokenResponse);

        PasswordResetTokenResponseDTO resultado = service.crearToken(request);

        assertNotNull(resultado);
        assertEquals(usuario.getId(), resultado.getUsuarioId());
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
    }

    // ✅ Test para validarToken()
    @Test
    void validarToken_debeRetornarTokenValido() throws Exception {
        when(tokenRepository.findByToken(tokenEntity.getToken())).thenReturn(Optional.of(tokenEntity));
        when(mapper.toDto(tokenEntity)).thenReturn(tokenResponse);

        PasswordResetTokenResponseDTO resultado = service.validarToken(tokenEntity.getToken());

        assertEquals(tokenEntity.getToken(), resultado.getToken());
        verify(tokenRepository, times(1)).findByToken(tokenEntity.getToken());
    }

    // ❌ Test para token expirado o usado
    @Test
    void validarToken_debeLanzarExcepcionSiTokenExpirado() {
        tokenEntity.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByToken(tokenEntity.getToken())).thenReturn(Optional.of(tokenEntity));

        Exception e = assertThrows(Exception.class, () -> service.validarToken(tokenEntity.getToken()));
        assertTrue(e.getMessage().contains("expirado"));
    }

    // ✅ Test para marcarComoUsado()
    @Test
    void marcarComoUsado_debeActualizarToken() throws Exception {
        when(tokenRepository.findByToken(tokenEntity.getToken())).thenReturn(Optional.of(tokenEntity));

        service.marcarComoUsado(tokenEntity.getToken());

        assertTrue(tokenEntity.isUsado());
        verify(tokenRepository, times(1)).save(tokenEntity);
    }
}
