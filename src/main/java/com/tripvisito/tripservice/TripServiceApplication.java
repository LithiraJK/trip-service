package com.tripvisito.tripservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Tripvisito Trip Service
 *
 * <p>Owns all trip-related concerns for the Tripvisito platform:
 *
 * <ul>
 *   <li><b>AI Generation:</b> Calls Google Gemini 2.5 Flash to generate
 *       personalised travel itineraries based on user preferences
 *       (country, budget, travelStyle, interests, duration, groupType).</li>
 *   <li><b>Image Sourcing:</b> Fetches trip cover images from Unsplash API
 *       based on the trip's country and interests.</li>
 *   <li><b>GCP Cloud Storage:</b> Uploads user-edited trip cover images to a
 *       Google Cloud Storage bucket (ECA mandatory cloud storage requirement)
 *       and stores the resulting public URLs in MongoDB.</li>
 *   <li><b>CRUD:</b> Full Create / Read / Update / Delete for trips with
 *       ownership enforcement via the {@code X-User-Id} gateway header.</li>
 *   <li><b>Internal Stats API:</b> Exposes {@code GET /internal/stats} for
 *       user-service's dashboard aggregation (trip counts, trends).</li>
 * </ul>
 *
 * <p><b>Database:</b> MongoDB — {@code tripvisito_trips} collection (Spring Data MongoDB).
 * <br>This fulfills the ECA mandatory Non-Relational database requirement.
 *
 * <p><b>Cloud Storage:</b> Google Cloud Storage bucket {@code tripvisito-trip-images}.
 * <br>This fulfills the ECA mandatory GCP Bucket integration requirement.
 *
 * <p><b>Port:</b> 8082 (configured via config-server)
 *
 * @author Tripvisito ECA Team
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TripServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripServiceApplication.class, args);
    }
}
