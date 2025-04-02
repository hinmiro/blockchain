package org.example.config;

import org.springframework.context.ApplicationEvent;

public class WalletsCreatedEvent extends ApplicationEvent {
    public WalletsCreatedEvent(Object source) {
        super(source);
    }
}
