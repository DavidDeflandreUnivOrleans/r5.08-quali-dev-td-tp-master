package org.openrichmedia.priv.tfa.orderflow.libs.kernel.domain.model.product;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class ProductTest {

    @Test
    void testCreateValidProduct() {
        // Arrange & Act
        Product product = Product.create(UUID.randomUUID().toString(), "Laptop", "Gaming Laptop", "SKU-123");

        // Assert
        Assertions.assertNotNull(product);
        Assertions.assertEquals(ProductStatus.ACTIVE, product.getStatus());
        Assertions.assertEquals("Laptop", product.getName());
    }

    @Test
    void testCreateInvalidProduct() {
        // Test Nom null
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Product.create(UUID.randomUUID().toString(), null, "Desc", "SKU-123");
        });

        // Test Description null
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Product.create(UUID.randomUUID().toString(), "Name", null, "SKU-123");
        });
        
        // Test SKU null
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Product.create(UUID.randomUUID().toString(), "Name", "Desc", null);
        });
    }

    @Test
    void testUpdateProductValid() {
        Product product = Product.create(UUID.randomUUID().toString(), "Laptop", "Desc", "SKU-123");
        
        // Act
        product.update("New Laptop", "New Desc");

        // Assert
        Assertions.assertEquals("New Laptop", product.getName());
        Assertions.assertEquals("New Desc", product.getDescription());
    }

    @Test
    void testUpdateProductInvalidState() {
        Product product = Product.create(UUID.randomUUID().toString(), "Laptop", "Desc", "SKU-123");
        product.retire(); // On passe le produit en RETIRED

        // Assert
        Assertions.assertThrows(IllegalStateException.class, () -> {
            product.update("Try Update", "Try Desc");
        });
    }

    @Test
    void testRetireProductValid() {
        Product product = Product.create(UUID.randomUUID().toString(), "Laptop", "Desc", "SKU-123");
        
        // Act
        product.retire();

        // Assert
        Assertions.assertEquals(ProductStatus.RETIRED, product.getStatus());
    }

    @Test
    void testRetireProductInvalidState() {
        Product product = Product.create(UUID.randomUUID().toString(), "Laptop", "Desc", "SKU-123");
        product.retire(); // Déjà RETIRED

        // Assert
        Assertions.assertThrows(IllegalStateException.class, () -> {
            product.retire();
        });
    }
}