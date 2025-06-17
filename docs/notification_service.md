# Notification Sender Service

This document outlines a high-level design of a service responsible for delivering notifications to users. It aims to keep the description short and clear so it can be easily updated when the implementation evolves.

## Goals

* Allow users to register their interest in specific events.
* Receive notifications from other internal services when notable actions occur.
* Match notifications to subscribed users and determine their preferred communication channel.
* Send messages using an external provider or a pluggable sending mechanism.

## Components

### 1. Subscription API

A small REST API where clients can subscribe to event types. Each subscription stores the user identifier, notification channel preferences (email, SMS, etc.) and optional filters such as feed or geographic area.

### 2. Notification Ingest

Other services publish notification events to a message queue. The sender service consumes them and enriches the payload with additional data if needed.

### 3. Recipient Matcher

The matcher resolves which users should receive a particular notification. It queries the subscription storage and groups the recipients by their preferred channels.

### 4. Delivery Workers

Dedicated workers process grouped notifications and push them to the actual delivery provider. Implementations for email and push messages can be added gradually.

## Data Model

```
subscription(id: uuid, user_id: uuid, event_type: text, filters: jsonb,
             channels: text[], created_at: timestamptz)
```

`filters` may contain optional criteria such as feed aliases or bounding boxes. Each user can register multiple subscriptions.

## Sequence

1. User registers a subscription via the API.
2. A different service emits a message about a new event to the queue.
3. Notification Ingest picks the message and forms a unified notification.
4. Recipient Matcher finds all subscriptions that match the event.
5. Delivery Workers send messages to users according to their channel preferences.

## Considerations

* The queue can be RabbitMQ or AWS SNS/SQS.
* Delivery implementations should have retry policies and record delivery status.
* Subscriptions may expire or require confirmation emails.

