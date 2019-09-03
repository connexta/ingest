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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.connexta.ingest.config.AmazonS3Configuration;
import java.net.URI;
import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureWebTestClient
public class IngestITests {

  private static final byte[] TEST_FILE = "some-content".getBytes();
  private static final String TEST_FILE_CONTENT_TYPE = "text/plain";
  private static final byte[] TEST_METACARD = "metacard-content".getBytes();
  private static final String MINIO_ADMIN_ACCESS_KEY = "admin";
  private static final String MINIO_ADMIN_SECRET_KEY = "12345678";
  private static final int MINIO_PORT = 9000;

  @Container
  public static final GenericContainer minioContainer =
      new GenericContainer("minio/minio:RELEASE.2019-07-10T00-34-56Z")
          .withEnv("MINIO_ACCESS_KEY", MINIO_ADMIN_ACCESS_KEY)
          .withEnv("MINIO_SECRET_KEY", MINIO_ADMIN_SECRET_KEY)
          .withExposedPorts(MINIO_PORT)
          .withCommand("server --compat /data")
          .waitingFor(
              new HttpWaitStrategy()
                  .forPath("/minio/health/ready")
                  .withStartupTimeout(Duration.ofSeconds(30)));

  @LocalServerPort int port;

  @Value("${endpointUrl.store}")
  private String endpointUrlStore;

  @Value("${endpointUrl.transform}")
  private String endpointUrlTransform;

  @Value("${endpointUrl.retrieve}")
  private String endpointUrlRetrieve;

  @Value("${endpoints.transform.version}")
  private String endpointsTransformVersion;

  @Value("${s3.bucket}")
  private String s3Bucket;

  @Inject private AmazonS3 amazonS3;

  @Inject
  @Named("storeClientRestTemplate")
  private RestTemplate storeClientRestTemplate;

  @Inject
  @Named("transformClientRestTemplate")
  private RestTemplate transformClientRestTemplate;

  private MockRestServiceServer storeMockRestServiceServer;
  private MockRestServiceServer transformMockRestServiceServer;

  @Inject private WebTestClient webTestClient;

  @TestConfiguration
  static class Config {

    @Bean
    public AmazonS3Configuration testAmazonS3Configuration() {
      return new AmazonS3Configuration(
          String.format(
              "http://%s:%d",
              minioContainer.getContainerIpAddress(), minioContainer.getMappedPort(MINIO_PORT)),
          "local",
          MINIO_ADMIN_ACCESS_KEY,
          MINIO_ADMIN_SECRET_KEY);
    }
  }

  @BeforeEach
  public void beforeEach() {
    amazonS3.createBucket(s3Bucket);
    storeMockRestServiceServer = MockRestServiceServer.createServer(storeClientRestTemplate);
    transformMockRestServiceServer =
        MockRestServiceServer.createServer(transformClientRestTemplate);
  }

  @AfterEach
  public void afterEach() {
    storeMockRestServiceServer.verify();
    storeMockRestServiceServer.reset();
    transformMockRestServiceServer.verify();
    transformMockRestServiceServer.reset();

    amazonS3.listObjects(s3Bucket).getObjectSummaries().stream()
        .map(S3ObjectSummary::getKey)
        .forEach(key -> amazonS3.deleteObject(s3Bucket, key));
    amazonS3.deleteBucket(s3Bucket);
  }

  @Test
  public void testContextLoads() {}

  /* START ingest request tests */

  @Test
  public void testSuccessfulIngestRequest() throws Exception {
    // given
    final URI location = new URI(endpointUrlStore + "1234");
    storeMockRestServiceServer
        .expect(requestTo(endpointUrlStore))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withCreatedEntity(location));

    transformMockRestServiceServer
        .expect(requestTo(endpointUrlTransform))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Accept-Version", endpointsTransformVersion))
        .andExpect(jsonPath("$.location").value(location.toString())) // TODO assert URI, not String
        .andExpect(jsonPath("$.mimeType").value(TEST_FILE_CONTENT_TYPE))
        .andExpect(jsonPath("$.metacardLocation").isNotEmpty()) // TODO verify retrieve URL works
        .andRespond(
            withStatus(HttpStatus.ACCEPTED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    new JSONObject()
                        .put("id", "asdf")
                        .put("message", "The ID asdf has been accepted")
                        .toString()));

    final MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part(
        "file",
        new ByteArrayResource(TEST_FILE) {

          @Override
          public long contentLength() {
            return TEST_FILE.length;
          }

          @Override
          public String getFilename() {
            return "originalFilename.txt";
          }
        });
    builder.part(
        "metacard",
        new ByteArrayResource(TEST_METACARD) {
          @Override
          public long contentLength() {
            return TEST_METACARD.length;
          }

          @Override
          public String getFilename() {
            return "idk why this is needed";
          }
        });
    builder.part("correlationId", "000f4e4a");
    final MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();
    // TODO inject this like we do for the transformApiVersion in TransformClient

    webTestClient
        .post()
        .uri("/ingest")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .syncBody(multipartBody)
        .header("Accept-Version", "0.5.0")
        .exchange()
        .expectStatus()
        .isAccepted();
  }

  @Test
  public void testIngestRequestWithMissingMetacard() throws Exception {

    final MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part(
        "file",
        new ByteArrayResource(TEST_FILE) {

          @Override
          public long contentLength() {
            return TEST_FILE.length;
          }

          @Override
          public String getFilename() {
            return "originalFilename.txt";
          }
        });
    builder.part("correlationId", "000f4e4a");
    final MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

    webTestClient
        .post()
        .uri("/ingest")
        .header("Accept-Version", "1.1.1")
        .body(BodyInserters.fromMultipartData(multipartBody))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  /* END ingest request tests */

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
    storeMockRestServiceServer
        .expect(requestTo(endpointUrlStore))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(status));

    transformMockRestServiceServer.expect(never(), requestTo(endpointUrlTransform));

    webTestClient
        .post()
        .uri("/ingest")
        .header("Accept-Version", "1.1.1")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .accept(MediaType.APPLICATION_JSON)
        .syncBody(createTestMultipartFile())
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  /* END store request tests */

  /* START transform request tests */

  // The error handler throws the same exception for all non-202 status codes returned by the
  // transformation endpoint.
  @Test
  public void testUnsuccessfulTransformRequest() throws Exception {
    final URI location = new URI("http://localhost:1232/store/1234");
    storeMockRestServiceServer
        .expect(requestTo(endpointUrlStore))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withCreatedEntity(location));

    transformMockRestServiceServer
        .expect(requestTo(endpointUrlTransform))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Accept-Version", endpointsTransformVersion))
        .andExpect(jsonPath("$.location").value(location.toString())) // TODO assert URI, not String
        .andExpect(jsonPath("$.mimeType").value(TEST_FILE_CONTENT_TYPE))
        .andExpect(jsonPath("$.metacardLocation").isNotEmpty())
        .andRespond(withServerError());

    webTestClient
        .post()
        .uri("/ingest")
        .header("Accept-Version", "1.1.1")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .accept(MediaType.APPLICATION_JSON)
        .syncBody(createTestMultipartFile())
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  /* END transform request tests */

  @NotNull
  private static MultiValueMap<String, HttpEntity<?>> createTestMultipartFile() {
    final MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part(
        "file",
        new ByteArrayResource(TEST_FILE) {

          @Override
          public long contentLength() {
            return TEST_FILE.length;
          }

          @Override
          public String getFilename() {
            return "originalFilename.txt";
          }
        });
    builder.part(
        "metacard",
        new ByteArrayResource(TEST_METACARD) {

          @Override
          public long contentLength() {
            return TEST_FILE.length;
          }

          @Override
          public String getFilename() {
            return "metacard.xml";
          }
        });
    builder.part("correlationId", "000f4e4a");
    return builder.build();
  }
}
