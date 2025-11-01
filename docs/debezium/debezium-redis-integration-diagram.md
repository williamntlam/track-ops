# Debezium Redis Cache Integration Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │     Debezium     │    │      Kafka      │
│   Database      │    │    Connector     │    │     Topics      │
│                 │    │                  │    │                 │
│ trackops_orders │───▶│ CDC Capture     │───▶│ trackops_orders │
│                 │    │                  │    │ .public.orders  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
                                                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Event Processing Layer                        │
├─────────────────┬─────────────────┬─────────────────────────────┤
│ DebeziumOrder   │ DebeziumRedis   │ DebeziumRedis               │
│ EventConsumer   │ CacheConsumer   │ CacheWarmer                 │
│                 │                 │                             │
│ • Publishes     │ • Invalidates   │ • Warms cache with         │
│   ORDER_CREATED │   related       │   fresh data               │
│   ORDER_CANCELLED│   cache entries│ • Updates TTL              │
│                 │ • Removes stale │ • Maintains consistency    │
│                 │   data          │                             │
└─────────────────┴─────────────────┴─────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Redis Cache Layer                          │
├─────────────────┬─────────────────┬─────────────────────────────┤
│ Order Entity    │ Order Status    │ Customer Orders             │
│ Cache           │ Cache           │ Cache                       │
│                 │                 │                             │
│ order:entity:   │ order:status:   │ orders:customer:           │
│ {orderId}       │ {orderId}       │ {customerId}               │
│                 │                 │                             │
│ TTL: 1 hour     │ TTL: 30 min     │ TTL: 1 hour                │
└─────────────────┴─────────────────┴─────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer                           │
├─────────────────┬─────────────────┬─────────────────────────────┤
│ Order Service   │ Inventory       │ Other Services              │
│                 │ Service         │                             │
│ • Reads from    │ • Consumes      │ • Benefits from             │
│   fresh cache   │   ORDER_CREATED │   consistent cache          │
│ • Faster        │   ORDER_CANCELLED│ • Reduced database load    │
│   responses     │ • Processes     │                             │
│                 │   inventory     │                             │
└─────────────────┴─────────────────┴─────────────────────────────┘

## Cache Invalidation Flow

Database Change → Debezium CDC → Cache Invalidation → Fresh Cache

1. Order created/updated in PostgreSQL
2. Debezium captures change event
3. DebeziumRedisCacheConsumer invalidates related cache entries
4. DebeziumRedisCacheWarmer updates cache with fresh data
5. Applications read from consistent, fresh cache

## Benefits

✅ Real-time cache invalidation
✅ Reduced cache staleness  
✅ Better performance
✅ Automatic cache management
✅ Consistent data across services
