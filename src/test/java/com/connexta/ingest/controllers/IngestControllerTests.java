/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest.controllers;

import com.connexta.ingest.service.api.IngestService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ValidationException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class IngestControllerTests {

  @Test
  public void ingest() throws IOException {
    IngestService service = mock(IngestService.class);
    MultipartFile multipartFile = mock(MultipartFile.class);
    Mockito.doThrow(new IOException()).when(multipartFile).getInputStream();
    IngestController controller = new IngestController(service);
    assertThrows(
        ValidationException.class,
        () -> {
          controller.ingest("nothing", multipartFile, "also nothing");
        });
  }
}
