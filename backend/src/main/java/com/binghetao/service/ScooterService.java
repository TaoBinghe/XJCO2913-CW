package com.binghetao.service;

import com.binghetao.domain.Scooter;

// Scooter CRUD for admin
public interface ScooterService {

    // Add new scooter
    boolean addScooter(Scooter scooter);

    // Update scooter
    boolean updateScooter(Scooter scooter);

    // Delete scooter by id
    boolean deleteScooter(Long id);
}

