package com.handson.api.cpre.event;


import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

public class Event<K, T> {

    private Event.Type eventType;
    private K key;
    private T data;
    private LocalDateTime eventCreatedAt;
    public Event() {
        this.eventType = null;
        this.key = null;
        this.data = null;
        this.eventCreatedAt = null;
    }

    public Event(Type eventType, K key, T data) {
        this.eventType = eventType;
        this.key = key;
        this.data = data;
        this.eventCreatedAt = now();
    }

    public Type getEventType() {
        return eventType;
    }

    public K getKey() {
        return key;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getEventCreatedAt() {
        return eventCreatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;

        Event<?, ?> event = (Event<?, ?>) o;

        if (getEventType() != event.getEventType()) return false;
        if (getKey() != null ? !getKey().equals(event.getKey()) : event.getKey() != null) return false;
        if (getData() != null ? !getData().equals(event.getData()) : event.getData() != null) return false;
        return getEventCreatedAt() != null ? getEventCreatedAt().equals(event.getEventCreatedAt()) : event.getEventCreatedAt() == null;
    }

    @Override
    public int hashCode() {
        int result = getEventType() != null ? getEventType().hashCode() : 0;
        result = 31 * result + (getKey() != null ? getKey().hashCode() : 0);
        result = 31 * result + (getData() != null ? getData().hashCode() : 0);
        result = 31 * result + (getEventCreatedAt() != null ? getEventCreatedAt().hashCode() : 0);
        return result;
    }

    public enum Type {CREATE, DELETE}
}