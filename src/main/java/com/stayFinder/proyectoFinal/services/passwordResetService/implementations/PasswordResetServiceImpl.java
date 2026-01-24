package com.stayFinder.proyectoFinal.services.passwordResetService.implementations;

import com.stayFinder.proyectoFinal.dto.inputDTO.PasswordResetTokenRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.PasswordResetTokenResponseDTO;
import com.stayFinder.proyectoFinal.entity.PasswordResetToken;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.repository.PasswordResetTokenRepository;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.services.emailService.interfaces.EmailServiceInterface;
import com.stayFinder.proyectoFinal.services.passwordResetService.interfaces.PasswordResetServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetServiceInterface {

    private final UsuarioRepository usuarioRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailServiceInterface emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PasswordResetTokenResponseDTO generarToken(PasswordResetTokenRequestDTO dto) throws Exception {
        Usuario usuario = usuarioRepo.findById(dto.getUsuarioId())
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .usuario(usuario)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .usado(false)
                .build();

        tokenRepo.save(resetToken);

        // Enviar email (prototipo)
        String body = "Hola " + usuario.getNombre() + ",\n\n"
                + "Usa este token para restablecer tu contraseña: " + token
                + "\n\nEl token expira en 15 minutos.";
        emailService.sendReservationConfirmation(usuario.getEmail(), "Recuperación de contraseña", body);

        return PasswordResetTokenResponseDTO.builder()
                .id(resetToken.getId())
                .usuarioId(usuario.getId())
                .token(resetToken.getToken())
                .expiresAt(resetToken.getExpiresAt())
                .usado(resetToken.isUsado())
                .build();
    }

    @Override
    public void resetPassword(String token, String nuevaPassword) throws Exception {
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new Exception("Token inválido"));

        if (resetToken.isUsado() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new Exception("Token expirado o ya usado");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setContrasena(passwordEncoder.encode(nuevaPassword));
        usuarioRepo.save(usuario);

        resetToken.setUsado(true);
        tokenRepo.save(resetToken);
    }
}
