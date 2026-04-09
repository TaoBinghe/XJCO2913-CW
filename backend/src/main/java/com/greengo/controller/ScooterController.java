package com.greengo.controller;

import com.greengo.domain.Result;
import com.greengo.domain.Scooter;
import com.greengo.service.ScooterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/scooter")
public class ScooterController {

    @Autowired
    private ScooterService scooterService;

    @GetMapping("/list")
    public Result<List<Scooter>> list() {
        return Result.success(scooterService.listAll());
    }
}

