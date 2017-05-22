/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elementalsource.example.securitydatarest.security.restassured;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.elementalsource.example.securitydatarest.model.Employee;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UrlLevelSecurityTests {

    private static final String PAYLOAD = "{\"firstName\": \"Saruman\", \"lastName\": \"the White\", " + "\"title\": \"Wizard\"}";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private FilterChainProxy filterChain;

    private MockMvc mvc;

    @Before
    public void setup() {
        this.mvc = webAppContextSetup(context).addFilters(filterChain).build();

        SecurityContextHolder.clearContext();
    }

    private MockMvcRequestSpecification given() {
        return RestAssuredMockMvc.given().mockMvc(mvc);
    }

    @Test
    public void allowsAccessToRootResource() throws Exception {

        given().
            accept(MediaTypes.HAL_JSON_VALUE).
        when().
            get().
        then().
            statusCode(is(HttpStatus.OK.value())).
            contentType(MediaTypes.HAL_JSON_VALUE).
            log().all();
    }

    @Test
    public void rejectsPostAccessToCollectionResource() throws Exception {

        given().
            accept(MediaTypes.HAL_JSON_VALUE).
            body(PAYLOAD).
        when().
            post("/employees").
        then().
            statusCode(is(HttpStatus.UNAUTHORIZED.value())).
            log().all();
    }

    @Test
    public void allowsGetRequestsForUser() throws Exception {

        given().
            accept(MediaTypes.HAL_JSON_VALUE).
            auth().with(httpBasic("greg", "turnquist")).
        when().
            get("/employees").
        then().
            statusCode(is(HttpStatus.OK.value())).
            contentType(MediaTypes.HAL_JSON_VALUE).
            log().all();
    }

    @Test
    public void rejectsPostForUser() throws Exception {
        given().
            accept(MediaTypes.HAL_JSON_VALUE).
            auth().with(httpBasic("greg", "turnquist")).
        when().
            post("/employees").
        then().
            statusCode(is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    public void allowsGetRequestsForAdmin() throws Exception {

        given().
            accept(MediaTypes.HAL_JSON_VALUE).
            auth().with(httpBasic("ollie", "gierke")).
        when().
            get("/employees").
        then().
            statusCode(is(HttpStatus.OK.value())).
            contentType(MediaTypes.HAL_JSON_VALUE).
            log().all();
    }

    @Test
    public void allowsPostRequestForAdminLikeModel() throws Exception {
        final Employee employee =

        given().
            accept(MediaTypes.HAL_JSON_VALUE).
            contentType(MediaType.APPLICATION_JSON_VALUE).
            auth().with(httpBasic("ollie", "gierke")).
            body(PAYLOAD).
        when().
            post("/employees").
        then().
            statusCode(is(HttpStatus.CREATED.value())).
            extract().body().as(Employee.class);

        assertThat(employee.getFirstName(), CoreMatchers.is("Saruman"));
        assertThat(employee.getLastName(), CoreMatchers.is("the White"));
        assertThat(employee.getTitle(), CoreMatchers.is("Wizard"));
    }

    @Test
    public void allowsPostRequestForAdminLikeInlineJson() throws Exception {
        given().
            accept(MediaTypes.HAL_JSON_VALUE).
            contentType(MediaType.APPLICATION_JSON_VALUE).
            auth().with(httpBasic("ollie", "gierke")).
            body(PAYLOAD).
        when().
            post("/employees").
        then().
            statusCode(is(HttpStatus.CREATED.value())).
            body("firstName", equalTo("Saruman")).
            body("lastName", equalTo("the White")).
            body("title", equalTo("Wizard"));
    }

    @Test
    public void allowsPostRequestForAdminLikeFileJson() throws Exception {
        given().
            accept(MediaTypes.HAL_JSON_VALUE).
            contentType(MediaType.APPLICATION_JSON_VALUE).
            auth().with(httpBasic("ollie", "gierke")).
            body(PAYLOAD).
        when().
            post("/employees").
        then().
            statusCode(is(HttpStatus.CREATED.value())).
            body(matchesJsonSchemaInClasspath("employee.json"));
    }

    @Test
    public void shouldPostAndGetUser() throws Exception {
        // insert new user
        final String location =

        given().
            accept(MediaTypes.HAL_JSON_VALUE).
            contentType(MediaType.APPLICATION_JSON_VALUE).
            auth().with(httpBasic("ollie", "gierke")).
            body(PAYLOAD).
        when().
            post("/employees").
        then().
            statusCode(is(HttpStatus.CREATED.value())).
            extract().header(HttpHeaders.LOCATION);

        // get created user recently
        final Employee employee =

        given().
            accept(MediaTypes.HAL_JSON_VALUE).
            auth().with(httpBasic("ollie", "gierke")).
        when().
            get(location).
        then().
            statusCode(is(HttpStatus.OK.value())).
            contentType(MediaTypes.HAL_JSON_VALUE).
            extract().body().as(Employee.class);

        assertThat(employee.getFirstName(), CoreMatchers.is("Saruman"));
        assertThat(employee.getLastName(), CoreMatchers.is("the White"));
        assertThat(employee.getTitle(), CoreMatchers.is("Wizard"));
    }

}
