package com.hyperlogix.server.features.trucks.usecases;

import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteTruckUseCaseImpl implements DeleteTruckUseCase {
  
  private final TruckRepository truckRepository;
  
  public DeleteTruckUseCaseImpl(TruckRepository truckRepository) {
    this.truckRepository = truckRepository;
  }
  
  @Override
  public void deleteTruck(String id) {
    // Check if truck exists
    if (!truckRepository.existsById(Long.valueOf(id))) {
      throw new RuntimeException("Truck not found with id: " + id);
    }
    
    // Delete from database
    truckRepository.deleteById(Long.valueOf(id));
  }
}
