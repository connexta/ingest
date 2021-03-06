/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest.controllers;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpHeaders.LAST_MODIFIED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.ingest.exceptions.StoreException;
import com.connexta.ingest.exceptions.StoreMetacardException;
import com.connexta.ingest.exceptions.TransformException;
import com.connexta.ingest.service.api.IngestService;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
public class IngestControllerTests {

  private static final String ACCEPT_VERSION = "0.1.0";
  private static final String CORRELATION_ID = "90210";
  private static final String LAST_MODIFIED_DATE = "1984-04-20T08:08:08Z";
  private static final MockMultipartFile FILE =
      new MockMultipartFile(
          "file", "originalFilename.txt", "text/plain", "file_content".getBytes());
  private static final MockMultipartFile METACARD =
      new MockMultipartFile("metacard", "ignored.xml", "application/xml", "content".getBytes());

  @MockBean private IngestService mockIngestService;
  @Inject private MockMvc mockMvc;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2017-06-11T14:32:28Z",
        "2007-07-04T09:09:09.120+00:00",
        "1985-10-25T17:32:28.101+00:00"
      })
  public void testSuccessfulIngestRequest(String lastModified) throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(FILE)
                .file(METACARD)
                .param("correlationId", CORRELATION_ID)
                .header(LAST_MODIFIED, lastModified)
                .header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().isAccepted());

    verify(mockIngestService)
        .ingest(
            eq(FILE.getSize()),
            eq(FILE.getContentType()),
            inputStreamContentEq(FILE.getInputStream()),
            eq(FILE.getOriginalFilename()),
            eq(METACARD.getSize()),
            inputStreamContentEq(METACARD.getInputStream()));
  }

  @Test
  public void testMissingFile() throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(METACARD)
                .param("correlationId", CORRELATION_ID)
                .header(LAST_MODIFIED, LAST_MODIFIED_DATE)
                .header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(mockIngestService);
  }

  @Test
  public void testMissingMetacard() throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(FILE)
                .param("correlationId", CORRELATION_ID)
                .header(LAST_MODIFIED, LAST_MODIFIED_DATE)
                .header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(mockIngestService);
  }

  @Test
  public void testMissingCorrelationID() throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(FILE)
                .file(METACARD)
                .header(LAST_MODIFIED, LAST_MODIFIED_DATE)
                .header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(mockIngestService);
  }

  @Test
  public void testMissingAcceptVersionHeader() throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(FILE)
                .file(METACARD)
                .header(LAST_MODIFIED, LAST_MODIFIED_DATE)
                .param("correlationId", CORRELATION_ID))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(mockIngestService);
  }

  @Test
  public void testMissingLastModifiedHeader() throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(FILE)
                .file(METACARD)
                .header("Accept-Version", ACCEPT_VERSION)
                .param("correlationId", CORRELATION_ID))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(mockIngestService);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2011-12-03", // ISO_LOCAL_DATE
        "2011-12-03+01:00", // ISO_OFFSET_DATE
        "10:15:30", // ISO_LOCAL_TIME
        "10:15:30+01:00", // ISO_OFFSET_TIME
        "2011-12-03T10:15:30", // ISO_LOCAL_DATE_TIME
        "2012-337", // ISO_ORDINAL_DATE
        "2012-W48-6", // ISO_WEEK_DATE
        "20111203", // BASIC_ISO_DATE
        "Sun, 11 Jun 2017 14:32:28 GMT", // RFC_1123_DATE_TIME
        "2017-06-11T14:32:28.120+0000", // ISO_DATE_TIME missing offset :
        "             ",
        ""
      })
  public void testInvalidLastModifiedHeader(String badDate) throws Exception {
    mockMvc
        .perform(
            multipart("/ingest")
                .file(FILE)
                .file(METACARD)
                .header("Accept-Version", ACCEPT_VERSION)
                .header(LAST_MODIFIED, badDate)
                .param("correlationId", CORRELATION_ID))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(mockIngestService);
  }

  /**
   * TODO Test RuntimeException and Throwable thrown by {@link IngestService#ingest(Long, String,
   * InputStream, String, Long, InputStream)}
   */
  @ParameterizedTest(name = "{0} is the response status code when IngestService#ingest throws {1}")
  @MethodSource("exceptionThrownByIngestServiceAndExpectedResponseStatus")
  public void testIngestServiceExceptions(
      final Throwable throwable, final HttpStatus expectedResponseStatus) throws Exception {
    doThrow(throwable).when(mockIngestService).ingest(any(), any(), any(), any(), any(), any());

    mockMvc
        .perform(
            multipart("/ingest")
                .file(FILE)
                .file(METACARD)
                .param("correlationId", CORRELATION_ID)
                .header(LAST_MODIFIED, LAST_MODIFIED_DATE)
                .header("Accept-Version", ACCEPT_VERSION))
        .andExpect(status().is(expectedResponseStatus.value()));
  }

  private static Stream<Arguments> exceptionThrownByIngestServiceAndExpectedResponseStatus() {
    return Stream.of(
        Arguments.of(new StoreException(new Throwable()), HttpStatus.INTERNAL_SERVER_ERROR),
        Arguments.of(new TransformException(new Throwable()), HttpStatus.INTERNAL_SERVER_ERROR),
        Arguments.of(
            new StoreMetacardException("Test", new Throwable()), HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @NotNull
  private static InputStream inputStreamContentEq(@NotNull final InputStream expected) {
    return argThat(
        actual -> {
          try {
            return IOUtils.contentEquals(expected, actual);
          } catch (final IOException e) {
            fail("Unable to compare input streams", e);
            return false;
          }
        });
  }
}
