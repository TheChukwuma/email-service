package com.octopus.email_service.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HTML detection logic.
 * Tests the HTML detection logic without Spring context dependency.
 */
public class EmailServiceHtmlDetectionUnitTest {

    /**
     * Helper method that replicates the HTML detection logic from EmailService
     */
    private boolean isHtmlContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        content = content.trim().toLowerCase();
        
        // Check for HTML doctype declaration
        if (content.startsWith("<!doctype html") || content.startsWith("<!doctype html>")) {
            return true;
        }
        
        // Check for opening HTML tag
        if (content.startsWith("<html")) {
            return true;
        }
        
        // Check for common HTML tags that indicate HTML content
        String[] htmlTags = {
            "<html", "<head", "<body", "<div", "<p>", "<br>", "<br/>", "<br />",
            "<span", "<h1", "<h2", "<h3", "<h4", "<h5", "<h6", "<ul", "<ol", "<li",
            "<table", "<tr", "<td", "<th", "<img", "<a", "<strong", "<b>", "<em", "<i>",
            "<style", "<script", "<meta", "<title", "<link"
        };
        
        for (String tag : htmlTags) {
            if (content.contains(tag)) {
                return true;
            }
        }
        
        // Check for HTML entities
        if (content.contains("&lt;") || content.contains("&gt;") || 
            content.contains("&amp;") || content.contains("&nbsp;") || 
            content.contains("&quot;")) {
            return true;
        }
        
        return false;
    }

    @Test
    public void testHtmlDetection() {
        // Test HTML content patterns
        assertTrue(isHtmlContent("<html><body><h1>Hello World</h1></body></html>"));
        assertTrue(isHtmlContent("<!DOCTYPE html><html><head></head><body>Content</body></html>"));
        assertTrue(isHtmlContent("<div>Simple div content</div>"));
        assertTrue(isHtmlContent("<p>Paragraph with <strong>bold</strong> text</p>"));
        assertTrue(isHtmlContent("Text with <br> line break"));
        assertTrue(isHtmlContent("Content with &nbsp; HTML entity"));
        assertTrue(isHtmlContent("<img src='test.jpg' alt='Test Image'>"));
        assertTrue(isHtmlContent("<a href='#'>Link text</a>"));

        // Test plain text content
        assertFalse(isHtmlContent("This is plain text content without HTML tags."));
        assertFalse(isHtmlContent("Just regular text with some words."));
        assertFalse(isHtmlContent("Email content with line breaks\nand multiple lines\nbut no HTML."));

        // Test edge cases
        assertFalse(isHtmlContent(null));
        assertFalse(isHtmlContent(""));
        assertFalse(isHtmlContent("   "));

        // Test false positives (text that mentions HTML but isn't HTML)
        assertFalse(isHtmlContent("Please check the HTML documentation for more info."));
        assertFalse(isHtmlContent("The file type should be HTML or PDF."));
    }

    @Test
    public void testCaseInsensitiveHtmlDetection() {
        // Test case insensitive detection
        assertTrue(isHtmlContent("<HTML><BODY><H1>UPPERCASE HTML</H1></BODY></HTML>"));
        assertTrue(isHtmlContent("<Html><Body><P>Mixed Case HTML</P></Body></Html>"));
        assertTrue(isHtmlContent("<DIV>UPPERCASE DIV</DIV>"));
    }

    @Test
    public void testHtmlEntitiesDetection() {
        // Test HTML entities
        assertTrue(isHtmlContent("Content with &lt;escaped&gt; HTML"));
        assertTrue(isHtmlContent("Text with &amp; ampersand"));
        assertTrue(isHtmlContent("Non-breaking&nbsp;space"));
        assertTrue(isHtmlContent("Quote: &quot;Hello World&quot;"));
    }

    @Test
    public void testDocTypeDetection() {
        // Test DOCTYPE declarations
        assertTrue(isHtmlContent("<!DOCTYPE html><html><body>Content</body></html>"));
        assertTrue(isHtmlContent("<!doctype html><html><body>Content</body></html>"));
    }
}
