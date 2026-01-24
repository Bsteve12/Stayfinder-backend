package com.stayFinder.proyectoFinal.services.userService;

import com.stayFinder.proyectoFinal.dto.inputDTO.CreateUserDTO;
import com.stayFinder.proyectoFinal.dto.inputDTO.LoginRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.LoginResponseDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.UsuarioResponseDTO;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.entity.enums.Role;
import com.stayFinder.proyectoFinal.mapper.UsuarioMapper;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.security.JWTUtil;
import com.stayFinder.proyectoFinal.services.userService.implementations.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioMapper usuarioMapper;

    // **IMPORTANTE:** Mockeamos JWTUtil, ya que ahora es 'final' en el servicio
    @Mock
    private JWTUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    // Se elimina la declaración 'private JWTUtil jwtUtil;' real

    @BeforeEach
    void setUp() {
        // Inicializa los Mocks y los inyecta en userService,
        // incluyendo el AuthenticationManager y el JWTUtil mockeado
        MockitoAnnotations.openMocks(this);
        // Se elimina la línea userService.setJwtUtil(jwtUtil);
    }

    // =========================================================================
    // PRUEBAS DE CREACIÓN DE USUARIO
    // =========================================================================

    @Test
    void testCreateUser_Success() throws Exception {
        // Arrange
        CreateUserDTO dto = new CreateUserDTO(
                "Allison", "alli@example.com", "3001234567",
                "2004-05-10", "contrasena", 100L
        );
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setRole(Role.CLIENT);
        usuario.setUsuarioId(100L);

        when(usuarioRepository.existsByEmail(dto.correo())).thenReturn(false);
        when(usuarioRepository.existsByUsuarioId(dto.usuarioId())).thenReturn(false);
        when(passwordEncoder.encode(dto.contrasena())).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toDto(any(Usuario.class))).thenReturn(new UsuarioResponseDTO(
                1L,
                "Allison",
                "alli@example.com",
                "3001234567",
                LocalDate.parse("2004-05-10"),
                100L,
                Role.CLIENT
        ));

        // Act
        UsuarioResponseDTO result = userService.createUser(dto, null, null);

        // Assert
        assertNotNull(result);
        assertEquals("Allison", result.nombre());
        verify(usuarioRepository, times(1)).existsByEmail(dto.correo());
        verify(usuarioRepository, times(1)).existsByUsuarioId(dto.usuarioId());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void testCreateUser_EmailAlreadyExists() {
        // Arrange
        CreateUserDTO dto = new CreateUserDTO(
                "Allison", "alli@example.com", "3001234567",
                "2004-05-10", "contrasena", 100L
        );

        when(usuarioRepository.existsByEmail(dto.correo())).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> userService.createUser(dto, null, null));
        assertEquals("El email ya existe", exception.getMessage());
        verify(usuarioRepository, times(1)).existsByEmail(dto.correo());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void testCreateUser_UserIdAlreadyExists() {
        // Arrange
        CreateUserDTO dto = new CreateUserDTO(
                "Allison", "alli@example.com", "3001234567",
                "2004-05-10", "contrasena", 100L
        );

        when(usuarioRepository.existsByEmail(dto.correo())).thenReturn(false);
        when(usuarioRepository.existsByUsuarioId(dto.usuarioId())).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> userService.createUser(dto, null, null));
        assertEquals("El usuario_id ya está en uso", exception.getMessage());
        verify(usuarioRepository, times(1)).existsByEmail(dto.correo());
        verify(usuarioRepository, times(1)).existsByUsuarioId(dto.usuarioId());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // =========================================================================
    // PRUEBAS DE LOGIN
    // =========================================================================

    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("john@example.com", "password");
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(101L);
        usuario.setEmail("john@example.com");
        usuario.setRole(Role.CLIENT);

        Authentication authentication = mock(Authentication.class);

        // 1. Simular autenticación exitosa
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john@example.com");

        // 2. Simular búsqueda de usuario por email
        when(usuarioRepository.findByEmail("john@example.com")).thenReturn(Optional.of(usuario));

        // 3. Simular generación de token por JWTUtil (usando el mock inyectado)
        when(jwtUtil.GenerateToken(usuario.getUsuarioId(), usuario.getEmail(), usuario.getRole())).thenReturn("mocked_jwt_token");

        // Act
        LoginResponseDTO response = userService.login(loginRequestDTO);
        String token = response.token();

        // Assert
        assertNotNull(token);
        assertEquals("mocked_jwt_token", token);
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(usuarioRepository, times(1)).findByEmail("john@example.com");
        verify(jwtUtil, times(1)).GenerateToken(usuario.getUsuarioId(), usuario.getEmail(), usuario.getRole());
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Arrange
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("john@example.com", "wrongPassword");

        // Simular que el AuthenticationManager falla lanzando una excepción.
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new RuntimeException());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> userService.login(loginRequestDTO));

        // El test verifica que la excepción LANZADA por el servicio contenga este mensaje.
        assertEquals("Credenciales inválidas", exception.getMessage());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}