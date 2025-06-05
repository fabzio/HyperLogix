package com.hyperlogix.server.features.blocks.utils;

import com.hyperlogix.server.domain.Roadblock;
import com.hyperlogix.server.features.blocks.entity.BlockEntity;
import java.util.ArrayList;

public class BlockMapper {
    public static Roadblock mapToDomain(BlockEntity entity) {
        if (entity == null)
            return null;
            
        Roadblock roadblock = new Roadblock(
            entity.getStartTime(),
            entity.getEndTime(),
            new ArrayList<>(entity.getNodes())
        );
        
        return roadblock;
    }

    public static BlockEntity mapToEntity(Roadblock roadblock) {
        if (roadblock == null)
            return null;
            
        BlockEntity entity = new BlockEntity();
        
        entity.setStartTime(roadblock.start());        
        entity.setEndTime(roadblock.end());             
        entity.setNodes(new ArrayList<>(roadblock.blockedNodes()));

        return entity;
    }
}
