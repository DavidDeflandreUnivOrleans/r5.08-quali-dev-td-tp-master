package org.ormi.priv.tfa.orderflow.productregistry.read;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Tests d'intégration pour ProductRegistryQueryResource.
 * Ces tests vérifient les fonctionnalités de recherche et de récupération de produits.
 */
@QuarkusTest
public class ProductRegistryQueryResourceTest {

    @Test
    public void testSearchProducts_WithMatchingFilter() {
        // Créer un produit avec un SKU spécifique
        String createJsonBody = """
            {
                "name": "Product for Search Test",
                "description": "Description for search test",
                "sku": "SEARCH-12345"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(createJsonBody)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201);

        // Rechercher avec le filtre
        given()
        .when()
            .get("/api/products?sku=SEARCH-12345")
        .then()
            .statusCode(200)
            .body("products.size()", is(1))
            .body("products[0].skuId", is("SEARCH-12345"));
    }

    @Test
    public void testSearchProducts_WithNoMatch() {
        given()
        .when()
            .get("/api/products?sku=NONEXISTENT-99999")
        .then()
            .statusCode(200)
            .body("products.size()", is(0));
    }

    @Test
    public void testSearchProducts_WithoutFilter() {
        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("page", is(0))
            .body("size", is(10))
            .body("products.size()", greaterThanOrEqualTo(0));
    }

    @Test
    public void testGetProductById_Existing() {
        // Créer un produit
        String createJsonBody = """
            {
                "name": "Product for Get Test",
                "description": "Description for get test",
                "sku": "GET-12345"
            }
            """;

        String location = given()
            .contentType(ContentType.JSON)
            .body(createJsonBody)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().header("Location");

        String productId = location.substring(location.lastIndexOf('/') + 1);

        // Récupérer le produit par ID
        given()
        .when()
            .get("/api/products/" + productId)
        .then()
            .statusCode(200)
            .body("id", is(productId))
            .body("name", is("Product for Get Test"));
    }

    @Test
    public void testGetProductById_NonExistent() {
        given()
        .when()
            .get("/api/products/00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(404);
    }
}
