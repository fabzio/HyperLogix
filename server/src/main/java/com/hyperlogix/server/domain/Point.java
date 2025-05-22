package com.hyperlogix.server.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record Point(
                int x,
                int y) {
}
