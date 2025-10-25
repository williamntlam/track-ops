package com.trackops.inventory.ports.output.persistence;

import com.trackops.inventory.domain.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    
    Optional<InventoryItem> findByProductId(String productId);
    
    List<InventoryItem> findByCategory(String category);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.availableQuantity >= :quantity")
    List<InventoryItem> findAvailableItemsWithQuantity(@Param("quantity") Integer quantity);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.productId IN :productIds")
    List<InventoryItem> findByProductIds(@Param("productIds") List<String> productIds);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.availableQuantity > 0")
    List<InventoryItem> findAvailableItems();
    
    boolean existsByProductId(String productId);
}
