package com.hyperlogix.server.features.stations.usecases;

import org.springframework.stereotype.Service;

@Service
public class DeleteStationUseCaseImpl implements DeleteStationUseCase {
  
  @Override
  public void deleteStation(String id) {
    // Here would be the repository delete operation
    // For now, just log the deletion
    System.out.println("Deleting station with ID: " + id);
  }
}
