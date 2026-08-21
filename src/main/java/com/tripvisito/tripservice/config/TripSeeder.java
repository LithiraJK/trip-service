package com.tripvisito.tripservice.config;

import com.tripvisito.tripservice.document.Trip;
import com.tripvisito.tripservice.document.embedded.Location;
import com.tripvisito.tripservice.document.embedded.TripDetails;
import com.tripvisito.tripservice.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TripSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TripSeeder.class);
    private final TripRepository tripRepository;

    public TripSeeder(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tripRepository.count() > 0) {
            log.info("[TripSeeder] Trips already exist — skipping seed.");
            return;
        }

        Trip trip1 = Trip.builder()
                .id("trip-seed-1")
                .userId("1") // Associated with Super Admin
                .paymentLink("")
                .imageUrls(List.of("https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=800&q=80"))
                .tripDetails(TripDetails.builder()
                        .name("7-Day Romantic Paris Getaway")
                        .description("Experience the city of lights, art galleries, cozy cafes, and cruises along the Seine.")
                        .estimatedPrice("1500")
                        .duration(7)
                        .budget("Moderate")
                        .travelStyle("Romantic")
                        .country("France")
                        .interests(List.of("Art", "History", "Food"))
                        .groupType("Couple")
                        .location(Location.builder()
                                .city("Paris")
                                .coordinates(List.of(48.8566, 2.3522))
                                .openStreetMap("https://www.openstreetmap.org/#map=12/48.8566/2.3522")
                                .build())
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        Trip trip2 = Trip.builder()
                .id("trip-seed-2")
                .userId("1")
                .paymentLink("")
                .imageUrls(List.of("https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?auto=format&fit=crop&w=800&q=80"))
                .tripDetails(TripDetails.builder()
                        .name("5-Day Tokyo Highlights")
                        .description("Immerse yourself in neon lights, historical temples, and delicious sushi in Japan's bustling capital.")
                        .estimatedPrice("2000")
                        .duration(5)
                        .budget("Luxury")
                        .travelStyle("City Exploration")
                        .country("Japan")
                        .interests(List.of("Culture", "Technology", "Food"))
                        .groupType("Solo")
                        .location(Location.builder()
                                .city("Tokyo")
                                .coordinates(List.of(35.6762, 139.6503))
                                .openStreetMap("https://www.openstreetmap.org/#map=12/35.6762/139.6503")
                                .build())
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        Trip trip3 = Trip.builder()
                .id("trip-seed-3")
                .userId("1")
                .paymentLink("")
                .imageUrls(List.of("https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?auto=format&fit=crop&w=800&q=80"))
                .tripDetails(TripDetails.builder()
                        .name("10-Day Gold Coast Beach Vacation")
                        .description("Fun-filled beach vacation with surf lessons, theme parks, and ocean safaris.")
                        .estimatedPrice("1200")
                        .duration(10)
                        .budget("Budget")
                        .travelStyle("Adventure")
                        .country("Australia")
                        .interests(List.of("Beaches", "Nature", "Wildlife"))
                        .groupType("Family")
                        .location(Location.builder()
                                .city("Gold Coast")
                                .coordinates(List.of(-28.0167, 153.4000))
                                .openStreetMap("https://www.openstreetmap.org/#map=12/-28.0167/153.4000")
                                .build())
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        tripRepository.saveAll(List.of(trip1, trip2, trip3));
        log.info("[TripSeeder] 3 seed trips saved successfully.");
    }
}
