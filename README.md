# Comparison of Session Caching in OAuth Workflow

## Table of Contents
- Overview
- Features
- Directory Structure
- Get Started
  - Prerequisites 
    - Quickstart using precompiled containers and helm structure
    - Build from source
  - Quick Start
  - Building Project From Source
- Examples
- Future work 

## Overview
Compares caching mechanisms using Redis, Hazelcast, and Memcached in the OAuth2 workflow using Spring Boot 3

## Features
- OAuth2 Authorization Server: Implements OAuth2 flows using Spring Security.
- Resource server: Integrates with the authorization server and shared cache for blacklisted tokens
- Caching Mechanisms: Saves user sessions to Redis, Hazelcast, and Memcached based on flags in the request
- JWT tokens: Secure structure for sessions
- Helm: Manages templating, turn key charts such as the caching, services, and revisioning the kubernetes cluster.
- Containerization: Client, Auth Server, and Resource server are containerized using jib and docker
- Actuator: Spring Boot Actuator is used to verify network connections on dependant services and provide a health check of the service 
  
## Directory Structure
This project is a multimodule project with the following structure 
- Parent (the root pom and structure that defines the dependencies)
  - auth-server: The authorization server module
  - cache-config: A configuration module that is shared between auth-server and resource-server to access caches
  - client: The client that calls auth-server and then resource-server (not yet implemented)
  - helm: Contains all kubernetes resources including helm charts, configurations, etc. 
  - resource-server: the resource server that just returns the name of the cache that it used to validate the session (not yet implemented


## Getting Started
### Prerequisites
#### Quickstart using precompiled containers and helm structure
- Helm
- Kubernetes/minikube
- Docker

#### Build from source 
- Java 21
- Docker
- Helm
- Maven 3.8 or greater
- Kubernetes/minikube

### Quickstart
Containers are defined in the helm charts, maven, pom.xml as well as on docker. No changes are needed

Start minikube 
```shell
minikube start
```
Deploy the helm charts which will pull down all the charts and containers you need
```shell
helm install oauth-cache-comparision ./helm
```
Start networking service 
```shell
 minikube service --all  
```
Proceed to example

### Building Project From Source
- go to the root pom.xml
- update the value ```docker-path``` to your docker hub account
```shell
mvn clean install -DskipTests=true jib:build
```
-DskipTests=true is needed right now tests are broken. In order for spring to start the stateful containers 
are needed and I haven't figured out how to get a work around. These containers are pulled in from helm when the service starts
but are not available during the testing phase.
This will build and deploy your containers to your docker hub 
- Update helm charts to point to your repo instead of jasonbuchanan145
- Do the same steps as quickstart


### Example Authorization Server Request
Cache session in redis and return authorization and refresh tokens
```shell
curl -X POST  http://127.0.0.1:50389/oauth2/token `
      -H "Content-Type: application/x-www-form-urlencoded" `
      -H "cache-type: redis" `
      -u "ThisIsMyClientId:myClientSecret" `
      -d "grant_type=authorization_code&client_id=ThisIsMyClientId&client_secret=myClientSecret"
```
```
{"access_token":"eyJraWQiOiI1OWMwZjI4Zi05NjBjLTQ0NGYtODdkMi0wMDg0MjVhZDY0N2IiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJUaGlzSXNNeUNsaWVudElkIiwiYXVkIjoiVGhpc0lzTXlDbGllbnRJZCIsIm5iZiI6MTczMDA3NTk4Nywic2NvcGUiOlsid3JpdGUiLCJvcGVuaWQiLCJyZWFkIl0sImlzcyI6Imh0dHA6Ly8xMjcuMC4wLjE6NTAzODkiLCJleHAiOjE3MzAwNzYyODcsImlhdCI6MTczMDA3NTk4NywianRpIjoiNGJhZmUzNmUtNzlkNS00MWUxLWIxMmYtMTI3NTdmOTIwMzZiIn0.ZmgaadrDyPe8wTAkduKTMtg6-rrttvdHU_nRF2Fa4SL8x1_1gNBoHz8jSrbv2Jfk8r2uYZXIS1bJP5rJ78djJzW-3an0KpSlz04PouwmkdSgBiW38icJBP_DgKCA3dQV0J6YoQeUpGVAMQrK1cMWuMgI0emBV-e7-mBXBMW2qoWqnsAfFcpcZd3XTMTzLGT-MDUStNj6aleP3Wg50XC2Y_IBoy9-0ChUMOn8rjCnnVT4TXoj8iXgnt8yFYDYiSitWVF0che8Q9Bfq1ckI1f1tWl9LZzxGMFdaw7MiTUATeEM4UyMpGXKWmov7hAVx4jgHFHR_cP899CSgKivZXYsYg","refresh_token":"nx-1OQUlQL88Dz1mm6maNCOl8i50D-xV-80ESQVdvRS4TTcglAcfnMg8WF7PXH2GVJ8ww6ga121Up_ll9ulik5shb6UMvGkMp5FGu3oGmraDqQwpT9_RyuUdLbBZ0t6g","scope":"read openid write","token_type":"Bearer","expires_in":299}
```
Saving in hazelcast
```shell
curl -X POST  http://127.0.0.1:50389/oauth2/token `
      -H "Content-Type: application/x-www-form-urlencoded" `
      -H "cache-type: hazelcast" `
      -u "ThisIsMyClientId:myClientSecret" `
      -d "grant_type=authorization_code&client_id=ThisIsMyClientId&client_secret=myClientSecret"
```
```
{"access_token":"eyJraWQiOiI1OWMwZjI4Zi05NjBjLTQ0NGYtODdkMi0wMDg0MjVhZDY0N2IiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJUaGlzSXNNeUNsaWVudElkIiwiYXVkIjoiVGhpc0lzTXlDbGllbnRJZCIsIm5iZiI6MTczMDA3NjE4NSwic2NvcGUiOlsicmVhZCIsIm9wZW5pZCIsIndyaXRlIl0sImlzcyI6Imh0dHA6Ly8xMjcuMC4wLjE6NTAzODkiLCJleHAiOjE3MzAwNzY0ODUsImlhdCI6MTczMDA3NjE4NSwianRpIjoiNThkYmQ4ZTktYTcyZC00Mjc3LTlhODAtYzkyMmFjYzBmYTE3In0.eZdGGAIZvTpCw9xjJtmlwZXGawQwlmsdWIxoN74NzWWJB77AumRhcwCbIPc4-Ryap9Rk6bwxDgaIJNRALeF-nJT793iezmAWpM55vDn6lm7m0bXckOG3tCG5wuKcDs2mjU4MfVWmIFr2_eb4mRNgSEqvdjp3aAAHOpIpZaT1S3m5lDvFB6_uyATGqG9nTYjdmtirNo9EYLVE3w6krLJYeKWsJBTlqETqiUWe9AlgdlOMHpfKmult1LZDQ75KKb8a__g4LANb4NanfKKMMGjEvOTfZ1fqKnB4qJAUYSUdYdAajs2Kuh7IdoFVJqL8x07YL7-x2qFtO7AyBju1_Gjplw","refresh_token":"xH5UtfOhihp16tbj7LhVEUEQk92gAG2yuE_c5tDVXWzRJE6muizjU2Z7q1R3-gD0-RQRHrYGpSnuaD7eW3Fc0OyV5bvln6doXpCQR2IwF3w5pC01qlTYkcDuzj6vayFD","scope":"read openid write","token_type":"Bearer","expires_in":299}
```

Saving in memcached (currently broken)
```shell
 curl -X POST  http://127.0.0.1:50389/oauth2/token `
      -H "Content-Type: application/x-www-form-urlencoded" `
      -H "cache-type: memcached" `
      -u "ThisIsMyClientId:myClientSecret" `
      -d "grant_type=authorization_code&client_id=ThisIsMyClientId&client_secret=myClientSecret"
```
```
{"timestamp":"2024-10-28T00:44:36.581+00:00","status":500,"error":"Internal Server Error","path":"/oauth2/token"}
```

### Example resource server examples
TODO:

### Example client usage
TODO: 

### Future work
There's so much work to do on this project so here's a priority list
### Top priority
- Finish the resource server
- work on the client

### Medium priority 
- Finish readme including architecture diagrams
- Code cleanup
