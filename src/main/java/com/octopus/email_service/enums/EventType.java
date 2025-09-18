package com.octopus.email_service.enums;

public enum EventType {
    ENQUEUED, SENDING, SENT, DELIVERED, BOUNCED, OPEN, CLICK, 
    SOFT_BOUNCE, HARD_BOUNCE, FAILED
}
