/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest.config;

import com.connexta.ingest.adaptors.MetacardStorageAdaptor;
import com.connexta.ingest.client.StoreClient;
import com.connexta.ingest.client.TransformClient;
import com.connexta.ingest.service.api.IngestService;
import com.connexta.ingest.service.impl.IngestServiceImpl;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestServiceConfiguration {

  @Bean
  public IngestService ingestService(
      @NotBlank @Value("${endpointUrl.retrieve}") final String retrieveEndpoint,
      @NotNull final StoreClient storeClient,
      @NotNull final TransformClient transformClient,
      @NotNull final MetacardStorageAdaptor metacardStorageAdaptor) {
    return new IngestServiceImpl(
        storeClient, metacardStorageAdaptor, retrieveEndpoint, transformClient);
  }
}
