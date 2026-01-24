package com.stayFinder.proyectoFinal.controller;

import com.stayFinder.proyectoFinal.dto.inputDTO.PasswordResetTokenRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.PasswordResetTokenResponseDTO;
import com.stayFinder.proyectoFinal.services.passwordResetService.interfaces.PasswordResetServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetServiceInterface resetService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Generar token de recuperación de contraseña")
    @ApiResponse(responseCode = "200", description = "Token generado y enviado al correo")
    public PasswordResetTokenResponseDTO forgotPassword(@RequestBody PasswordResetTokenRequestDTO dto) throws Exception {
        return resetService.generarToken(dto);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer contraseña con token")
    @ApiResponse(responseCode = "200", description = "Contraseña restablecida correctamente")
    public String resetPassword(@RequestParam String token, @RequestParam String nuevaPassword) throws Exception {
        resetService.resetPassword(token, nuevaPassword);
        return "Contraseña restablecida correctamente";
    }
}
