# Tripvisito - Trip Service

## Mandatory Student & GCP Information
- **Student Name:** Lithira Jayanaka
- **Student Number:** 241722002
- **GCP Project ID:** project-a4f7bad0-3923-4cdb-b9b

---

## Project Description
The **Trip Service** handles trip management and AI travel itinerary generation. It interfaces with:
1. **Google Gemini AI:** Generates custom travel plans based on user budget, style, interests, and duration.
2. **Unsplash API:** Sourced cover photos corresponding to trip destinations.
3. **Google Cloud Storage (GCS):** Uploads user-edited trip cover images to a bucket and stores URLs.

## Database & Cloud Storage
- **Non-Relational Database:** MongoDB (`tripvisito_trips` collection) - satisfies the ECA Non-Relational DB requirement.
- **Cloud Storage:** Google Cloud Storage bucket (`tripvisito-trip-images`) - satisfies the ECA GCP Bucket integration requirement.

## Technology Stack
- **Runtime:** Java 25
- **Framework:** Spring Boot (v3+)
- **Database:** Spring Data MongoDB
- **AI/External APIs:** Google Gemini REST API & Unsplash API
- **Storage:** Google Cloud Storage SDK
- **Build Tool:** Maven

## Setup / Getting Started Instructions

### Prerequisites
- JDK 25 installed
- MongoDB (Atlas or Local container running on port `27017`)
- Gemini API Key & GCP Service Account credentials

### Local Setup
1. Navigate to the service folder:
   ```bash
   cd tripvisito-springboot/business-services/trip-service
   ```
2. Set up environment variables in `.env`:
   ```env
   SPRING_DATA_MONGODB_URI=mongodb+srv://<username>:<password>@eca.xhcwdk1.mongodb.net/?appName=eca
   GEMINI_API_KEY=your-gemini-key
   UNSPLASH_ACCESS_KEY=your-unsplash-key
   GCP_BUCKET_NAME=your-gcp-bucket-name
   ```
3. Run the service:
   ```bash
   mvn spring-boot:run
   ```
   The service runs on port `8082`.

### PM2 Deployment
On the GCP VM (IaaS):
```bash
pm2 start ecosystem.config.js --only trip-service
```
PM2 manages service lifetime, process logging, and auto-restart properties.
