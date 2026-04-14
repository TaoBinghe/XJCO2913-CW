package com.greengo.controller;

import com.greengo.domain.Result;
import com.greengo.domain.Scooter;
import com.greengo.domain.ScooterWalkingRoute;
import com.greengo.service.RoutePlanningService;
import com.greengo.service.ScooterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/scooter")
public class ScooterController {

    @Autowired
    private ScooterService scooterService;

    @Autowired
    private RoutePlanningService routePlanningService;

    @GetMapping("/list")
    public Result<List<Scooter>> list() {
        return Result.success(scooterService.listAll());
    }

    @GetMapping("/route")
    public Result<?> route(@RequestParam Long scooterId,
                           @RequestParam BigDecimal fromLongitude,
                           @RequestParam BigDecimal fromLatitude) {
        try {
            ScooterWalkingRoute route = routePlanningService.planWalkingRouteToScooter(
                    scooterId,
                    fromLongitude,
                    fromLatitude
            );
            return Result.success(route);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}

