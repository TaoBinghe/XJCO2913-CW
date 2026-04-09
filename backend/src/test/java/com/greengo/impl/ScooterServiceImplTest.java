package com.greengo.impl;

import com.greengo.domain.Scooter;
import com.greengo.mapper.ScooterMapper;
import com.greengo.service.GeoAddressService;
import com.greengo.service.impl.ScooterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScooterServiceImplTest {

    @Mock
    private ScooterMapper scooterMapper;
    @Mock
    private GeoAddressService geoAddressService;

    private ScooterServiceImpl scooterService;

    @BeforeEach
    void setUp() {
        scooterService = new ScooterServiceImpl();
        ReflectionTestUtils.setField(scooterService, "baseMapper", scooterMapper);
        ReflectionTestUtils.setField(scooterService, "geoAddressService", geoAddressService);
    }

    @Test
    void addScooterRejectsMissingCoordinates() {
        Scooter scooter = Scooter.builder()
                .scooterCode("SC011")
                .status("AVAILABLE")
                .location("Campus West Gate")
                .build();

        boolean saved = scooterService.addScooter(scooter);

        assertFalse(saved);
        verify(scooterMapper, never()).insert(any(Scooter.class));
    }

    @Test
    void addScooterStoresValidCoordinates() {
        Scooter scooter = Scooter.builder()
                .scooterCode("SC011")
                .status("AVAILABLE")
                .location("Campus West Gate")
                .longitude(new BigDecimal("113.320001"))
                .latitude(new BigDecimal("23.099001"))
                .build();
        when(scooterMapper.selectCount(any())).thenReturn(0L);
        when(geoAddressService.reverseGeocode(new BigDecimal("113.320001"), new BigDecimal("23.099001")))
                .thenReturn("Generated Campus West Gate");
        when(scooterMapper.insert(any(Scooter.class))).thenReturn(1);

        boolean saved = scooterService.addScooter(scooter);

        ArgumentCaptor<Scooter> scooterCaptor = ArgumentCaptor.forClass(Scooter.class);
        assertTrue(saved);
        verify(scooterMapper).insert(scooterCaptor.capture());
        assertEquals("Generated Campus West Gate", scooterCaptor.getValue().getLocation());
        assertEquals(new BigDecimal("113.320001"), scooterCaptor.getValue().getLongitude());
        assertEquals(new BigDecimal("23.099001"), scooterCaptor.getValue().getLatitude());
    }

    @Test
    void updateScooterRejectsOutOfRangeCoordinates() {
        Scooter existing = Scooter.builder()
                .id(1L)
                .scooterCode("SC001")
                .status("AVAILABLE")
                .location("Campus North Gate")
                .longitude(new BigDecimal("113.323912"))
                .latitude(new BigDecimal("23.097891"))
                .build();
        Scooter update = Scooter.builder()
                .id(1L)
                .longitude(new BigDecimal("181"))
                .latitude(new BigDecimal("23.000001"))
                .build();
        when(scooterMapper.selectById(1L)).thenReturn(existing);

        boolean updated = scooterService.updateScooter(update);

        assertFalse(updated);
        verify(scooterMapper, never()).updateById(any(Scooter.class));
    }

    @Test
    void listAllUsesResolvedAddressFromCoordinates() {
        Scooter existing = Scooter.builder()
                .id(1L)
                .scooterCode("SC001")
                .status("AVAILABLE")
                .location("Old hard coded address")
                .longitude(new BigDecimal("113.323912"))
                .latitude(new BigDecimal("23.097891"))
                .build();
        when(scooterMapper.selectList(null)).thenReturn(java.util.List.of(existing));
        when(geoAddressService.reverseGeocode(new BigDecimal("113.323912"), new BigDecimal("23.097891")))
                .thenReturn("Resolved Campus North Gate");

        java.util.List<Scooter> scooters = scooterService.listAll();

        assertEquals(1, scooters.size());
        assertEquals("Resolved Campus North Gate", scooters.get(0).getLocation());
    }
}

