package com.hyperlogix.server.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record Point(
        double x,
        double y) {
    public Point integerPoint() {
        return new Point(Math.round(x), Math.round(y));
    }

    public boolean isClose(Point o) {
        double eps = 1e-4;
        return Math.abs(this.x - o.x) <= eps && Math.abs(this.y - o.y) <= eps;
    }
}
