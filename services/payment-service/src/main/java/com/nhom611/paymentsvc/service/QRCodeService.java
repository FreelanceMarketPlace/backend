package com.nhom611.paymentsvc.service;

import org.springframework.stereotype.Service;
import java.util.Base64;

/**
 * Service generate QR code (mock)
 * Sử dụng external API hoặc library để generate real QR
 */
@Service
public class QRCodeService {

    /**
     * Mock: Generate QR code using external API
     * Có thể sử dụng thực:
     * - goqr.me API (miễn phí)
     * - qrserver.com API
     * - zxing-java library
     */
    public String generateQRCode(String content, int size) {
        try {
            // Option 1: Sử dụng goqr.me API (free, no auth needed)
            // String qrApiUrl = String.format(
            //     "https://api.qrserver.com/v1/create-qr-code/?size=%dx%d&data=%s",
            //     size, size, URLEncoder.encode(content, StandardCharsets.UTF_8)
            // );
            
            // Option 2: Mock - tạo data URL giả lập
            String mockQRDataUrl = generateMockQRCodeDataUrl(content);
            return mockQRDataUrl;
            
            // Option 3: Thực - gọi API
            // RestTemplate restTemplate = new RestTemplate();
            // byte[] qrImageBytes = restTemplate.getForObject(qrApiUrl, byte[].class);
            // String base64 = Base64.getEncoder().encodeToString(qrImageBytes);
            // return "data:image/png;base64," + base64;
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh QR code: " + e.getMessage());
        }
    }

    /**
     * Mock: Tạo QR code image giả lập
     * Trong thực tế, sử dụng library như zxing hoặc gọi API
     */
    private String generateMockQRCodeDataUrl(String content) {
        // Tạo 1x1 transparent pixel PNG (giả lập)
        // Trong production, sử dụng thực QR code library
        
        // Simple SVG placeholder (không phải QR thật, chỉ để mock)
        String svgContent = String.format(
            "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'>" +
            "<rect width='200' height='200' fill='white'/>" +
            "<text x='50' y='100' font-size='12' fill='black'>%s</text>" +
            "</svg>",
            content.substring(0, Math.min(20, content.length()))
        );
        
        // Encode to base64
        String base64 = Base64.getEncoder().encodeToString(svgContent.getBytes());
        return "data:image/svg+xml;base64," + base64;
    }
}
