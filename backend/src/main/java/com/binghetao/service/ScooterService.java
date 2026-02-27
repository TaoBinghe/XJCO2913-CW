package com.binghetao.service;

import com.binghetao.domain.Scooter;

public interface ScooterService {


    boolean addScooter(Scooter scooter);


    boolean updateScooter(Scooter scooter);


    boolean deleteScooter(Long id);
}

