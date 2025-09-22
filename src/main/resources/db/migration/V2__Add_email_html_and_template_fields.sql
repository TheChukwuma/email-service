-- Add new fields to emails table for HTML detection and fallback template handling
ALTER TABLE emails
ADD COLUMN is_html_body BOOLEAN DEFAULT FALSE,
ADD COLUMN needs_fallback_template BOOLEAN DEFAULT FALSE,
ADD COLUMN reply_to_address VARCHAR(255);

-- Add comments for documentation
COMMENT ON COLUMN emails.is_html_body IS 'Indicates whether the email body contains HTML content';
COMMENT ON COLUMN emails.needs_fallback_template IS 'Indicates whether the email should use a fallback template for plain text content';
