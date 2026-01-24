package com.stayFinder.proyectoFinal.services.passwordResetService;

import com.stayFinder.proyectoFinal.dto.inputDTO.PasswordResetTokenRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.PasswordResetTokenResponseDTO;
import com.stayFinder.proyectoFinal.entity.PasswordResetToken;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.repository.PasswordResetTokenRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.emailService.interfaces.EmailServiceInterface;
import com.stayFinder.proyectoFinal.services.passwordResetService.implementations.PasswordResetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private PasswordResetTokenRepository tokenRepo;

    @Mock
    private EmailServiceInterface emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private Usuario usuario;
    private PasswordResetToken token;
    private PasswordResetTokenRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Allison");
        usuario.setEmail("allison@example.com");
        usuario.setContrasena("12345");

        token = PasswordResetToken.builder()
                .id(10L)
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .usado(false)
                .build();

        requestDTO = PasswordResetTokenRequestDTO.builder()
                .usuarioId(1L)
                .build();
    }

    // ✅ Test generarToken correctamente
    @Test
    void generarToken_deberiaCrearYEnviarEmail() throws Exception {
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuario));
        when(tokenRepo.save(any(PasswordResetToken.class))).thenReturn(token);

        PasswordResetTokenResponseDTO result = passwordResetService.generarToken(requestDTO);

        assertNotNull(result);
        assertEquals(usuario.getId(), result.getUsuarioId());
        assertFalse(result.isUsado());
        verify(tokenRepo, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendReservationConfirmation(
                eq(usuario.getEmail()),
                eq("Recuperación de contraseña"),
                contains("Usa este token para restablecer tu contraseña")
        );
    }

    // ⚠️ Test generarToken cuando usuario no existe
    @Test
    void generarToken_conUsuarioInexistente_deberiaLanzarExcepcion() {
        when(usuarioRepo.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> passwordResetService.generarToken(requestDTO));
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    // ✅ Test resetPassword correctamente
    @Test
    void resetPassword_conTokenValido_deberiaActualizarPasswordYMarcarUsado() throws Exception {
        when(tokenRepo.findByToken(token.getToken())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass")).thenReturn("encodedPass");
        when(usuarioRepo.save(any(Usuario.class))).thenReturn(usuario);

        passwordResetService.resetPassword(token.getToken(), "newpass");

        verify(passwordEncoder, times(1)).encode("newpass");
        verify(usuarioRepo, times(1)).save(usuario);
        verify(tokenRepo, times(1)).save(token);
        assertTrue(token.isUsado());
    }

    // ⚠️ Test resetPassword con token inexistente
    @Test
    void resetPassword_conTokenInexistente_deberiaLanzarExcepcion() {
        when(tokenRepo.findByToken("invalido")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> passwordResetService.resetPassword("invalido", "newpass"));
        assertEquals("Token inválido", exception.getMessage());
    }

    // ⚠️ Test resetPassword con token expirado
    @Test
    void resetPassword_conTokenExpirado_deberiaLanzarExcepcion() {
        token.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(tokenRepo.findByToken(token.getToken())).thenReturn(Optional.of(token));

        Exception exception = assertThrows(Exception.class, () -> passwordResetService.resetPassword(token.getToken(), "newpass"));
        assertEquals("Token expirado o ya usado", exception.getMessage());
    }

    // ⚠️ Test resetPassword con token ya usado
    @Test
    void resetPassword_conTokenYaUsado_deberiaLanzarExcepcion() {
        token.setUsado(true);
        when(tokenRepo.findByToken(token.getToken())).thenReturn(Optional.of(token));

        Exception exception = assertThrows(Exception.class, () -> passwordResetService.resetPassword(token.getToken(), "newpass"));
        assertEquals("Token expirado o ya usado", exception.getMessage());
    }
}
