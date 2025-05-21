package com.hyperlogix.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class Routes {
  Map<String, List<Stop>> stops;
  Map<String, List<Path>> paths;
  double cost;
}
