package com.hyperlogix.server.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record Path(
        List<Point> points,
        int length) {
    public Path reverse() {
        List<Point> reversedPoints = new ArrayList<>(points);
        Collections.reverse(reversedPoints);
        return new Path(reversedPoints, length);
    }
}
