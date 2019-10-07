/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest.controllers;

import com.connexta.ingest.exceptions.StoreMetacardException;
import com.connexta.ingest.rest.spring.IngestApi;
import com.connexta.ingest.service.api.IngestService;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@AllArgsConstructor
public class IngestController implements IngestApi {

  public static final MediaType METACARD_MEDIA_TYPE = MediaType.APPLICATION_XML;
  @NotNull private final IngestService ingestService;

  @Override
  public ResponseEntity<Void> ingest(
      String acceptVersion, MultipartFile file, String correlationId, MultipartFile metacard) {
    String fileName = file.getOriginalFilename();
    log.info("Ingest request received fileName={}", fileName);
    InputStream inputStream;
    InputStream metacardInputStream;
    try {
      inputStream = file.getInputStream();
      metacardInputStream = metacard.getInputStream();
    } catch (IOException e) {
      throw new ValidationException("Could not open attachment");
    }
    ingestService.ingest(
        file.getSize(),
        file.getContentType(),
        inputStream,
        fileName,
        metacard.getSize(),
        metacardInputStream);

    return ResponseEntity.accepted().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Resource> retrieveMetacard(
      @ApiParam(required = true) @PathVariable("id") final String id) {
    InputStream inputStream = null;
    try {
      // TODO return 404 if key doesn't exist
      inputStream = ingestService.retrieveMetacard(id);
      log.info("Successfully retrieved metacard id={}", id);
      return ResponseEntity.ok()
          .contentType(METACARD_MEDIA_TYPE)
          .body(new InputStreamResource(inputStream));
    } catch (RuntimeException e) {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException ioe) {
          log.warn("Unable to close InputStream when retrieving metacard id={}.", id, ioe);
        }
      }

      log.warn("Unable to retrieve metacard id={}", id, e);
      throw new StoreMetacardException("Unable to retrieve metacard", e);
    } catch (Throwable t) {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Unable to close InputStream when retrieving metacard id={}.", id, e);
        }
      }
      throw t;
    }
  }
}
