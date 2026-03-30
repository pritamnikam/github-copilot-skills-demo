# Order Service — Copilot Instructions

## Service identity
Name: order-service
Language: Java 21 / Spring Boot 3.2
Port: 8080

## This service depends on
| Dependency       | Protocol | Base URL property                   |
|------------------|----------|-------------------------------------|
| user-service     | REST     | clients.user-service.url            |
| inventory-service| REST     | clients.inventory-service.url       |
| orders.created   | Kafka    | publish on order creation           |
| payments.processed| Kafka   | consume to update order status      |

## Test base class
Always extend: com.example.order.support.BaseIntegrationTest
Never create a new container setup — use what BaseIntegrationTest provides.

## WireMock stub paths
- user-service:      src/test/resources/wiremock/user-service/mappings/
- inventory-service: src/test/resources/wiremock/inventory-service/mappings/

## Key scenarios to always cover for placeOrder
- Happy path: user found + inventory reserved → 201 + event published
- User not found → 404 + no order persisted + inventory NOT called
- Insufficient inventory → 409 + no order persisted
- User service 503 → 503 propagated to caller
- Inventory service 503 → 503 propagated, no order persisted

## Key scenarios for cancelOrder
- Happy path: PENDING order → CANCELLED
- Order not found → 404
- Order already SHIPPED → 409 (cannot cancel)

## Kafka verification
After placeOrder happy path, always verify:
  kafkaTemplate sent to topic "orders.created"
  Event contains: orderId, userId, productId, quantity, status=PENDING

## Run integration tests
mvn test -Dtest="*IT" -pl order-service



# Order Service — Unit Test Standards

## Class under test naming
Always name the instance under test: sut
  @InjectMocks OrderService sut;

## Mocks to declare for OrderService
  @Mock UserClient          userClient;
  @Mock InventoryClient     inventoryClient;
  @Mock OrderRepository     orderRepository;
  @Mock KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

## Test data imports
Always import static:
  import static com.example.order.support.UnitTestSupport.*;

## Exception types to know
  UserNotFoundException         → when userClient.getUser() returns 404
  InventoryUnavailableException → when inventoryClient.reserve() returns 409
  OrderNotFoundException        → when order ID not found in repository
  OrderAlreadyShippedException  → when cancelling a SHIPPED order
  InvalidOrderException         → when quantity <= 0 or price <= 0

## Mandatory test scenarios for OrderService

### placeOrder(userId, productId, quantity, totalPrice)
  [ ] happy path → order persisted, event sent, returns order
  [ ] userId null → throws IllegalArgumentException
  [ ] userId blank → throws IllegalArgumentException
  [ ] quantity zero → throws InvalidOrderException
  [ ] quantity negative → throws InvalidOrderException
  [ ] totalPrice zero → throws InvalidOrderException
  [ ] userClient returns 404 → throws UserNotFoundException
  [ ] inventoryClient returns 409 → throws InventoryUnavailableException
  [ ] orderRepository.save() throws → exception propagates, event NOT sent
  [ ] verify: kafkaTemplate.send called exactly once on happy path
  [ ] verify: kafkaTemplate.send NOT called when user not found
  [ ] verify: kafkaTemplate.send NOT called when inventory fails

### cancelOrder(orderId)
  [ ] happy path PENDING → status set to CANCELLED, saved
  [ ] orderId null → throws IllegalArgumentException
  [ ] order not found → throws OrderNotFoundException
  [ ] order is SHIPPED → throws OrderAlreadyShippedException
  [ ] order already CANCELLED → throws InvalidOrderException
  [ ] verify: orderRepository.save called with CANCELLED status

## Coverage target for this service
  OrderService:    90% lines, 85% branches
  OrderValidator:  95% lines
  PricingEngine:   90% lines
  OrderController: 80% lines (unit — service mocked)