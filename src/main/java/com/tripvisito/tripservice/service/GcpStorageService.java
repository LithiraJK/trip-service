package com.tripvisito.tripservice.service;

import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

/**
 * Google Cloud Storage service for trip cover image uploads.
 *
 * <p>This class fulfills the <b>ECA mandatory GCP Bucket integration requirement</b>:
 * "At least one service must integrate with Google Cloud Storage (GCP Buckets)
 * for file uploads (e.g., trip images)."
 *
 * <h3>Access Control</h3>
 * <p>Uploaded objects are made publicly readable via
 * {@link Acl.Role#READER} + {@link Acl.User#ofAllUsers()}.
 * This requires the GCS bucket to have <b>uniform bucket-level access disabled</b>
 * (fine-grained ACL mode), which allows per-object ACLs.
 *
 * <p>Alternatively, configure the bucket with a public IAM policy:
 * <pre>
 *   gcloud storage buckets add-iam-policy-binding gs://tripvisito-trip-images \
 *     --member=allUsers --role=roles/storage.objectViewer
 * </pre>
 * In that case, remove the ACL call in {@link #uploadFile} and just build the
 * public URL directly.
 *
 * <h3>File Naming</h3>
 * <p>Objects are stored as {@code trips/{UUID}_{sanitized-original-name}} to:
 * <ul>
 *   <li>Prevent name collisions between users</li>
 *   <li>Avoid path-traversal attacks from raw filenames</li>
 *   <li>Group all trip images under the {@code trips/} prefix for easy lifecycle rules</li>
 * </ul>
 */
@Service
public class GcpStorageService {

    private static final Logger log = LoggerFactory.getLogger(GcpStorageService.class);

    private final Storage storage;

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    public GcpStorageService(Storage storage) {
        this.storage = storage;
    }

    /**
     * Uploads a file to the GCP bucket and returns the publicly accessible URL.
     *
     * @param originalFileName the original file name from the upload (used for extension)
     * @param content          the raw file bytes
     * @param contentType      MIME type (e.g. {@code image/jpeg}, {@code image/png})
     * @return public URL in the format
     *         {@code https://storage.googleapis.com/{bucket}/trips/{uuid}_{fileName}}
     * @throws RuntimeException if the upload fails
     */
    public String uploadFile(String originalFileName, byte[] content, String contentType) {
        // Sanitise filename — strip path separators and special chars
        String safeName = sanitizeFileName(originalFileName);
        String objectName = "trips/" + UUID.randomUUID() + "_" + safeName;

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        try {
            // Upload the file bytes
            storage.create(blobInfo, content);

            // Make the object publicly readable (commented out as uniform bucket-level access is enabled)
            // storage.createAcl(blobId, Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

            String publicUrl = String.format(
                    "https://storage.googleapis.com/%s/%s", bucketName, objectName);

            log.info("[GcpStorageService] Uploaded: {} → {}", safeName, publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("[GcpStorageService] Upload failed for '{}': {}", safeName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to GCP Storage: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from the GCS bucket by its public URL.
     * Used when a user removes an existing image during trip update.
     *
     * @param publicUrl the full public URL of the object to delete
     */
    public void deleteFile(String publicUrl) {
        try {
            // Extract object name from URL: "https://storage.googleapis.com/{bucket}/{objectName}"
            String prefix = "https://storage.googleapis.com/" + bucketName + "/";
            if (!publicUrl.startsWith(prefix)) {
                log.warn("[GcpStorageService] Skipping delete — URL not from this bucket: {}", publicUrl);
                return;
            }
            String objectName = publicUrl.substring(prefix.length());
            boolean deleted = storage.delete(BlobId.of(bucketName, objectName));
            if (deleted) {
                log.info("[GcpStorageService] Deleted: {}", objectName);
            } else {
                log.warn("[GcpStorageService] Object not found for deletion: {}", objectName);
            }
        } catch (Exception e) {
            log.error("[GcpStorageService] Delete failed for '{}': {}", publicUrl, e.getMessage());
            // Non-fatal — don't throw; a stale file in GCS is preferable to a failed update
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "image.jpg";
        }
        // Keep only alphanumeric chars, dots, hyphens, underscores
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
