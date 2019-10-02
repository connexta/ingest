/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

import com.connexta.ingest.service.api.IngestService;
import java.io.IOException;
import javax.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class IngestControllerTests {

  @Test
  public void ingest(
      @Mock final MultipartFile mockFile,
      @Mock final IngestService mockIngestService,
      @Mock final MultipartFile mockMetacard)
      throws Exception {
    doThrow(IOException.class).when(mockFile).getInputStream();
    final IngestController ingestController = new IngestController(mockIngestService);
    assertThrows(
        ValidationException.class,
        () -> ingestController.ingest("nothing", mockFile, "also nothing", mockMetacard));
  }
}
