package org.ormi.priv.tfa.orderflow.kernel.product;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import org.ormi.priv.tfa.orderflow.kernel.Product;

@QuarkusTest
public class ProductTest {

    @Test
    public void testCreateProductValid() {
        String jsonBody = """
            {
                "name": "Integration Test Product",
                "description": "Description test",
                "sku": "SKU-TEST-001"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(jsonBody)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .header("Location", notNullValue());
    }

    @Test
    public void testCreateProductInvalid() {
        // JSON Invalide (manque le nom)
        String jsonBody = """
            {
                "description": "Description test"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(jsonBody)
        .when()
            .post("/api/products")
        .then()
            .statusCode(400); // Bad Request
    }

    @Test
    public void testGetProducts() {
        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("size()", is(org.hamcrest.Matchers.greaterThanOrEqualTo(0)));
    }

    @Test
    public void testGetProductByIdNotFound() {
        given()
        .when()
            .get("/api/products/non-existent-id")
        .then()
            .statusCode(404);
    }
}