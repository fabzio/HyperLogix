package com.hyperlogix.server.infrastructure;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.features.incidents.repository.IncidentRepository;
import com.hyperlogix.server.features.incidents.utils.IncidentMapper;
import com.hyperlogix.server.features.trucks.TruckRepositoryHolder;
import com.hyperlogix.server.features.trucks.entity.TruckEntity;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!prod")
@Slf4j
@DependsOn("truckDataLoader") // Ensure trucks are loaded first
public class IncidentDataLoader implements CommandLineRunner {

    // Fixed batch size for processing
    private static final int BATCH_SIZE = 100;
      // Pattern for parsing incident data: Turn_TruckCode_IncidentType
    private static final Pattern INCIDENT_PATTERN = Pattern.compile("(T[1-3])_(T[ABCD][0-9]{1,2})_(TI[1-3])");

    @Autowired
    private IncidentRepository incidentRepository;
    
    @Autowired
    private TruckRepository truckRepository;

    @Override
    public void run(String... args) {
        // Set the TruckRepositoryHolder
        if (TruckRepositoryHolder.truckRepository == null) {
            new TruckRepositoryHolder(truckRepository);
        }
        
        if (incidentRepository.findAll().isEmpty()) {
            log.info("Starting incident data loading process from averias.txt...");
            
            String filePath = "src/main/resources/data/averias.txt";
            List<Incident> incidents = loadIncidentsFromFile(filePath);
            
            if (incidents.isEmpty()) {
                log.warn("No valid incidents found in the file.");
                return;
            }
            
            log.info("Loaded {} incidents from file, starting batch save process...", incidents.size());
            
            saveBatches(incidents);
            
            log.info("Incident data loading completed successfully!");
        } else {
            log.info("Incidents already exist in database, skipping data loading.");
        }
    }
    
    private List<Incident> loadIncidentsFromFile(String filePath) {
        List<Incident> incidents = new ArrayList<>();
        int filteredCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                Matcher matcher = INCIDENT_PATTERN.matcher(line);
                if (matcher.matches()) {                    String turn = matcher.group(1);
                    String truckCode = matcher.group(2);
                    String incidentType = matcher.group(3);
                    
                    // Validate that the truck exists in the database
                    TruckEntity truck = truckRepository.findByCode(truckCode);
                    if (truck == null) {
                        log.warn("Skipping incident for non-existent truck: {}", truckCode);
                        filteredCount++;
                        continue;
                    }
                    
                    Incident incident = new Incident();
                    incident.setId(UUID.randomUUID().toString());
                    incident.setTurn(turn);
                    incident.setType(incidentType);
                    incident.setTruckCode(truckCode);
                    incident.setDaysSinceIncident(0); // Initialize to 0
                    
                    incidents.add(incident);
                } else {
                    log.warn("Skipping malformed line in {}: {}", filePath, line);
                }
            }
            
            if (filteredCount > 0) {
                log.info("Filtered out {} incidents due to missing trucks", filteredCount);
            }
            
        } catch (IOException e) {
            log.error("Error reading incident file {}: {}", filePath, e.getMessage());
        }
        
        return incidents;
    }    @Transactional
    private void saveBatches(List<Incident> incidents) {
        int totalIncidents = incidents.size();
        int totalBatches = (int) Math.ceil((double) totalIncidents / BATCH_SIZE);
        
        for (int i = 0; i < totalIncidents; i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, totalIncidents);
            List<Incident> batch = incidents.subList(i, endIndex);
            
            int currentBatch = (i / BATCH_SIZE) + 1;
            log.info("Processing batch {}/{} ({} incidents)...", currentBatch, totalBatches, batch.size());
            
            try {
                List<com.hyperlogix.server.features.incidents.entity.IncidentEntity> entities = batch.stream()
                    .map(IncidentMapper::mapToEntity)
                    .toList();
                
                incidentRepository.saveAll(entities);
                log.info("Successfully saved batch {}/{}", currentBatch, totalBatches);
                
            } catch (Exception e) {
                log.error("Failed to save batch {}/{}: {}", currentBatch, totalBatches, e.getMessage());
                throw new RuntimeException("Batch save failed", e);
            }
        }
    }
}
