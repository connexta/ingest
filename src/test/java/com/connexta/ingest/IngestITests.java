/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest;

import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import javax.inject.Inject;
import javax.inject.Named;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class IngestITests {

  private static final byte[] TEST_FILE = "some-content".getBytes();
  private static final byte[] TEST_METACARD = "metacard-content".getBytes();

  @Value("${endpointUrl.store}")
  private String endpointUrlStore;

  @Value("${endpointUrl.transform}")
  private String endpointUrlTransform;

  @Value("${endpoints.transform.version}")
  private String endpointsTransformVersion;

  @Inject private MockMvc mvc;
  @Inject private RestTemplate restTemplate;

  @Inject
  @Named("nonBufferingRestTemplate")
  private RestTemplate nonBufferingRestTemplate;

  private MockRestServiceServer storeServer;
  private MockRestServiceServer transformServer;

  @BeforeEach
  public void beforeEach() {
    storeServer = MockRestServiceServer.createServer(nonBufferingRestTemplate);
    transformServer = MockRestServiceServer.createServer(restTemplate);
  }

  @AfterEach
  public void afterEach() {
    storeServer.verify();
    storeServer.reset();
    transformServer.verify();
    transformServer.reset();
  }

  @Test
  public void testContextLoads() {}

  @Test
  public void testSuccessfulIngestRequest() throws Exception {
    final String location = "http://localhost:1232/store/1234";
    storeServer
        .expect(requestTo(endpointUrlStore))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withCreatedEntity(new URI(location)));

    transformServer
        .expect(requestTo(endpointUrlTransform))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Accept-Version", endpointsTransformVersion))
        .andExpect(jsonPath("$.location").value(location))
        .andRespond(
            withStatus(HttpStatus.ACCEPTED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    new JSONObject()
                        .put("id", "asdf")
                        .put("message", "The ID asdf has been accepted")
                        .toString()));

    mvc.perform(
            multipart("/ingest")
                .file("file", TEST_FILE)
                .file("metacard", TEST_METACARD)
                .param("correlationId", "000f4e4a")
                .header("Accept-Version", "1.2.1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isAccepted());
  }

  @Test
  public void testIngestRequestWithMissingMetacard() throws Exception {
    mvc.perform(
            multipart("/ingest")
                .file("file", TEST_FILE)
                .param("correlationId", "000f4e4a")
                .header("Accept-Version", "1.2.1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  /* START store request tests */

  @ParameterizedTest
  @EnumSource(
      value = HttpStatus.class,
      names = {
        "BAD_REQUEST",
        "UNAUTHORIZED",
        "FORBIDDEN",
        "NOT_IMPLEMENTED",
        "INTERNAL_SERVER_ERROR"
      })
  public void testStoreRequests(HttpStatus status) throws Exception {
    storeServer
        .expect(requestTo(endpointUrlStore))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(status));

    transformServer.expect(never(), requestTo(endpointUrlTransform));

    mvc.perform(
            multipart("/ingest")
                .file("file", TEST_FILE)
                .file("metacard", TEST_METACARD)
                .param("correlationId", "000f4e4a")
                .header("Accept-Version", "1.2.1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isInternalServerError());
  }

  /* END store request tests */

  /* START transform request tests */

  // The error handler throws the same exception for all non-202 status codes returned by the
  // transformation endpoint.
  @Test
  public void testUnsuccessfulTransformRequest() throws Exception {
    final String location = "http://localhost:1232/store/1234";
    storeServer
        .expect(requestTo(endpointUrlStore))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withCreatedEntity(new URI(location)));

    transformServer
        .expect(requestTo(endpointUrlTransform))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Accept-Version", endpointsTransformVersion))
        .andExpect(jsonPath("$.location").value(location))
        .andRespond(withServerError());

    mvc.perform(
            multipart("/ingest")
                .file("file", TEST_FILE)
                .file("metacard", TEST_METACARD)
                .param("correlationId", "000f4e4a")
                .header("Accept-Version", "1.2.1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isInternalServerError());
  }
  /* END transform request tests */
}
