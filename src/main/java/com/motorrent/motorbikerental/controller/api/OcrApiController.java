package com.motorrent.motorbikerental.controller.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/ocr")
public class OcrApiController {

    @Value("${google.cloud.vision.api-key}")
    private String googleApiKey;

    @PostMapping("/cccd")
    public ResponseEntity<String> scanCccd(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error_code\": 1, \"error_message\": \"File is empty\"}");
        }

        try {
            // 1. Encode image to Base64
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

            // 2. Prepare Google Vision API Request
            String apiUrl = "https://vision.googleapis.com/v1/images:annotate?key=" + googleApiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{"
                    + "\"requests\": [{"
                    + "\"image\": {\"content\": \"" + base64Image + "\"},"
                    + "\"features\": [{\"type\": \"DOCUMENT_TEXT_DETECTION\"}]"
                    + "}]"
                    + "}";

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = new RestTemplate();
            
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 3. Parse Google Vision Response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode responses = root.path("requests").isMissingNode() ? root.path("responses") : root.path("responses");
            
            if (responses.isEmpty() || responses.get(0).path("fullTextAnnotation").isMissingNode()) {
                return ResponseEntity.ok("{\"error_code\": 1, \"error_message\": \"Không tìm thấy chữ nào trên ảnh\"}");
            }

            String fullText = responses.get(0).path("fullTextAnnotation").path("text").asText();
            
            // 4. Custom Regex Parser for CCCD
            String id = extractByRegex(fullText, "(?i)(?:Số|No\\.?)\\s*[:/]*\\s*(\\d{12})");
            String dob = extractByRegex(fullText, "(?i)Ngày sinh.*?(\\d{2}/\\d{2}/\\d{4})");
            String name = extractField(fullText, new String[]{"họ và tên", "full name"}, false);
            String address = extractField(fullText, new String[]{"nơi thường trú", "residence"}, true);

            // Xử lý làm sạch chuỗi
            if (name != null) name = name.replaceAll("(?i)(ngày sinh|date of birth).*", "").trim();
            if (address != null) address = address.replaceAll("(?i)(có giá trị đến|date of expiry).*", "").trim();

            // 5. Build output matching Zalo API format for frontend
            ObjectNode responseData = mapper.createObjectNode();
            responseData.put("error_code", 0);
            responseData.put("error_message", "Success");
            
            ArrayNode dataArray = mapper.createArrayNode();
            ObjectNode dataItem = mapper.createObjectNode();
            ObjectNode info = mapper.createObjectNode();
            info.put("id", id != null ? id : "");
            info.put("name", name != null ? name : "");
            info.put("dob", dob != null ? dob : "");
            info.put("address", address != null ? address : "");
            
            dataItem.set("info", info);
            dataArray.add(dataItem);
            responseData.set("data", dataArray);

            return ResponseEntity.ok(responseData.toString());

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("Google API Error: " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body("{\"error_code\": 1, \"error_message\": \"Google API Error. Please check API Key.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("{\"error_code\": 1, \"error_message\": \"Lỗi Server: " + e.getMessage() + "\"}");
        }
    }

    private String extractByRegex(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractField(String text, String[] keywords, boolean includeNextLine) {
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String lowerLine = lines[i].toLowerCase();
            boolean match = false;
            for (String kw : keywords) {
                if (lowerLine.contains(kw)) {
                    match = true;
                    break;
                }
            }
            
            if (match) {
                StringBuilder result = new StringBuilder();
                int colonIndex = lines[i].indexOf(":");
                
                if (colonIndex != -1 && colonIndex + 1 < lines[i].length()) {
                    result.append(lines[i].substring(colonIndex + 1).trim());
                } else {
                    // Xóa các nhãn phổ biến trên dòng để lấy phần giá trị
                    String cleaned = lines[i].replaceAll("(?i)(họ và tên|full name|nơi thường trú|place of residence)[/\\s]*", "").trim();
                    if (cleaned.length() > 3 && !cleaned.equals(lines[i].trim())) {
                        result.append(cleaned);
                    }
                }

                if (includeNextLine && i + 1 < lines.length) {
                    String nextLine = lines[i + 1].trim();
                    String lowerNext = nextLine.toLowerCase();
                    // Bỏ qua nếu dòng tiếp theo là một nhãn khác
                    if (!lowerNext.contains("có giá trị") && !lowerNext.contains("expiry") 
                        && !lowerNext.contains("ngày sinh") && !lowerNext.contains("date of birth")
                        && !lowerNext.contains("đặc điểm") && !lowerNext.contains("nơi cấp")) {
                        
                        if (result.length() > 0 && !result.toString().endsWith(",")) {
                            result.append(", ");
                        } else if (result.length() > 0) {
                            result.append(" ");
                        }
                        result.append(nextLine);
                    }
                } else if (result.length() == 0 && i + 1 < lines.length) {
                    // Dữ liệu nằm hoàn toàn ở dòng dưới (thường gặp ở Họ và tên)
                    result.append(lines[i + 1].trim());
                }
                
                // Clean up leading characters
                String finalResult = result.toString().trim();
                while (finalResult.startsWith("/") || finalResult.startsWith(",") || finalResult.startsWith(":")) {
                    finalResult = finalResult.substring(1).trim();
                }
                
                return finalResult;
            }
        }
        return null;
    }
}
