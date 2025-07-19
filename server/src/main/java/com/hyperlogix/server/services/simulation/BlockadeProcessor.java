package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.domain.Roadblock;
import com.hyperlogix.server.domain.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BlockadeProcessor {
    private static final Logger log = LoggerFactory.getLogger(BlockadeProcessor.class);

    private final Map<String, List<BlockadeEntry>> blockadeCache = new ConcurrentHashMap<>();
    private final String blockadeDirectory = "src/main/java/com/hyperlogix/server/mock/bloqueos.20250419/";

    public static class BlockadeEntry {
        public final LocalDateTime startTime;
        public final LocalDateTime endTime;
        public final List<Point> affectedPoints;

        public BlockadeEntry(LocalDateTime startTime, LocalDateTime endTime, List<Point> affectedPoints) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.affectedPoints = affectedPoints;
        }
    }

    /**
     * Load blockades for a specific month (format: YYYYMM)
     */
    public List<BlockadeEntry> loadBlockades(String monthKey) {
        if (blockadeCache.containsKey(monthKey)) {
            return blockadeCache.get(monthKey);
        }

        String filename = monthKey + ".bloqueos.txt";
        Path filePath = Paths.get(blockadeDirectory, filename);

        List<BlockadeEntry> blockades = new ArrayList<>();

        try {
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);

                for (String line : lines) {
                    if (line.trim().isEmpty())
                        continue;

                    try {
                        BlockadeEntry entry = parseBlockadeLine(line, monthKey);
                        if (entry != null) {
                            blockades.add(entry);
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing blockade line: {} - {}", line, e.getMessage());
                    }
                }

                blockadeCache.put(monthKey, blockades);
                log.info("Loaded {} blockades for month {}", blockades.size(), monthKey);
            } else {
                log.warn("Blockade file not found: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Error loading blockades from file: {}", filePath, e);
        }

        return blockades;
    }

    /**
     * Parse a blockade line: 01d00h31m-01d21h35m:15,10,30,10,30,18
     */
    private BlockadeEntry parseBlockadeLine(String line, String monthKey) {
        String[] parts = line.split(":");
        if (parts.length != 2) {
            return null;
        }

        // Parse time range
        String timeRange = parts[0];
        String[] timeRangeParts = timeRange.split("-");
        if (timeRangeParts.length != 2) {
            return null;
        }

        LocalDateTime startTime = parseBlockadeTime(timeRangeParts[0], monthKey);
        LocalDateTime endTime = parseBlockadeTime(timeRangeParts[1], monthKey);

        if (startTime == null || endTime == null) {
            return null;
        }

        // Parse affected points
        String pointsStr = parts[1];
        String[] pointCoords = pointsStr.split(",");
        List<Point> affectedPoints = new ArrayList<>();

        for (int i = 0; i < pointCoords.length; i += 2) {
            if (i + 1 < pointCoords.length) {
                try {
                    double x = Double.parseDouble(pointCoords[i]);
                    double y = Double.parseDouble(pointCoords[i + 1]);
                    affectedPoints.add(new Point(x, y));
                } catch (NumberFormatException e) {
                    log.warn("Invalid point coordinates: {}, {}", pointCoords[i], pointCoords[i + 1]);
                }
            }
        }

        return new BlockadeEntry(startTime, endTime, affectedPoints);
    }

    /**
     * Parse blockade time format: 01d00h31m
     */
    private LocalDateTime parseBlockadeTime(String timeStr, String monthKey) {
        try {
            // Extract day, hour, minute
            String dayStr = timeStr.substring(0, 2);
            String hourStr = timeStr.substring(3, 5);
            String minuteStr = timeStr.substring(6, 8);

            int day = Integer.parseInt(dayStr);
            int hour = Integer.parseInt(hourStr);
            int minute = Integer.parseInt(minuteStr);

            // Get year and month from monthKey (YYYYMM)
            int year = Integer.parseInt(monthKey.substring(0, 4));
            int month = Integer.parseInt(monthKey.substring(4, 6));

            return LocalDateTime.of(year, month, day, hour, minute);
        } catch (Exception e) {
            log.warn("Error parsing blockade time: {} - {}", timeStr, e.getMessage());
            return null;
        }
    }

    /**
     * Get active blockades for a specific time
     */
    public List<Roadblock> getActiveBlockades(LocalDateTime currentTime) {
        String monthKey = currentTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
        List<BlockadeEntry> blockades = loadBlockades(monthKey);

        List<Roadblock> activeBlockades = new ArrayList<>();

        for (BlockadeEntry entry : blockades) {
            if (isTimeInRange(currentTime, entry.startTime, entry.endTime)) {
                // Create a single Roadblock with all affected points
                Roadblock roadblock = new Roadblock(
                        entry.startTime,
                        entry.endTime,
                        entry.affectedPoints);
                activeBlockades.add(roadblock);
            }
        }

        return activeBlockades;
    }

    /**
     * Check if current time is within blockade time range
     */
    private boolean isTimeInRange(LocalDateTime current, LocalDateTime start, LocalDateTime end) {
        return !current.isBefore(start) && !current.isAfter(end);
    }

    /**
     * Clear cache for testing or memory management
     */
    public void clearCache() {
        blockadeCache.clear();
        log.info("Blockade cache cleared");
    }

    /**
     * Check if blockades have changed since last check
     */
    public boolean hasActiveBlockadesChanged(LocalDateTime currentTime, List<Roadblock> previousBlockades) {
        List<Roadblock> currentBlockades = getActiveBlockades(currentTime);

        if (previousBlockades.size() != currentBlockades.size()) {
            return true;
        }

        // Compare based on time ranges and affected points
        for (int i = 0; i < currentBlockades.size(); i++) {
            Roadblock current = currentBlockades.get(i);
            Roadblock previous = previousBlockades.get(i);

            if (!current.start().equals(previous.start()) ||
                    !current.end().equals(previous.end()) ||
                    !current.blockedNodes().equals(previous.blockedNodes())) {
                return true;
            }
        }

        return false;
    }
}
