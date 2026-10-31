package com.materialstudio.controller;

import com.materialstudio.dto.SimulationSettings;
import com.materialstudio.service.SimulationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/simulation")
public class SimulationController {
    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping
    SimulationSettings get() {
        return simulationService.get();
    }

    @PutMapping
    SimulationSettings update(@RequestBody SimulationSettings settings) {
        return simulationService.update(settings);
    }
}
