package com.binghetao.service;

import com.binghetao.domain.Scooter;

import java.util.List;

// Scooter CRUD for admin
public interface ScooterService {

    List<Scooter> listAll();

    boolean addScooter(Scooter scooter);

    boolean updateScooter(Scooter scooter);

    boolean deleteScooter(Long id);
}

