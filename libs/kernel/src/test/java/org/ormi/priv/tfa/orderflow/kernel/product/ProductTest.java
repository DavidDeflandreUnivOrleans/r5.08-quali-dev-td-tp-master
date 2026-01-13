package org.ormi.priv.tfa.orderflow.kernel.product;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ormi.priv.tfa.orderflow.kernel.Product;
import org.ormi.priv.tfa.orderflow.kernel.product.ProductLifecycle;
import org.ormi.priv.tfa.orderflow.kernel.product.SkuId;

class ProductTest {

    @Test
    void testCreateValidProduct() {
        // Arrange & Act
        Product product = Product.create("Laptop", "Gaming Laptop", new SkuId("ABC-12345"));

        // Assert
        Assertions.assertNotNull(product);
        Assertions.assertEquals(ProductLifecycle.ACTIVE, product.getStatus());
        Assertions.assertEquals("Laptop", product.getName());
    }

    @Test
    void testCreateInvalidProduct_NullName() {
        Assertions.assertThrows(Exception.class, () -> {
            Product.create(null, "Desc", new SkuId("ABC-12345"));
        });
    }

    @Test
    void testCreateInvalidProduct_EmptyName() {
        Assertions.assertThrows(Exception.class, () -> {
            Product.create("", "Desc", new SkuId("ABC-12345"));
        });
    }

    @Test
    void testCreateInvalidProduct_NullDescription() {
        Assertions.assertThrows(Exception.class, () -> {
            Product.create("Name", null, new SkuId("ABC-12345"));
        });
    }
    
    @Test
    void testCreateInvalidProduct_NullSkuId() {
        Assertions.assertThrows(Exception.class, () -> {
            Product.create("Name", "Desc", null);
        });
    }

    @Test
    void testUpdateProductValid() {
        Product product = Product.create("Laptop", "Desc", new SkuId("ABC-12345"));
        
        // Act
        product.updateName("New Laptop");
        product.updateDescription("New Desc");

        // Assert
        Assertions.assertEquals("New Laptop", product.getName());
        Assertions.assertEquals("New Desc", product.getDescription());
    }

    @Test
    void testUpdateProduct_InvalidInput() {
        Product product = Product.create("Laptop", "Desc", new SkuId("ABC-12345"));
        
        // Test mise à jour avec nom invalide (vide)
        Assertions.assertThrows(Exception.class, () -> {
            product.updateName("");
        });
        
        // Test mise à jour avec nom invalide (null)
        Assertions.assertThrows(Exception.class, () -> {
            product.updateName(null);
        });
    }

    @Test
    void testUpdateProductInvalidState() {
        Product product = Product.create("Laptop", "Desc", new SkuId("ABC-12345"));
        product.retire(); // On passe le produit en RETIRED

        // Assert
        Assertions.assertThrows(IllegalStateException.class, () -> {
            product.updateName("Try Update");
        });
    }

    @Test
    void testRetireProductValid() {
        Product product = Product.create("Laptop", "Desc", new SkuId("ABC-12345"));
        
        // Act
        product.retire();

        // Assert
        Assertions.assertEquals(ProductLifecycle.RETIRED, product.getStatus());
    }

    @Test
    void testRetireProductInvalidState() {
        Product product = Product.create("Laptop", "Desc", new SkuId("ABC-12345"));
        product.retire(); // Déjà RETIRED

        // Assert
        Assertions.assertThrows(IllegalStateException.class, () -> {
            product.retire();
        });
    }
}