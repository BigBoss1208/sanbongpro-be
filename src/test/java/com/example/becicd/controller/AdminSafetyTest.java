package com.example.becicd.controller;
import com.example.becicd.entity.*;
import com.example.becicd.repository.*;
import com.example.becicd.service.MinioService;
import com.example.becicd.dto.UserInput;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class AdminSafetyTest {
    @Test void changesAndClearsCluster() {
        SanRepository pitches = mock(SanRepository.class); CumSanRepository clusters = mock(CumSanRepository.class);
        San pitch = new San(); pitch.setId(1L); CumSan cluster = new CumSan(); cluster.setId(4L);
        when(pitches.findById(1L)).thenReturn(Optional.of(pitch)); when(clusters.findById(4L)).thenReturn(Optional.of(cluster)); when(pitches.save(pitch)).thenReturn(pitch);
        SanAdminController controller = new SanAdminController(pitches, clusters, mock(MinioService.class));
        controller.suaSan(1L, Map.of("cumSanId", 4)); assertSame(cluster, pitch.getCumSan());
        Map<String,Object> empty = new HashMap<>(); empty.put("cumSanId", null); controller.suaSan(1L, empty); assertNull(pitch.getCumSan());
        assertThrows(ResponseStatusException.class, () -> controller.suaSan(1L, Map.of("cumSanId", 999)));
    }
    @Test void refusesDeletionWithHistory() {
        SanRepository pitches = mock(SanRepository.class); San pitch = new San(); pitch.setDanhSachDatSan(List.of(new DatSan()));
        when(pitches.existsById(1L)).thenReturn(true); when(pitches.findById(1L)).thenReturn(Optional.of(pitch));
        SanAdminController controller = new SanAdminController(pitches, mock(CumSanRepository.class), mock(MinioService.class));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> controller.xoaSan(1L)).getStatus().value()); verify(pitches, never()).delete(any());
    }
    @Test void rejectsMalformedUserData() {
        assertThrows(ResponseStatusException.class, () -> UserInput.name(" "));
        assertThrows(ResponseStatusException.class, () -> UserInput.email("bad"));
        assertThrows(ResponseStatusException.class, () -> UserInput.username("x".repeat(51)));
        assertThrows(ResponseStatusException.class, () -> UserInput.phone("oops"));
        assertThrows(ResponseStatusException.class, () -> UserInput.password("short"));
        assertNull(UserInput.phone("")); assertEquals("A B", UserInput.name(" A B "));
    }
}
