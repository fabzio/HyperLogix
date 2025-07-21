package com.hyperlogix.server.features.trucks;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class TruckRepositoryHolder {
    public static TruckRepository truckRepository;

    @Autowired
    public TruckRepositoryHolder(TruckRepository repo) {
        truckRepository = repo;
    }
}