package com.hyperlogix.server.domain;

import java.util.List;

public record Path(
    List<Point> points,
    int length) {
}
