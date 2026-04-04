package handler;

import java.util.HashMap;
import java.util.Map;
import service.CompressionService;

public class LambdaHandler {

    public Map<String, Object> handleRequest(Map<String, Object> input) {
        try {
            String inputPath = (String) input.get("inputPath");
            String outputPath = (String) input.get("outputPath");
            String method = (String) input.get("method");
            int threshold = ((Number) input.get("threshold")).intValue();

            CompressionService.CompressionResult result = CompressionService.compress(inputPath, outputPath, method, threshold);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.success);
            response.put("message", result.message);
            response.put("outputPath", result.outputPath);
            response.put("eqm", result.eqm);
            response.put("executionTimeMs", result.executionTimeMs);

            return response;

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error: " + e.getMessage());
            return errorResponse;
        }
    }
}
