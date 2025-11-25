package fr.tp.slr201.projects.robotsim.service.simulation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.slr201.projects.robotsim.service.simulation.dto.FactoryDTO;
import fr.tp.slr201.projects.robotsim.service.simulation.service.SimulationService;

@RestController
@RequestMapping("/simulation")
public class SimulationController {

    private static final Logger LOG = LoggerFactory.getLogger(SimulationController.class);

    private final SimulationService simulationService;

    public SimulationController(final SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * List ids of currently running simulations.
     */
    @GetMapping()
    public ResponseEntity<List<String>> listRunning() {
        return ResponseEntity.ok(simulationService.listRunningIds());
    }

    /**
     * Start simulating a factory by id. Calls persistence server to read model, stores it and starts it.
     */
    @RequestMapping(value = "/start", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<Boolean> start(@RequestParam("id") final String id) {
        LOG.info("REST start called for id='{}'", id);
        final boolean ok = simulationService.startSimulation(id);
        return ResponseEntity.ok(ok);
    }

    /**
     * Retrieve the factory model currently being simulated.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Factory> get(@PathVariable("id") final String id) {
        LOG.debug("REST get called for id='{}'", id);
        final Factory f = simulationService.getFactory(id);
        if (f == null) {
            LOG.warn("Factory id='{}' not found among running simulations", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(f);
    }

    /**
     * Stop a running simulation.
     */
    @RequestMapping(value = "/stop", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<Boolean> stop(@RequestParam("id") final String id) {
        LOG.info("REST stop called for id='{}'", id);
        final boolean ok = simulationService.stopSimulation(id);
        return ResponseEntity.ok(ok);
    }

    /**
     * Lightweight DTO form of the factory (no polymorphic type metadata / deep graph).
     */
    @GetMapping("/{id}/dto")
    public ResponseEntity<FactoryDTO> getDto(@PathVariable("id") final String id) {
        LOG.debug("REST get DTO called for id='{}'", id);
        final FactoryDTO dto = simulationService.getFactoryDTO(id);
        if (dto == null) {
            LOG.warn("Factory id='{}' not found among running simulations (DTO)", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }
}
