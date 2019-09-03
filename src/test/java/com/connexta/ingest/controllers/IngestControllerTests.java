/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.ingest.exceptions.StoreException;
import com.connexta.ingest.exceptions.StoreMetacardException;
import com.connexta.ingest.exceptions.TransformException;
import com.connexta.ingest.service.impl.IngestServiceImpl;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
public class IngestControllerTests {

  private static final String ACCEPT_VERSION = "0.1.0";
  private static final String CORRELATION_ID = "90210";
  private static final MockMultipartFile file =
      new MockMultipartFile("file", "file_content".getBytes());
  private static final MockMultipartFile metacard =
      new MockMultipartFile("metacard", "content".getBytes());

  @MockBean private IngestServiceImpl ingestService;
  @Inject private MockMvc mockMvc;

  @Test
  public void testSuccessfulIngestRequest() throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(file)
                .file(metacard)
                .param("correlationId", CORRELATION_ID)
                .header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().isAccepted());
  }

  @Test
  public void testMissingFile() throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(metacard)
                .param("correlationId", CORRELATION_ID)
                .header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testMissingMetacard() throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(file)
                .param("correlationId", CORRELATION_ID)
                .header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testMissingCorrelationID() throws Exception {
    mockMvc
        .perform(
            multipart("/ingest").file(file).file(metacard).header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testMissingAcceptVersionHeader() throws Exception {
    mockMvc
        .perform(multipart("/ingest").file(file).file(metacard))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest(name = "{0} is thrown when IngestService#ingest throws {1}")
  @MethodSource("requestsThatThrowErrors")
  public void testIngestServiceExceptions(HttpStatus responseStatus, Throwable throwableType)
      throws Exception {
    doThrow(throwableType).when(ingestService).ingest(any(), any(), any(), any(), any(), any());

    mockMvc
        .perform(
            multipart("/ingest")
                .file(file)
                .file(metacard)
                .param("correlationId", CORRELATION_ID)
                .header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().is(responseStatus.value()));
  }

  private static Stream<Arguments> requestsThatThrowErrors() {
    return Stream.of(
        Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, new StoreException(new Throwable())),
        Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, new TransformException(new Throwable())),
        Arguments.of(
            HttpStatus.INTERNAL_SERVER_ERROR, new StoreMetacardException("Test", new Throwable())));
  }
}
