# ECS-in-Business-Microservices
Evaluation of the Entity Component System Architecture in Microservice-Based Business Applications

## What is it?
This contains two root projects that implemented an e-commerce application.
These implement different types of architectures:
- Entity Component System (ECS) architecture
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
