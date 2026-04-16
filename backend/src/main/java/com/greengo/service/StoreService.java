package com.greengo.service;

import com.greengo.domain.Store;

import java.time.LocalDateTime;
import java.util.List;

public interface StoreService {

    List<Store> listEnabledStores(LocalDateTime appointmentStart, String hiredPeriod);

    Store getEnabledStore(Long storeId, LocalDateTime appointmentStart, String hiredPeriod);

    List<Store> listAllStores();

    Store getStoreById(Long id);

    boolean createStore(Store store);

    boolean updateStore(Long id, Store store);

    boolean deleteStore(Long id);
}
