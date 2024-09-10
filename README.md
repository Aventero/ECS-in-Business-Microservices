# ECS-in-Business-Microservices
Evaluation of the Entity Component System Architecture in Microservice-Based Business Applications

## Code
Contains two root projects that implemented an e-commerce application.
These implement different types of architectures:
- Entity Component System architecture
- Traditional layered architecture
Each root project contains the modules of each service.

## Requirements
- Running docker environment to deploy databases for authservice, productservice and orderservice.
- Java 21 

## How to run
1. Open one of the root projects
2. Run docker-compose in the root project. This runs the databases in docker.
3. Run 'mvn clean install' in the root project.
4. Run the services: gatewayservice, authservice, productservice and orderservice

### Gatewayservice
- Routes all request to the correct service.

### Authservice
Used for auth and role management. Client receives JSON-web-token on login. Must be used on every request afterwards.
- Register/Login/Logout
- Add/Remove roles

### Productservice
Does productmanagement like:
- create/removing products
- searching for products.

### Orderservice
Responsible for shopping cart of a user:
- Add/Remove item to the cart
- Empty the cart

As well as ordering:
- Place an order
- See order history
