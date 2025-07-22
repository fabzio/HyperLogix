package com.hyperlogix.server.optimizer;

import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.PLGNetwork;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
public class OptimizerContext {
    public PLGNetwork plgNetwork;
    public LocalDateTime algorithmStartDate;
    public List<Incident> incidents;

    public OptimizerContext(PLGNetwork network, LocalDateTime algorithmStartDate) {
        this.plgNetwork = network;
        this.algorithmStartDate = algorithmStartDate;
        this.incidents = List.of(); // Default to empty list if no incidents provided
    }

}
