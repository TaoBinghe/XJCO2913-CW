package com.greengo.impl;

import com.greengo.domain.Scooter;
import com.greengo.domain.Store;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.mapper.StoreMapper;
import com.greengo.service.impl.ScooterServiceImpl;
import com.greengo.utils.RentalConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

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
    private StoreMapper storeMapper;

    @Mock
    private BookingMapper bookingMapper;

    private ScooterServiceImpl scooterService;

    @BeforeEach
    void setUp() {
        scooterService = new ScooterServiceImpl();
        ReflectionTestUtils.setField(scooterService, "baseMapper", scooterMapper);
        ReflectionTestUtils.setField(scooterService, "storeMapper", storeMapper);
        ReflectionTestUtils.setField(scooterService, "bookingMapper", bookingMapper);
    }

    @Test
    void addScooterRejectsUnknownStore() {
        Scooter scooter = Scooter.builder()
                .scooterCode("SC011")
                .storeId(99L)
                .build();

        boolean saved = scooterService.addScooter(scooter);

        assertFalse(saved);
        verify(scooterMapper, never()).insert(any(Scooter.class));
    }

    @Test
    void addScooterStoresSelectedStoreSnapshotAndDefaultStates() {
        Scooter scooter = Scooter.builder()
                .scooterCode("SC011")
                .storeId(7L)
                .rentalMode(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .build();
        Store store = store();

        when(scooterMapper.selectCount(any())).thenReturn(0L);
        when(storeMapper.selectById(7L)).thenReturn(store);
        when(scooterMapper.insert(any(Scooter.class))).thenReturn(1);

        boolean saved = scooterService.addScooter(scooter);

        ArgumentCaptor<Scooter> scooterCaptor = ArgumentCaptor.forClass(Scooter.class);
        assertTrue(saved);
        verify(scooterMapper).insert(scooterCaptor.capture());
        assertEquals(RentalConstants.SCOOTER_STATUS_AVAILABLE, scooterCaptor.getValue().getStatus());
        assertEquals(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED, scooterCaptor.getValue().getLockStatus());
        assertEquals(store.getAddress(), scooterCaptor.getValue().getLocation());
        assertEquals(store.getLongitude(), scooterCaptor.getValue().getLongitude());
        assertEquals(store.getLatitude(), scooterCaptor.getValue().getLatitude());
    }

    @Test
    void addScanRideScooterStoresCurrentCoordinatesWithoutStore() {
        Scooter scooter = Scooter.builder()
                .scooterCode("SC201")
                .rentalMode(RentalConstants.RENTAL_TYPE_SCAN_RIDE)
                .location("Xipu East Roadside")
                .longitude(new BigDecimal("103.982120"))
                .latitude(new BigDecimal("30.767320"))
                .build();

        when(scooterMapper.selectCount(any())).thenReturn(0L);
        when(scooterMapper.insert(any(Scooter.class))).thenReturn(1);

        boolean saved = scooterService.addScooter(scooter);

        ArgumentCaptor<Scooter> scooterCaptor = ArgumentCaptor.forClass(Scooter.class);
        assertTrue(saved);
        verify(scooterMapper).insert(scooterCaptor.capture());
        assertEquals(RentalConstants.RENTAL_TYPE_SCAN_RIDE, scooterCaptor.getValue().getRentalMode());
        assertEquals(new BigDecimal("103.982120"), scooterCaptor.getValue().getLongitude());
        assertEquals(new BigDecimal("30.767320"), scooterCaptor.getValue().getLatitude());
        assertEquals("Xipu East Roadside", scooterCaptor.getValue().getLocation());
        assertEquals(null, scooterCaptor.getValue().getStoreId());
    }

    @Test
    void updateScooterRejectsChangingStoreWhileScooterIsInUse() {
        Scooter existing = Scooter.builder()
                .id(1L)
                .scooterCode("SC001")
                .storeId(7L)
                .rentalMode(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .status(RentalConstants.SCOOTER_STATUS_IN_USE)
                .lockStatus(RentalConstants.SCOOTER_LOCK_STATUS_UNLOCKED)
                .build();
        Scooter update = Scooter.builder()
                .id(1L)
                .storeId(8L)
                .build();

        when(scooterMapper.selectById(1L)).thenReturn(existing);

        boolean updated = scooterService.updateScooter(update);

        assertFalse(updated);
        verify(scooterMapper, never()).updateById(any(Scooter.class));
    }

    @Test
    void listAllEnrichesScootersWithStoreInfo() {
        Scooter scooter = Scooter.builder()
                .id(1L)
                .scooterCode("SC001")
                .storeId(7L)
                .rentalMode(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .status(RentalConstants.SCOOTER_STATUS_AVAILABLE)
                .lockStatus(RentalConstants.SCOOTER_LOCK_STATUS_LOCKED)
                .location("Old address")
                .build();
        Store store = store();

        when(scooterMapper.selectList(any())).thenReturn(List.of(scooter));
        when(storeMapper.selectBatchIds(any())).thenReturn(List.of(store));

        List<Scooter> scooters = scooterService.listAll();

        assertEquals(1, scooters.size());
        assertEquals(store.getName(), scooters.get(0).getStoreName());
        assertEquals(store.getAddress(), scooters.get(0).getLocation());
    }

    private Store store() {
        return Store.builder()
                .id(7L)
                .name("Xipu North Hub")
                .address("Xipu Campus Library North Plaza")
                .longitude(new BigDecimal("103.981570"))
                .latitude(new BigDecimal("30.768249"))
                .status(RentalConstants.STORE_STATUS_ENABLED)
                .build();
    }
}
