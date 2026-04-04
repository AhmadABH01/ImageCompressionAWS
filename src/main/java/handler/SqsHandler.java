package handler;

import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import service.CompressionService;

public class SqsHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    private static final AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();

    // SNS Topic ARN from environment variable
    private static final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");

    // Default values if client doesn't specify metadata
    private static final String DEFAULT_METHOD = "lambda";
    private static final int DEFAULT_THRESHOLD = 30;

    @SuppressWarnings("unchecked")
    public String handleRequest(Map<String, Object> event) {
        try {
            // SQS sends "Records" array
            List<Map<String, Object>> records = (List<Map<String, Object>>) event.get("Records");

            if (records == null || records.isEmpty()) {
                return "No records to process";
            }

            int processed = 0;

            for (Map<String, Object> record : records) {
                // SQS message body contains S3 event as JSON string
                String body = (String) record.get("body");
                Map<String, Object> s3Event = objectMapper.readValue(body, Map.class); // converted to JSON

                // S3 event also has "Records" array
                List<Map<String, Object>> s3Records = (List<Map<String, Object>>) s3Event.get("Records");

                if (s3Records == null) continue;

                for (Map<String, Object> s3Record : s3Records) {
                    // Extract bucket and key from S3 event
                    Map<String, Object> s3 = (Map<String, Object>) s3Record.get("s3");
                    Map<String, Object> bucket = (Map<String, Object>) s3.get("bucket");
                    Map<String, Object> object = (Map<String, Object>) s3.get("object");

                    String bucketName = (String) bucket.get("name");
                    String objectKey = (String) object.get("key");

                    // Read metadata from S3 object (client-specified parameters)
                    ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, objectKey);

                    String method = metadata.getUserMetaDataOf("method");
                    String thresholdStr = metadata.getUserMetaDataOf("threshold");

                    // Use client values or defaults
                    if (method == null || method.isEmpty()) {
                        method = DEFAULT_METHOD;
                    }
                    int threshold = DEFAULT_THRESHOLD;
                    if (thresholdStr != null && !thresholdStr.isEmpty()) {
                        threshold = Integer.parseInt(thresholdStr);
                    }

                    System.out.println("Client parameters - Method: " + method + ", Threshold: " + threshold);

                    // Build S3 paths
                    String inputPath = "s3://" + bucketName + "/" + objectKey;

                    // Output to different bucket with "-compressed" suffix
                    String outputBucket = bucketName.replace("-input-", "-output-");
                    String outputKey = objectKey.replace(".png", "-compressed.png");
                    String outputPath = "s3://" + outputBucket + "/" + outputKey;

                    System.out.println("Processing: " + inputPath + " -> " + outputPath);

                    // Compress the image with client-specified parameters
                    CompressionService.CompressionResult result =
                        CompressionService.compress(inputPath, outputPath, method, threshold);

                    if (result.success) {
                        System.out.println("Success! EQM: " + result.eqm + ", Time: " + result.executionTimeMs + "ms");
                        processed++;

                        // Send email notification via SNS
                        sendNotification(objectKey, outputPath, method, threshold, result);
                    } else {
                        System.out.println("Failed: " + result.message);

                        // Send failure notification
                        sendFailureNotification(objectKey, result.message);
                    }
                }
            }

            return "Processed " + processed + " images";

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private void sendNotification(String fileName, String outputPath, String method, int threshold, CompressionService.CompressionResult result) {
        if (SNS_TOPIC_ARN == null || SNS_TOPIC_ARN.isEmpty()) {
            System.out.println("SNS_TOPIC_ARN not configured, skipping notification");
            return;
        }

        String subject = "Image Compression Complete: " + fileName;
        String message = String.format(
            "Your image has been compressed successfully!\n\n" +
            "File: %s\n" +
            "Output: %s\n" +
            "Method: %s\n" +
            "Threshold: %d\n" +
            "EQM (Error): %.4f\n" +
            "Processing Time: %d ms\n\n" +
            "You can download your compressed image from the output S3 bucket.",
            fileName, outputPath, method, threshold, result.eqm, result.executionTimeMs
        );

        try {
            snsClient.publish(SNS_TOPIC_ARN, message, subject);
            System.out.println("Notification sent successfully");
        } catch (Exception e) {
            System.out.println("Failed to send notification: " + e.getMessage());
        }
    }

    private void sendFailureNotification(String fileName, String errorMessage) {
        if (SNS_TOPIC_ARN == null || SNS_TOPIC_ARN.isEmpty()) {
            return;
        }

        String subject = "Image Compression Failed: " + fileName;
        String message = String.format(
            "Image compression failed.\n\n" +
            "File: %s\n" +
            "Error: %s\n\n" +
            "Please check the file and try again.",
            fileName, errorMessage
        );

        try {
            snsClient.publish(SNS_TOPIC_ARN, message, subject);
        } catch (Exception e) {
            System.out.println("Failed to send failure notification: " + e.getMessage());
        }
    }
}
