package com.example.becicd.controller;

import com.example.becicd.entity.San;
import com.example.becicd.repository.*;
import com.example.becicd.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PitchImageUrlTest {
    @Test void updatesExternalImageWithoutCallingStorage() throws Exception {
        SanRepository repository = mock(SanRepository.class);
        MinioService storage = mock(MinioService.class);
        San pitch = new San(); pitch.setHinhAnh("san/old.png");
        when(repository.findById(1L)).thenReturn(Optional.of(pitch));
        when(repository.save(pitch)).thenReturn(pitch);
        SanAdminController controller = new SanAdminController(repository, mock(CumSanRepository.class), storage);
        controller.suaSan(1L, Map.of("hinhAnh", "https://images.example.com/pitch.jpg"));
        assertEquals("https://images.example.com/pitch.jpg", pitch.getHinhAnh());
        assertEquals(pitch.getHinhAnh(), controller.imageUrl(1L).get("url"));
        verifyNoInteractions(storage);
    }
    @Test void invalidReplacementPreservesExistingImageAndOtherFields() {
        SanRepository repository = mock(SanRepository.class);
        San pitch = new San(); pitch.setHinhAnh("san/old.png"); pitch.setTenSan("Original");
        when(repository.findById(1L)).thenReturn(Optional.of(pitch));
        SanAdminController controller = new SanAdminController(repository, mock(CumSanRepository.class), new MinioService((io.minio.MinioClient)null));
        assertThrows(ResponseStatusException.class, () -> controller.suaSan(1L, Map.of("hinhAnh", "https://localhost/x.jpg", "tenSan", "Changed")));
        assertEquals("san/old.png", pitch.getHinhAnh()); assertEquals("Original", pitch.getTenSan());
        verify(repository, never()).save(any());
    }
    @Test void rejectsUnsafeOrTemporaryReferences() {
        for (String url : List.of("http://images.example.com/a.jpg", "https://127.0.0.1/a.jpg", "https://[::1]/a.jpg", "https://images.local/a.jpg", "https://user:pass@images.example.com/a.jpg", "https://images.example.com/a.jpg?token=abc", "data:image/png;base64,abc", "https://images.example.com/")) {
            assertThrows(ResponseStatusException.class, () -> PitchImageReference.httpsUrl(url), url);
        }
    }
    @Test void disabledStorageReturnsServiceUnavailable() {
        MinioService storage = new MinioService((io.minio.MinioClient)null);
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> storage.upload(null, "san")).getStatus().value());
    }
}
