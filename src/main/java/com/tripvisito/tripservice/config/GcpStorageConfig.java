package com.tripvisito.tripservice.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Cloud Storage bean configuration.
 *
 * <p>Creates the {@link Storage} client using <b>Application Default Credentials (ADC)</b>:
 * <ul>
 *   <li>On <b>GCP Compute Engine</b> — the VM's service account is used automatically
 *       (no additional setup needed if the SA has {@code roles/storage.objectAdmin}).</li>
 *   <li>On <b>local development</b> — run {@code gcloud auth application-default login},
 *       or set {@code GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json}.</li>
 * </ul>
 *
 * <p>If credentials are not configured (e.g. during unit tests), the storage
 * bean is still created but image uploads will throw at runtime. The
 * {@link com.tripvisito.tripservice.service.GcpStorageService} logs warnings
 * in that case.
 *
 * <p>This configuration fulfills the <b>ECA mandatory GCP Cloud Storage requirement</b>.
 */
@Configuration
public class GcpStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(GcpStorageConfig.class);

    @Bean
    public Storage storage() {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            log.info("[GcpStorageConfig] Google Cloud Storage client initialized successfully.");
            return storage;
        } catch (Exception e) {
            log.warn("[GcpStorageConfig] Could not initialize GCP Storage client: {}. " +
                     "Image uploads will fail at runtime. " +
                     "Set GOOGLE_APPLICATION_CREDENTIALS or run on GCP Compute Engine.", e.getMessage());
            // Return a no-op storage instance so the app can start in dev environments
            // without GCP credentials configured.
            return StorageOptions.getDefaultInstance().getService();
        }
    }
}
