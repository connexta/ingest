/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest.controllers;

import static org.mockito.Mockito.mock;

import com.connexta.ingest.service.api.IngestService;
import java.io.IOException;
import javax.validation.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

public class IngestControllerTests {

  @Test
  public void ingest() throws IOException {
    IngestService service = mock(IngestService.class);
    MultipartFile multipartFile = mock(MultipartFile.class);
    Mockito.doThrow(new IOException()).when(multipartFile).getInputStream();
    IngestController controller = new IngestController(service);
    Assertions.assertThrows(
        ValidationException.class,
        () -> {
          controller.ingest("nothing", multipartFile, "also nothing");
        });
  }
}
