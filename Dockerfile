FROM maven:3.6.3-jdk-8-slim AS builder

WORKDIR /presto-gateway
COPY baseapp                         /presto-gateway/baseapp
COPY proxyserver                     /presto-gateway/proxyserver
COPY gateway-ha                      /presto-gateway/gateway-ha
COPY pom.xml /presto-gateway/


RUN mvn -Dmaven.artifact.threads=100  clean install  -Dmaven.test.skip=true -Dcheckstyle.skip
RUN curl -L https://github.com/a8m/envsubst/releases/download/v1.1.0/envsubst-`uname -s`-`uname -m` -o envsubst
RUN chmod +x envsubst

FROM openjdk:8u242-jre-slim


RUN apt-get update
RUN apt-get install -y mysql\*

WORKDIR /var/lib/presto-gateway

COPY --from=builder /presto-gateway/gateway-ha/target/gateway-ha-*-jar-with-dependencies.jar /var/lib/presto-gateway/gateway-ha.jar
COPY --from=builder /presto-gateway/gateway-ha/src/main/resources/gateway-ha-persistence.sql /var/lib/presto-gateway/gateway-ha-persistence.sql
COPY --from=builder /presto-gateway/envsubst /usr/local/bin
COPY ./entrypoint.sh /entrypoint.sh


EXPOSE 8080 8090 8091

CMD  [ "/entrypoint.sh" ] 

