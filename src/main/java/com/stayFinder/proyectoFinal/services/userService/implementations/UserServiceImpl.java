package com.stayFinder.proyectoFinal.services.userService.implementations;

import com.stayFinder.proyectoFinal.dto.inputDTO.CreateUserDTO;
import com.stayFinder.proyectoFinal.dto.inputDTO.LoginRequestDTO;
import com.stayFinder.proyectoFinal.dto.inputDTO.UpdateUserDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.LoginResponseDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.UsuarioResponseDTO;
import com.stayFinder.proyectoFinal.entity.Usuario;
import com.stayFinder.proyectoFinal.entity.enums.Role;
import com.stayFinder.proyectoFinal.mapper.UsuarioMapper;
import com.stayFinder.proyectoFinal.repository.UsuarioRepository;
import com.stayFinder.proyectoFinal.security.JWTUtil;
import com.stayFinder.proyectoFinal.services.userService.interfaces.UserServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserServiceInterface {

    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil; // Inyección final (por constructor de Lombok)
    private final UsuarioMapper usuarioMapper;

    @Override
    public UsuarioResponseDTO createUser(CreateUserDTO dto, Role roleSolicitado, Long adminUsuarioId) throws Exception {
        if (usuarioRepository.existsByEmail(dto.correo())) {
            throw new Exception("El email ya existe");
        }
        if (usuarioRepository.existsByUsuarioId(dto.usuarioId())) {
            throw new Exception("El usuario_id ya está en uso");
        }

        Role rolFinal = Role.CLIENT;

        if (roleSolicitado != null) {
            if (adminUsuarioId == null) throw new Exception("Solo un ADMIN puede asignar roles");
            Usuario admin = usuarioRepository.findByUsuarioId(adminUsuarioId)
                    .orElseThrow(() -> new Exception("Admin no encontrado"));
            if (admin.getRole() != Role.ADMIN) throw new Exception("No autorizado para asignar roles");
            rolFinal = roleSolicitado;
        }

        String encodedPassword = passwordEncoder.encode(dto.contrasena());

        Usuario usuario = Usuario.builder()
                .nombre(dto.nombre())
                .email(dto.correo())
                .telefono(dto.telefono())
                .fechaNacimiento(dto.fechaNacimiento())
                .usuarioId(dto.usuarioId())
                .contrasena(encodedPassword)
                .role(rolFinal)
                .build();

        return usuarioMapper.toDto(usuarioRepository.save(usuario));
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) throws Exception {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDTO.email(),
                            loginRequestDTO.contrasena()
                    )
            );

            if (authentication.isAuthenticated()) {
                Optional<Usuario> user = usuarioRepository.findByEmail(authentication.getName());
                if (user.isEmpty()) throw new Exception("Usuario no existe");

                Usuario usuario = user.get();

                String token = jwtUtil.GenerateToken(
                        usuario.getUsuarioId(),
                        usuario.getEmail(),
                        usuario.getRole()
                );

                return new LoginResponseDTO(token);
            }

        } catch (Exception e) { // Captura la excepción de autenticación (BadCredentialsException, etc.)
            // Lanza tu propia excepción con el mensaje deseado.
            throw new Exception("Credenciales inválidas");
        }

        // Si la autenticación pasa pero isAuthenticated() es falso (esto es raro en la práctica)
        throw new Exception("Credenciales inválidas");
    }

    @Override
    public UsuarioResponseDTO updateUser(Long usuarioId, UpdateUserDTO dto, Long actorUsuarioId) throws Exception {
        Usuario actor = usuarioRepository.findByUsuarioId(actorUsuarioId)
                .orElseThrow(() -> new Exception("Usuario actor no encontrado"));
        Usuario usuario = usuarioRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        if (!actor.getUsuarioId().equals(usuario.getUsuarioId()) && actor.getRole() != Role.ADMIN) {
            throw new Exception("No tienes permisos para actualizar este usuario");
        }

        usuario.setNombre(dto.nombre());
        usuario.setTelefono(dto.telefono());
        usuario.setFechaNacimiento(dto.fechaNacimiento());
        // Quitar la línea de abajo si usuarioId es una clave inmutable de negocio
        // usuario.setUsuarioId(dto.usuarioId());


        if (dto.contrasena() != null && !dto.contrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(dto.contrasena()));
        }

        return usuarioMapper.toDto(usuarioRepository.save(usuario));
    }

    @Override
    public void deleteUser(Long usuarioId, Long actorUsuarioId) throws Exception {
        Usuario actor = usuarioRepository.findByUsuarioId(actorUsuarioId)
                .orElseThrow(() -> new Exception("Usuario actor no encontrado"));
        Usuario usuario = usuarioRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        if (!actor.getUsuarioId().equals(usuario.getUsuarioId()) && actor.getRole() != Role.ADMIN) {
            throw new Exception("No tienes permisos para eliminar este usuario");
        }

        usuarioRepository.delete(usuario);
    }

    @Override
    public UsuarioResponseDTO assignRole(Long usuarioId, Role newRole, Long adminUsuarioId) throws Exception {
        Usuario admin = usuarioRepository.findByUsuarioId(adminUsuarioId)
                .orElseThrow(() -> new Exception("Admin no encontrado"));
        if (admin.getRole() != Role.ADMIN) throw new Exception("No autorizado para asignar roles");

        Usuario usuario = usuarioRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        usuario.setRole(newRole);
        return usuarioMapper.toDto(usuarioRepository.save(usuario));
    }

    @Override
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Override
    public Usuario save(Usuario usuario) {
        if (usuario.getContrasena() != null && !usuario.getContrasena().startsWith("$2a$")) {
            usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public List<UsuarioResponseDTO> findAll() {
        return usuarioRepository.findAll().stream()
                .map(usuarioMapper::toDto)
                .toList();
    }

    @Override
    public List<UsuarioResponseDTO> getUsuariosPorRol(String role) {
        return usuarioRepository.buscarUsuariosPorRol(role).stream()
                .map(usuarioMapper::toDto)
                .toList();
    }
}