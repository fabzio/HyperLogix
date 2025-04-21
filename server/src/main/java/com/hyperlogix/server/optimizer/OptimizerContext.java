package com.hyperlogix.server.optimizer;

import com.hyperlogix.server.domain.PLGNetwork;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
public class OptimizerContext {
    public PLGNetwork plgNetwork;
    /**
     * Fecha actual que usará el optimizador
     */
    public LocalDateTime algorithmStartDate;
}
