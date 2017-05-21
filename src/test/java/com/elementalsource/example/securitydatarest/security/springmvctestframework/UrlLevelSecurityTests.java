/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.elementalsource.example.securitydatarest.security.springmvctestframework;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.elementalsource.example.securitydatarest.model.Employee;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Test cases that verify the URL level of security by using the Spring MVC test framework.
 *
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UrlLevelSecurityTests {

    private static final String PAYLOAD = "{\"firstName\": \"Saruman\", \"lastName\": \"the White\", " + "\"title\": \"Wizard\"}";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private FilterChainProxy filterChain;

    private MockMvc mvc;

    @Before
    public void setUp() {

        this.mvc = webAppContextSetup(context).addFilters(filterChain).build();

        SecurityContextHolder.clearContext();
    }

    @Test
    public void allowsAccessToRootResource() throws Exception {

        mvc.perform(get("/").
            accept(MediaTypes.HAL_JSON)).
            andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON)).
            andExpect(status().isOk()).
            andDo(print());
    }

    @Test
    public void rejectsPostAccessToCollectionResource() throws Exception {

        mvc.perform(post("/employees").
            content(PAYLOAD).
            accept(MediaTypes.HAL_JSON)).
            andExpect(status().isUnauthorized()).
            andDo(print());
    }

    @Test
    public void allowsGetRequestsForUser() throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaTypes.HAL_JSON_VALUE);
        headers.add(HttpHeaders.AUTHORIZATION, "Basic " + new String(Base64.encode(("greg:turnquist").getBytes())));

        mvc.perform(get("/employees").
            headers(headers)).
            andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON)).
            andExpect(status().isOk()).
            andDo(print());
    }

    @Test
    public void rejectsPostForUser() throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaTypes.HAL_JSON_VALUE);
        headers.add(HttpHeaders.AUTHORIZATION, "Basic " + new String(Base64.encode(("greg:turnquist").getBytes())));

        mvc.perform(post("/employees").
            headers(headers)).
            andExpect(status().isForbidden()).
            andDo(print());
    }

    @Test
    public void allowsGetRequestsForAdmin() throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaTypes.HAL_JSON_VALUE);
        headers.add(HttpHeaders.AUTHORIZATION, "Basic " + new String(Base64.encode(("ollie:gierke").getBytes())));

        mvc.perform(get("/employees").
            headers(headers)).
            andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON)).
            andExpect(status().isOk()).
            andDo(print());
    }

    @Test
    public void allowsPostRequestForAdmin() throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaTypes.HAL_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + new String(Base64.encode(("ollie:gierke").getBytes())));

        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String location = mvc
            .perform(post("/employees").
                content(PAYLOAD).
                headers(headers)).
                andExpect(status().isCreated()).
                andDo(print()).
                andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        ObjectMapper mapper = new ObjectMapper();

        String content = mvc.perform(get(location)).
            andReturn().getResponse().getContentAsString();
        Employee employee = mapper.readValue(content, Employee.class);

        assertThat(employee.getFirstName(), is("Saruman"));
        assertThat(employee.getLastName(), is("the White"));
        assertThat(employee.getTitle(), is("Wizard"));
    }
}
