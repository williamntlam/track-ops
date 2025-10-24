# Hexagonal Architecture (Ports and Adapters)

## Overview

TrackOps follows the Hexagonal Architecture pattern, also known as Ports and Adapters. This architecture isolates the core business logic from external concerns.

## Core Principles

### 1. Domain-Centric Design
- Business logic lives in the `domain` package
- No dependencies on external frameworks
- Pure Java objects with business rules

### 2. Ports (Interfaces)
- **Input Ports**: Define what the application can do
- **Output Ports**: Define what the application needs from outside

### 3. Adapters (Implementations)
- **Input Adapters**: Handle incoming requests (REST, Kafka, etc.)
- **Output Adapters**: Handle outgoing calls (Database, External APIs, etc.)

## Package Structure

```
src/main/java/com/trackops/server/
├── domain/                    # Core business logic
│   ├── model/                # Domain entities and value objects
│   ├── events/               # Domain events
│   └── exceptions/            # Domain-specific exceptions
├── ports/                    # Ports (interfaces)
│   ├── input/                # Input ports (what app can do)
│   └── output/               # Output ports (what app needs)
└── adapters/                 # Adapters (implementations)
    ├── input/                # Input adapters (REST, Kafka, etc.)
    └── output/               # Output adapters (Database, Cache, etc.)
```

## Key Components

### Domain Layer
- **Entities**: Core business objects (Order, Customer, etc.)
- **Value Objects**: Immutable objects (Address, Money, etc.)
- **Domain Services**: Business logic that doesn't belong to entities
- **Events**: Domain events for decoupling

### Ports Layer
- **Input Ports**: Define application capabilities
- **Output Ports**: Define external dependencies

### Adapters Layer
- **Input Adapters**: REST controllers, Kafka consumers, etc.
- **Output Adapters**: Database repositories, external API clients, etc.

## Benefits

1. **Testability**: Easy to mock dependencies
2. **Flexibility**: Can swap implementations
3. **Maintainability**: Clear separation of concerns
4. **Independence**: Core logic doesn't depend on frameworks

## Implementation Guidelines

### Creating a New Feature
1. Start with domain model
2. Define input/output ports
3. Implement business logic
4. Create adapters for external concerns
5. Wire everything together with Spring

### Port Design
- Keep ports focused and cohesive
- Use descriptive method names
- Include proper error handling
- Document expected behavior

### Adapter Implementation
- Implement ports from the domain
- Handle external concerns (serialization, mapping, etc.)
- Use appropriate Spring annotations
- Keep adapters thin and focused
