import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.hamcrest.Matcher;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static io.restassured.path.json.JsonPath.*;


public class Test1 {

    private String baseURI = "https://jsonplaceholder.typicode.com";

    @Test
    public void testWithRootPath() {
        get("https://jsonplaceholder.typicode.com/posts")
                .then()
                .rootPath("[7]")
                .body("userId", is(1))
                .body("id", is(8));
    }

    @Test
    public void test2() {
        String href = when().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .contentType(ContentType.JSON)
                .body("albumId", is(1))
                .extract()
                .path("url");

        System.out.println(href);

        when().get(href).then().statusCode(200);
    }

    @Test
    public void test3() {
        String href = get("https://jsonplaceholder.typicode.com/photos/1").path("thumbnailUrl");
        System.out.println(href);
        when().get(href).then().statusCode(200);

        String href1 = get("https://jsonplaceholder.typicode.com/photos/1").andReturn().jsonPath().getString("thumbnailUrl");
        System.out.println(href1);
        when().get(href1).then().statusCode(200);
    }

    @Test
    public void testExtractDetailsUsingResponse() {
        Response response = when()
                .get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .extract()
                .response();

        System.out.println("Content-type: " + response.getContentType());
        System.out.println("Href: " + response.path("url"));
        System.out.println("Status Code: " + response.getStatusCode());
    }

    @Test
    public void testJsonSchemaValidation() {
        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("test_schema.json"));
    }

    private static final String value = "somevalue";

    /*
     THIS METHOD WHAT I NEED TO CHANGE PROPERTIES
      */
    @Test
    public void testProperties() {
        String title = get("https://jsonplaceholder.typicode.com/photos/1").path("title");
        updateProperties("value", title);
        System.out.println(getPropertyValue());
    }

    private String getPropertyValue() {
        CompositeConfiguration config = new CompositeConfiguration();
        config.addConfiguration(new SystemConfiguration());
        try {
            config.addConfiguration(new PropertiesConfiguration("src/main/resources/test_prop.properties"));
        } catch (ConfigurationException e) {
            System.out.println("Fail with config in getPropertyValue()");
        }

        String myValue = config.getString("third");
        return myValue;
    }

    private void updateProperties(String key, String value) {
        try {
            FileInputStream input = new FileInputStream("src/main/resources/test_prop.properties");
            Properties props = new Properties();
            props.load(input);
            input.close();

            FileOutputStream output = new FileOutputStream("src/main/resources/test_prop.properties");
            props.setProperty(key, value);
            props.store(output, null);
            output.close();
        } catch (IOException ex) {
            System.out.println("Fail with update properties in testMethod()");
        }
    }

    @Test
    public void testPresenceOfElements() {
        given()
                .get("http://www.groupkt.com/country/search?text=lands")
                .then()
                .body("RestResponse.result.name", hasItems("Cayman Islands", "Cook Islands")).log().all();
    }

    @Test
    public void testLengthOfResponse() {
        when()
                .get("http://www.groupkt.com/country/search?text=lands")
                .then()
                .body("RestResponse.result.alpha3_code*.length().sum()", greaterThan(47));
    }

    @Test
    public void testGetResponseAsList() {
        String response = get("http://www.groupkt.com/country/search?text=lands").asString();

        List<String> list = from(response).getList("RestResponse.result.name");

        System.out.println("List size: " + list.size());
        for (String country : list) {
            System.out.println(country);
        }
    }

    @Test
    public void testConditionsOnList() {
        String response = get("http://www.groupkt.com/country/search?text=lands").asString();
        List<String> list = from(response).getList("RestResponse.result.findAll { it.name.length() > 40 }.name");
        System.out.println(list);
    }

    @Test
    public void testJsonPath() {
        String json = when()
                .get("http://www.groupkt.com/country/search?text=lands")
                .then()
                .extract()
                .asString();

        JsonPath jsonPath = new JsonPath(json).setRootPath("RestResponse.result");

        List<String> list = jsonPath.get("name");
        System.out.println(list);
    }

    @Test
    public void testResponseHeaders() {
        Response response = get("http://www.groupkt.com/country/search?text=lands");

        String header = response.getHeader("keep-alive");
        System.out.println("header keep-alive: " + header);

        System.out.println();

        Headers headers = response.getHeaders();
        for (Header h : headers) {
            System.out.println(h);
        }
    }

    @Test
    public void testCookies() {
        Response response = get("https://jsonplaceholder.typicode.com/posts");
        Map<String, String> cookies = response.getCookies();

        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    @Test
    public void testDetailedCookies() {
        Response response = get("https://jsonplaceholder.typicode.com/posts");

        Cookie cookie = response.getDetailedCookie("__cfduid");
        System.out.println(cookie.hasExpiryDate());
        System.out.println(cookie.getExpiryDate());
        System.out.println(cookie.hasValue());
    }

    @Test
    public void testConnectRequest() {
        when().request("CONNECT", "http://api.fonts.com/rest/json/Accounts")
                .then()
                .statusCode(400);
    }

    @Test
    public void testQueryParameters() {
        String test = given().queryParam("text", "lands")
                .when()
                .get("http://www.groupkt.com/country/search")
                .asString();
        /*
        .then().statusCode(200);
         */

        System.out.println(test);
    }

    @Test
    public void testFormParameters() {
        given().formParam("A", "A val")
                .formParam("B", "B val")
                .when()
                .post("http://api.fonts.com/rest/json/Accounts")
                .then()
                .statusCode(400);
    }

    @Test
    public void testSetParameters() {
        given().param("A", "A val")
                .param("B", "B val")
                .when()
                .get("http://api.fonts.com/rest/json/Accounts")
                .then()
                .statusCode(400);
    }

    @Test
    public void testSetMultiValueParameters() {
        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");

        given().param("A", "val1", "val2", "val3")
                .param("B")
                .param("C", list)
                .when()
                .get("http://api.fonts.com/rest/json/Accounts")
                .then()
                .statusCode(400);
    }

    @Test
    public void testSetPathParameters() {
        given().pathParam("type", "json")
                .pathParam("section", "Domains")
                .when()
                .post("http://api.fonts.com/rest/{type}/{section}")
                .then()
                .statusCode(400);
    }

    @Test
    public void testSetCookiesInRequest() {
        given().cookie("JSESSIONID", "1")
                .when()
                .get("http://www.groupkt.com/country/search?text=lands")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSetHeaders() {
        given().header("k", "v")
                .header("k10", "val1", "val2", "val3")
                .headers("k1", "v1", "k2", "v2")
                .when()
                .get("http://api.fonts.com/rest/json/Accounts")
                .then()
                .statusCode(400);
    }

    @Test
    public void testContentType() {
        given().contentType(ContentType.JSON)
                .contentType("application/json; charset=utf-8")
                .when()
                .get("http://api.fonts.com/rest/json/Accounts")
                .then()
                .statusCode(400);
    }

    @Test
    public void testStatusInResponse() {
        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .assertThat()
                .statusCode(200)
                .log()
                .all();

        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .assertThat()
                .statusLine("HTTP/1.1 200 OK");

        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .assertThat()
                .statusLine(containsString("OK"));
    }

    @Test
    public void testHeadersInResponse() {
        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .assertThat()
                .header("X-Powered-By", "Express");

        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .assertThat()
                .headers("Vary", "Origin, Accept-Encoding", "Content-Type", containsString("json"));
    }

    @Test
    public void testContentTypeInResponse() {
        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .assertThat()
                .contentType(ContentType.JSON);
    }

    @Test
    public void testBodyInResponse() {
        String responseString = get("https://jsonplaceholder.typicode.com/photos/1").asString();
        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .assertThat()
                .body(equalTo(responseString));
    }

    @Test
    public void testBodyParametersInResponse() {
        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .body("thumbnailUrl", response -> equalTo("https://via.placeholder.com/150/92c952"));

        given().get("https://jsonplaceholder.typicode.com/photos/1")
                .then()
                .body("thumbnailUrl", endsWith("92c952"));
    }

    @Test
    public void testCookiesInResponse() {
        given().get("https://jsonplaceholder.typicode.com/comments")
                .then()
                .log()
                .all()
                .assertThat()
                .cookie("Domain", containsString(".typicode.com"));
    }

    @Test
    public void testResponseTime() {
        long t = given().get("https://jsonplaceholder.typicode.com/comments").time();
        System.out.println("Time(ms): " + t);
    }

    @Test
    public void testResponseTimeInUnit() {
        long t = given().get("https://jsonplaceholder.typicode.com/comments").timeIn(TimeUnit.MILLISECONDS);
        System.out.println("Time(ms): " + t);
    }

    @Test
    public void testResponseTimeAssertation() {
        given().get("https://jsonplaceholder.typicode.com/comments")
                .then()
                .time(lessThan(1000L));
    }

    @Test
    public void testResponseSpecBuilder() {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectHeader("Content-Type", "application/json; charset=utf-8");
        ResponseSpecification responseSpecification = builder.build();

        when().get("https://jsonplaceholder.typicode.com/comments")
                .then()
                .spec(responseSpecification)
                .time(lessThan(4000L));
    }

    @Test
    public void testRequestSpecBuilder() {
        RequestSpecBuilder builder = new RequestSpecBuilder();
        builder.addParam("parameter1", "parameterValue");
        builder.addHeader("header1", "headerValue");
        RequestSpecification requestSpecification = builder.build();

        given().spec(requestSpecification)
                .when()
                .get("https://jsonplaceholder.typicode.com/comments")
                .then()
                .statusCode(200)
                .log()
                .all();
    }

    @Test
    public void testLogHeaders() {
        given().get("https://jsonplaceholder.typicode.com/comments")
                .then()
                .log()
                .headers();
//      .body();
//      .cookies();
//      .all();
    }

    @Test
    public void testLogIfError() {
        given().get("https://jsonplaceholder.typicode.com/comments1")
                .then()
                .log()
                .ifError();
    }

    @Test
    public void testLogWithCondition() {
        given().get("https://jsonplaceholder.typicode.com/comments")
                .then()
                .log()
                .ifStatusCodeIsEqualTo(200);
    }

    @Test
    public void testSerializationUsingHashMap() {
        Map<String, String> inputJson = new HashMap<>();
        inputJson.put("firstName", "name");
        inputJson.put("secondName", "surname");
        inputJson.put("age", "21");

        given().contentType("application/json")
                .body(inputJson)
                .when()
                .post("http://thomas-bayer.com/restnames/countries.groovy")
                .then()
                .statusCode(200);
    }
}


