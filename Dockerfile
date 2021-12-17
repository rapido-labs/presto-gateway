FROM maven:3.6.3-jdk-8-slim AS builder

WORKDIR /presto-gateway
COPY baseapp                         /presto-gateway/baseapp
COPY proxyserver                     /presto-gateway/proxyserver
COPY gateway-ha                      /presto-gateway/gateway-ha
COPY pom.xml /presto-gateway/


RUN mvn -Dmaven.artifact.threads=100  clean install 
# -Dmaven.test.skip=true


FROM openjdk:8u242-jre-slim

WORKDIR /var/lib/presto-gateway

COPY --from=builder /presto-gateway/gateway-ha/target/gateway-ha-*-jar-with-dependencies.jar /var/lib/presto-gateway/gateway-ha.jar

EXPOSE 8080 8090 8091

CMD  java -jar gateway-ha.jar server gateway-ha-config.yml
