package com.trackops.inventory.application.services;

import com.trackops.inventory.domain.model.InventoryItem;
import com.trackops.inventory.ports.output.persistence.InventoryItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class InventoryDataInitializationService implements CommandLineRunner {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryDataInitializationService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing inventory data...");
        
        // Check if data already exists
        if (inventoryItemRepository.count() > 0) {
            log.info("Inventory data already exists, skipping initialization");
            return;
        }

        // Create sample inventory items
        List<InventoryItem> sampleItems = List.of(
            InventoryItem.builder()
                .productId("PROD-001")
                .productName("Wireless Headphones")
                .description("High-quality wireless headphones with noise cancellation")
                .availableQuantity(50)
                .unitPrice(new BigDecimal("199.99"))
                .category("Electronics")
                .build(),
            InventoryItem.builder()
                .productId("PROD-002")
                .productName("Smart Watch")
                .description("Fitness tracking smart watch with heart rate monitor")
                .availableQuantity(30)
                .unitPrice(new BigDecimal("299.99"))
                .category("Electronics")
                .build(),
            InventoryItem.builder()
                .productId("PROD-003")
                .productName("Coffee Maker")
                .description("Programmable coffee maker with thermal carafe")
                .availableQuantity(25)
                .unitPrice(new BigDecimal("89.99"))
                .category("Appliances")
                .build(),
            InventoryItem.builder()
                .productId("PROD-004")
                .productName("Running Shoes")
                .description("Lightweight running shoes with cushioned sole")
                .availableQuantity(100)
                .unitPrice(new BigDecimal("129.99"))
                .category("Sports")
                .build(),
            InventoryItem.builder()
                .productId("PROD-005")
                .productName("Laptop Stand")
                .description("Adjustable aluminum laptop stand for ergonomic work")
                .availableQuantity(75)
                .unitPrice(new BigDecimal("49.99"))
                .category("Accessories")
                .build()
        );

        inventoryItemRepository.saveAll(sampleItems);
        log.info("Successfully initialized {} inventory items", sampleItems.size());
    }
}
