FROM openjdk:21-slim

RUN apt update
RUN apt install wget -y
WORKDIR /opt/app
RUN wget -O Dynatrace-OneAgent-Linux-arm-1.281.120.20240108-110942.sh \
    "https://vab56136.live.dynatrace.com/api/v1/deployment/installer/agent/unix/default/latest?arch=arm" \
    --header="Authorization: Api-Token dt0c01.FZBUVHXRHUZKXRLBBMEA3QVR.DEHSNVJUMGNUL5VFQE7ZGDXCJBQAWWNOWALANNINFNYJLXPTPG2F6D3KN5BUEZBT"
#
#RUN wget https://ca.dynatrace.com/dt-root.cert.pem ; \
#    ( echo 'Content-Type: multipart/signed; protocol="application/x-pkcs7-signature"; micalg="sha-256"; boundary="--SIGNED-INSTALLER"'; \
#    echo ; echo ; echo '----SIGNED-INSTALLER' ; \
#    cat Dynatrace-OneAgent-Linux-arm-1.281.120.20240108-110942.sh ) | openssl cms -verify -CAfile dt-root.cert.pem > /dev/null

ADD payments-api/build/libs/payments-api.jar app.jar
RUN ls

ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]
#  | /bin/sh Dynatrace-OneAgent-Linux-arm-1.281.120.20240108-110942.sh --set-monitoring-mode=fullstack --set-app-log-content-access=true >> /dev/null 2>&1

#CMD [
#  "|",
#  "./Dynatrace-OneAgent-Linux-arm-1.281.120.20240108-110942.sh",
#  "--set-monitoring-mode=fullstack",
#  "--set-app-log-content-access=true",
#  ">>",
#  "/dev/null"
#]