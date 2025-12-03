# Order Service

## Overview

The **Order Service** is the orchestration hub of the Food Ordering System. It manages order creation, status tracking, and coordinates with other services through both synchronous REST calls and asynchronous messaging via RabbitMQ.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Data JPA**: Database operations
- **Spring Web**: REST API
- **Spring AMQP**: RabbitMQ integration
- **WebClient**: Inter-service communication
- **MySQL**: Database (order_db)
- **RabbitMQ**: Event-driven messaging
- **Lombok**: Boilerplate code reduction
- **Springdoc OpenAPI**: API documentation (Swagger)

## Port

- **Service Port**: 8083
- **Database Port**: 33063 (MySQL)

## Database

- **Database Name**: `order_db`
- **Tables**:
  - `orders` - Order information
  - `order_items` - Items in each order

### Order Schema
```sql
- id (Long, Primary Key)
- user_id (Long)
- restaurant_id (Long)
- amount (Double)
- status (Enum: PENDING, CONFIRMED, FAILED, CANCELLED)
- created_at (Timestamp)
- updated_at (Timestamp)
```

### Order Item Schema
```sql
- id (Long, Primary Key)
- order_id (Long, Foreign Key)
- menu_item_id (Long)
- quantity (Integer)
- price (Double)
- item_name (String)
```

## API Endpoints

### Swagger Documentation
- **Swagger UI**: http://localhost:8083/swagger-ui/index.html
- **OpenAPI Spec**: http://localhost:8083/v3/api-docs

### Order Endpoints

#### Create Order
```http
POST /orders
Content-Type: application/json
Authorization: Bearer {jwt-token}

{
  "restaurantId": 1,
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2
    },
    {
      "menuItemId": 2,
      "quantity": 1
    }
  ]
}
```

**Response**:
```json
{
  "id": 1,
  "userId": 1,
  "restaurantId": 1,
  "amount": 899.97,
  "status": "PENDING",
  "createdAt": "2025-12-03T10:00:00",
  "items": [
    {
      "id": 1,
      "menuItemId": 1,
      "itemName": "Margherita Pizza",
      "quantity": 2,
      "price": 299.99
    },
    {
      "id": 2,
      "menuItemId": 2,
      "itemName": "Garlic Bread",
      "quantity": 1,
      "price": 299.99
    }
  ]
}
```

#### Get Order by ID
```http
GET /orders/{orderId}
Authorization: Bearer {jwt-token}
```

**Response**:
```json
{
  "id": 1,
  "userId": 1,
  "restaurantId": 1,
  "amount": 899.97,
  "status": "CONFIRMED",
  "createdAt": "2025-12-03T10:00:00",
  "updatedAt": "2025-12-03T10:01:00",
  "items": [...]
}
```

#### Get User Orders
```http
GET /orders/user/{userId}
Authorization: Bearer {jwt-token}
```

**Response**:
```json
[
  {
    "id": 1,
    "restaurantId": 1,
    "amount": 899.97,
    "status": "CONFIRMED",
    "createdAt": "2025-12-03T10:00:00"
  }
]
```

#### Update Order Status (Internal)
```http
PUT /orders/{orderId}/status
Content-Type: application/json

{
  "status": "CONFIRMED"
}
```

## Order Status Flow

```
PENDING → CONFIRMED (on successful payment)
PENDING → FAILED (on payment failure)
CONFIRMED → CANCELLED (on user cancellation)
```

## Event-Driven Architecture

### Published Events

#### OrderCreatedEvent
Published to: `orders-exchange` with routing key `order.created`

```json
{
  "orderId": 1,
  "userId": 1,
  "restaurantId": 1,
  "amount": 899.97,
  "timestamp": "2025-12-03T10:00:00"
}
```

**Consumers**: Payment Service

### Consumed Events

#### PaymentResultEvent
Consumed from: `payment-result-queue` (bound to `payments-exchange`)

```json
{
  "orderId": 1,
  "success": true,
  "transactionId": "txn-uuid-123",
  "timestamp": "2025-12-03T10:01:00"
}
```

**Action**: Updates order status to CONFIRMED or FAILED

## Configuration

### application.properties

```properties
# Server Configuration
server.port=8083

# Database Configuration
spring.datasource.url=jdbc:mysql://mysql_order:3306/order_db
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# RabbitMQ Configuration
spring.rabbitmq.host=rabbitmq
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# RabbitMQ Exchange and Queue
rabbitmq.exchange.orders=orders-exchange
rabbitmq.routingkey.order-created=order.created
rabbitmq.queue.payment-result=payment-result-queue

# Service URLs (for WebClient)
service.restaurant.url=http://restaurant-service:8082

# Application Name
spring.application.name=order-service
```

## Building the Service

### Prerequisites
- Java 17+
- Maven 3.6+

### Build Commands

```bash
# Build the service
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Run locally
mvn spring-boot:run
```

## Docker

### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build Docker Image
```bash
docker build -t order-service:latest .
```

### Run with Docker Compose
```bash
# From project root
docker-compose up order-service
```

## Dependencies

Key dependencies include:
- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-data-jpa` - Database operations
- `spring-boot-starter-amqp` - RabbitMQ integration
- `spring-boot-starter-webflux` - WebClient for inter-service calls
- `spring-boot-starter-actuator` - Health checks and metrics
- `spring-boot-starter-security` - JWT authentication
- `mysql-connector-j` - MySQL driver
- `lombok` - Code generation
- `springdoc-openapi-starter-webmvc-ui` - Swagger documentation

## Project Structure

```
order-service/
├── src/main/java/com/foodorder/order/
│   ├── controller/
│   │   └── OrderController.java
│   ├── service/
│   │   ├── OrderService.java
│   │   └── OrderCalculationService.java
│   ├── repository/
│   │   ├── OrderRepository.java
│   │   └── OrderItemRepository.java
│   ├── model/
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   └── OrderStatus.java
│   ├── dto/
│   │   ├── OrderRequest.java
│   │   ├── OrderResponse.java
│   │   └── OrderItemRequest.java
│   ├── event/
│   │   ├── OrderCreatedEvent.java
│   │   └── PaymentResultEvent.java
│   ├── messaging/
│   │   ├── OrderEventPublisher.java
│   │   └── PaymentEventListener.java
│   ├── config/
│   │   ├── RabbitMQConfig.java
│   │   ├── WebClientConfig.java
│   │   ├── SecurityConfig.java
│   │   └── SwaggerConfig.java
│   └── OrderServiceApplication.java
├── src/main/resources/
│   └── application.properties
├── Dockerfile
├── pom.xml
└── README.md
```

## Inter-Service Communication

### Synchronous (REST via WebClient)

#### Outbound Calls
- **Restaurant Service** → GET `/restaurants/menu/{menuItemId}`
  - Purpose: Validate menu items and fetch current prices
  - Used during order creation

### Asynchronous (RabbitMQ)

#### Publishes To
- **orders-exchange** (Topic Exchange)
  - Routing Key: `order.created`
  - Event: `OrderCreatedEvent`
  - Consumer: Payment Service

#### Listens To
- **payment-result-queue**
  - Bound to: `payments-exchange`
  - Routing Key: `payment.result`
  - Event: `PaymentResultEvent`
  - Action: Update order status

## Business Logic

### Order Creation Flow

1. **Validate Request**: Check user authentication and request data
2. **Fetch Menu Items**: Call Restaurant Service to get menu item details and prices
3. **Calculate Total**: Server-side calculation of order amount (security measure)
4. **Create Order**: Save order with PENDING status
5. **Publish Event**: Send OrderCreatedEvent to RabbitMQ
6. **Return Response**: Return order details to client

### Order Calculation

```java
// Server-side calculation prevents price manipulation
for (OrderItemRequest item : request.getItems()) {
    MenuItem menuItem = restaurantService.getMenuItem(item.getMenuItemId());
    double itemTotal = menuItem.getPrice() * item.getQuantity();
    totalAmount += itemTotal;
}
```

### Payment Result Handling

1. **Listen for Event**: PaymentResultEvent from payment-result-queue
2. **Update Status**: 
   - Success → CONFIRMED
   - Failure → FAILED
3. **Save Changes**: Persist updated order status
4. **Notification**: Notification Service also receives the event

## Security

- **JWT Authentication**: Required for all endpoints
- **User Authorization**: Users can only access their own orders
- **Server-Side Calculation**: Order amounts calculated on server to prevent tampering
- **Input Validation**: All inputs validated before processing

## Testing

### Manual Testing with cURL

```bash
# Create an order (requires JWT token)
curl -X POST http://localhost:8083/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {your-jwt-token}" \
  -d '{
    "restaurantId": 1,
    "items": [
      {
        "menuItemId": 1,
        "quantity": 2
      }
    ]
  }'

# Get order by ID
curl http://localhost:8083/orders/1 \
  -H "Authorization: Bearer {your-jwt-token}"

# Get user orders
curl http://localhost:8083/orders/user/1 \
  -H "Authorization: Bearer {your-jwt-token}"
```

### Via API Gateway

```bash
# Create order via gateway
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{...}'

# Get order via gateway
curl http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer {token}"
```

### End-to-End Testing

See the project's demo scripts:
- `demo_order_flow.sh` - Complete order flow demonstration
- `simple_order_test.sh` - Simple order creation test
- `test_order_flow.sh` - Automated testing script

## Monitoring & Health

### Actuator Endpoints
- **Health**: http://localhost:8083/actuator/health
- **Info**: http://localhost:8083/actuator/info
- **Metrics**: http://localhost:8083/actuator/metrics

### RabbitMQ Monitoring
- Check message flow in RabbitMQ Management UI: http://localhost:15672
- Monitor `orders-exchange` and `payment-result-queue`

## Troubleshooting

### Common Issues

1. **Order Creation Fails - Menu Item Not Found**
   ```
   Cause: Restaurant Service is down or menu item doesn't exist
   Solution: Verify Restaurant Service is running and menu item exists
   ```

2. **Order Stuck in PENDING Status**
   ```
   Cause: Payment Service not processing events or RabbitMQ connection issue
   Solution: Check RabbitMQ logs and Payment Service status
   ```

3. **WebClient Connection Timeout**
   ```
   Cause: Restaurant Service unreachable
   Solution: Verify service discovery and network connectivity
   ```

4. **Database Connection Failed**
   ```bash
   # Check MySQL container
   docker ps | grep mysql_order
   docker logs mysql_order
   ```

## Data Validation

### Order Request Validation
- Restaurant ID: Required, must be positive
- Items: Required, non-empty array
- Menu Item ID: Required for each item
- Quantity: Required, must be positive

### Business Rules
- Minimum order amount: None (configurable)
- Maximum items per order: None (configurable)
- Order timeout: Configurable (default: no timeout)

## Environment Variables

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://mysql_order:3306/order_db
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
SPRING_RABBITMQ_HOST=rabbitmq
SERVICE_RESTAURANT_URL=http://restaurant-service:8082
SERVER_PORT=8083
```

## Performance Considerations

- **Async Processing**: Payment processing is asynchronous
- **WebClient**: Non-blocking HTTP client for inter-service calls
- **Connection Pooling**: Database connection pooling enabled
- **Indexing**: Order ID and User ID indexed for fast queries

## Saga Pattern Implementation

The Order Service implements the **Saga Pattern** for distributed transactions:

1. **Order Created** → Publish event
2. **Payment Processed** → Receive result event
3. **Order Updated** → Based on payment result
4. **Compensation**: If payment fails, order status set to FAILED

See [PAYMENT_SAGA_PATTERN.md](../PAYMENT_SAGA_PATTERN.md) for detailed documentation.

## Future Enhancements

- [ ] Order cancellation workflow
- [ ] Order modification support
- [ ] Delivery tracking integration
- [ ] Order history with pagination
- [ ] Scheduled orders
- [ ] Recurring orders
- [ ] Order analytics and reporting
- [ ] Inventory management integration

## Contributing

When contributing to this service:
1. Maintain event-driven architecture patterns
2. Use WebClient for inter-service communication
3. Implement proper error handling for distributed calls
4. Update both REST and messaging documentation
5. Add integration tests for event flows
6. Follow the Saga pattern for transactions

## Related Documentation

- [Main Project README](../README.md)
- [API Documentation](../API_DOCUMENTATION.md)
- [Architecture Overview](../ARCHITECTURE.md)
- [Payment Saga Pattern](../PAYMENT_SAGA_PATTERN.md)
- [Order Flow Documentation](../ORDER_FLOW_DOCUMENTATION.md)
- [Restaurant Service README](../restaurant-service/README.md)
- [Payment Service README](../payment-service/README.md)

## License

Part of the Food Ordering Microservices System - A demonstration project for microservices architecture.
