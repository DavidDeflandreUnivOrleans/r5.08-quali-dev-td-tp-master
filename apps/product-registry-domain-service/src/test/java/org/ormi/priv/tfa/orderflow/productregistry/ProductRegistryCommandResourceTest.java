package org.ormi.priv.tfa.orderflow.productregistry;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Tests d'intégration pour ProductRegistryCommandResource.
 * Ces tests vérifient l'intégration entre le client et le serveur via l'API HTTP.
 */
@QuarkusTest
public class ProductRegistryCommandResourceTest {

    @Test
    public void testRegisterProduct_Valid() {
        String jsonBody = """
            {
                "name": "Test Product",
                "description": "Test Description",
                "sku": "ABC-12345"
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
    public void testRegisterProduct_Invalid_MissingName() {
        String jsonBody = """
            {
                "description": "Test Description",
                "sku": "ABC-12345"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(jsonBody)
        .when()
            .post("/api/products")
        .then()
            .statusCode(400);
    }

    @Test
    public void testRegisterProduct_Invalid_NoBody() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/products")
        .then()
            .statusCode(400);
    }

    @Test
    public void testUpdateProductName_Valid() {
        // D'abord créer un produit
        String createBody = """
            {
                "name": "Original Name",
                "description": "Description",
                "sku": "XYZ-99999"
            }
            """;
        
        String location = given()
            .contentType(ContentType.JSON)
            .body(createBody)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract()
            .header("Location");
        
        // Extraire l'ID du produit depuis Location
        String productId = location.substring(location.lastIndexOf("/") + 1);
        
        // Mettre à jour le nom
        String updateBody = """
            {
                "name": "Updated Name"
            }
            """;
        
        given()
            .contentType(ContentType.JSON)
            .body(updateBody)
        .when()
            .patch("/api/products/" + productId + "/name")
        .then()
            .statusCode(204);
    }

    @Test
    public void testUpdateProductName_Invalid_MissingBody() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .patch("/api/products/00000000-0000-0000-0000-000000000000/name")
        .then()
            .statusCode(400);
    }

    @Test
    public void testUpdateProductName_Invalid_InvalidName() {
        // Créer un produit d'abord
        String createBody = """
            {
                "name": "Original Name",
                "description": "Description",
                "sku": "UPD-22222"
            }
            """;
        
        String location = given()
            .contentType(ContentType.JSON)
            .body(createBody)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract()
            .header("Location");
        
        String productId = location.substring(location.lastIndexOf("/") + 1);
        
        // Essayer de mettre à jour avec un nom invalide (vide)
        String updateBody = """
            {
                "name": ""
            }
            """;
        
        given()
            .contentType(ContentType.JSON)
            .body(updateBody)
        .when()
            .patch("/api/products/" + productId + "/name")
        .then()
            .statusCode(400);
    }

    @Test
    public void testRetireProduct_Valid() {
        // Créer un produit
        String createBody = """
            {
                "name": "Product To Retire",
                "description": "Description",
                "sku": "RET-11111"
            }
            """;
        
        String location = given()
            .contentType(ContentType.JSON)
            .body(createBody)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract()
            .header("Location");
        
        String productId = location.substring(location.lastIndexOf("/") + 1);
        
        // Retirer le produit
        given()
        .when()
            .delete("/api/products/" + productId)
        .then()
            .statusCode(204);
    }

    @Test
    public void testRetireProduct_Invalid_NonExistent() {
        given()
        .when()
            .delete("/api/products/00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(400);
    }

    @Test
    public void testUpdateProductDescription_Valid() {
        // Créer un produit
        String createBody = """
            {
                "name": "Product For Description",
                "description": "Original Description",
                "sku": "DESC-33333"
            }
            """;
        
        String location = given()
            .contentType(ContentType.JSON)
            .body(createBody)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract()
            .header("Location");
        
        String productId = location.substring(location.lastIndexOf("/") + 1);
        
        // Mettre à jour la description
        String updateBody = """
            {
                "description": "Updated Description"
            }
            """;
        
        given()
            .contentType(ContentType.JSON)
            .body(updateBody)
        .when()
            .patch("/api/products/" + productId + "/description")
        .then()
            .statusCode(204);
    }

    @Test
    public void testUpdateProductDescription_Invalid_MissingBody() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .patch("/api/products/00000000-0000-0000-0000-000000000000/description")
        .then()
            .statusCode(400);
    }
}
