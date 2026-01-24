package com.stayFinder.proyectoFinal.services.emailService;

import com.stayFinder.proyectoFinal.services.emailService.implementations.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // TEST 1: Enviar confirmación correctamente
    @Test
    void sendReservationConfirmation_deberiaEnviarCorreo() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendReservationConfirmation(
                "cliente@example.com",
                "Confirmación de reserva",
                "Tu reserva fue confirmada correctamente."
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    // TEST 2: Simular fallo en envío de confirmación (no debe lanzar excepción)
    @Test
    void sendReservationConfirmation_conError_noDebeLanzarExcepcion() {
        doThrow(new RuntimeException("Error SMTP")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendReservationConfirmation(
                "cliente@example.com",
                "Confirmación",
                "Contenido del mensaje"
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    // TEST 3: Enviar cancelación correctamente
    @Test
    void sendReservationCancellation_deberiaEnviarCorreo() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendReservationCancellation(
                "cliente@example.com",
                "Cancelación de reserva",
                "Tu reserva fue cancelada correctamente."
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    // TEST 4: Simular fallo en envío de cancelación
    @Test
    void sendReservationCancellation_conError_noDebeLanzarExcepcion() {
        doThrow(new RuntimeException("Servidor no disponible")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendReservationCancellation(
                "cliente@example.com",
                "Cancelación",
                "El mensaje no pudo ser enviado."
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
