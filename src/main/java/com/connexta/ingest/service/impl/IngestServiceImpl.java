/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest.service.impl;

import com.connexta.ingest.adaptors.MetacardStorageAdaptor;
import com.connexta.ingest.client.StoreClient;
import com.connexta.ingest.client.TransformClient;
import com.connexta.ingest.exceptions.StoreException;
import com.connexta.ingest.exceptions.StoreMetacardException;
import com.connexta.ingest.exceptions.TransformException;
import com.connexta.ingest.service.api.IngestService;
import com.google.common.annotations.VisibleForTesting;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class IngestServiceImpl implements IngestService {

  @VisibleForTesting
  static final String INVALID_RETRIEVE_URL_REASON = "Unable to construct retrieve URI";

  @NotNull private final StoreClient storeClient;
  @NotNull private final MetacardStorageAdaptor metacardStorageAdaptor;
  @NotBlank private final String retrieveEndpoint;
  @NotNull private final TransformClient transformClient;

  @Override
  public void ingest(
      @NotNull @Min(1L) @Max(10737418240L) final Long fileSize,
      @NotBlank final String mimeType,
      @NotNull final InputStream inputStream,
      @NotBlank final String fileName,
      @NotNull @Min(1L) @Max(10737418240L) final Long metacardFileSize,
      @NotNull final InputStream metacardInputStream)
      throws StoreException, TransformException, StoreMetacardException {
    final URI location = storeClient.store(fileSize, mimeType, inputStream, fileName);

    final String key = UUID.randomUUID().toString().replace("-", "");
    // TODO verify mimetype of metacard
    metacardStorageAdaptor.store(metacardFileSize, metacardInputStream, key);
    final URI metacardLocation;
    try {
      metacardLocation = new URI(retrieveEndpoint + key);
    } catch (URISyntaxException e) {
      throw new StoreMetacardException(INVALID_RETRIEVE_URL_REASON, e);
    }

    transformClient.requestTransform(location, mimeType, metacardLocation);
    log.info("Successfully submitted a transform request for {}", fileName);
  }

  // TODO test this method
  @Override
  public InputStream retrieveMetacard(@NotBlank String id) {
    return metacardStorageAdaptor.retrieve(id);
  }
}
