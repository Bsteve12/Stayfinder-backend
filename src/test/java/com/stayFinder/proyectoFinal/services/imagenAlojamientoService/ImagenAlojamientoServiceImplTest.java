package com.stayFinder.proyectoFinal.services.imagenAlojamientoService;

import com.stayFinder.proyectoFinal.dto.inputDTO.ImagenAlojamientoRequestDTO;
import com.stayFinder.proyectoFinal.dto.outputDTO.ImagenAlojamientoResponseDTO;
import com.stayFinder.proyectoFinal.entity.Alojamiento;
import com.stayFinder.proyectoFinal.entity.ImagenAlojamiento;
import com.stayFinder.proyectoFinal.mapper.ImagenAlojamientoMapper;
import com.stayFinder.proyectoFinal.repository.AlojamientoRepository;
import com.stayFinder.proyectoFinal.repository.ImagenAlojamientoRepository;
import com.stayFinder.proyectoFinal.services.imagenService.implementations.ImagenAlojamientoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImagenAlojamientoServiceImplTest {

    @Mock
    private ImagenAlojamientoRepository imagenRepository;

    @Mock
    private AlojamientoRepository alojamientoRepository;

    @Mock
    private ImagenAlojamientoMapper mapper;

    @InjectMocks
    private ImagenAlojamientoServiceImpl imagenService;

    private Alojamiento alojamiento;
    private ImagenAlojamiento imagen;
    private ImagenAlojamientoRequestDTO requestDTO;
    private ImagenAlojamientoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        alojamiento = new Alojamiento();
        alojamiento.setId(1L);

        imagen = new ImagenAlojamiento();
        imagen.setId(10L);
        imagen.setUrl("https://cdn.stayfinder.com/alojamientos/imagen1.jpg");
        imagen.setAlojamiento(alojamiento);

        requestDTO = new ImagenAlojamientoRequestDTO();
        requestDTO.setAlojamientoId(1L);
        requestDTO.setUrl("https://cdn.stayfinder.com/alojamientos/imagen1.jpg");

        responseDTO = new ImagenAlojamientoResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setUrl(imagen.getUrl());
        responseDTO.setAlojamientoId(1L);
    }

    //  Test subirImagen()
    @Test
    void subirImagen_deberiaGuardarYRetornarDTO() throws Exception {
        when(alojamientoRepository.findById(1L)).thenReturn(Optional.of(alojamiento));
        when(mapper.toEntity(requestDTO)).thenReturn(imagen);
        when(imagenRepository.save(imagen)).thenReturn(imagen);
        when(mapper.toDto(imagen)).thenReturn(responseDTO);

        ImagenAlojamientoResponseDTO result = imagenService.subirImagen(requestDTO);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("https://cdn.stayfinder.com/alojamientos/imagen1.jpg", result.getUrl());
        verify(imagenRepository, times(1)).save(imagen);
    }

    // Test subirImagen() cuando el alojamiento no existe
    @Test
    void subirImagen_conAlojamientoInexistente_deberiaLanzarExcepcion() {
        when(alojamientoRepository.findById(1L)).thenReturn(Optional.empty());
        Exception exception = assertThrows(Exception.class, () -> imagenService.subirImagen(requestDTO));
        assertEquals("Alojamiento no encontrado", exception.getMessage());
    }

    //  Test obtenerImagen()
    @Test
    void obtenerImagen_deberiaRetornarDTO() throws Exception {
        when(imagenRepository.findById(10L)).thenReturn(Optional.of(imagen));
        when(mapper.toDto(imagen)).thenReturn(responseDTO);

        ImagenAlojamientoResponseDTO result = imagenService.obtenerImagen(10L);

        assertEquals(10L, result.getId());
        verify(imagenRepository, times(1)).findById(10L);
    }

    // ⚠️ Test obtenerImagen() cuando no existe
    @Test
    void obtenerImagen_conIdInexistente_deberiaLanzarExcepcion() {
        when(imagenRepository.findById(99L)).thenReturn(Optional.empty());
        Exception exception = assertThrows(Exception.class, () -> imagenService.obtenerImagen(99L));
        assertEquals("Imagen no encontrada", exception.getMessage());
    }

    //  Test listarPorAlojamiento()
    @Test
    void listarPorAlojamiento_deberiaRetornarListaDeImagenes() throws Exception {
        alojamiento.setImagenes(List.of(imagen));
        when(alojamientoRepository.findById(1L)).thenReturn(Optional.of(alojamiento));
        when(mapper.toDto(imagen)).thenReturn(responseDTO);

        List<ImagenAlojamientoResponseDTO> result = imagenService.listarPorAlojamiento(1L);

        assertEquals(1, result.size());
        assertEquals("https://cdn.stayfinder.com/alojamientos/imagen1.jpg", result.get(0).getUrl());
        verify(alojamientoRepository, times(1)).findById(1L);
    }

    //  Test listarPorAlojamiento() con alojamiento inexistente
    @Test
    void listarPorAlojamiento_conAlojamientoInexistente_deberiaLanzarExcepcion() {
        when(alojamientoRepository.findById(1L)).thenReturn(Optional.empty());
        Exception exception = assertThrows(Exception.class, () -> imagenService.listarPorAlojamiento(1L));
        assertEquals("Alojamiento no encontrado", exception.getMessage());
    }

    // Test eliminarImagen()
    @Test
    void eliminarImagen_deberiaEliminarCorrectamente() throws Exception {
        when(imagenRepository.findById(10L)).thenReturn(Optional.of(imagen));
        doNothing().when(imagenRepository).delete(imagen);

        imagenService.eliminarImagen(10L);

        verify(imagenRepository, times(1)).delete(imagen);
    }

    //  Test eliminarImagen() cuando no existe
    @Test
    void eliminarImagen_conIdInexistente_deberiaLanzarExcepcion() {
        when(imagenRepository.findById(99L)).thenReturn(Optional.empty());
        Exception exception = assertThrows(Exception.class, () -> imagenService.eliminarImagen(99L));
        assertEquals("Imagen no encontrada", exception.getMessage());
    }
}
