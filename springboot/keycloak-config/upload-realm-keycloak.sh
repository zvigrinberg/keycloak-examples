#!/bin/bash
httpCode=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080)
while [ "${httpCode}" != "200" ]; do
    sleep 1
    httpCode=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080)
#    echo "http_code= $http_code" &>> /tmp/scripts/log.out
done
#sleep 10
ACCESS_TOKEN=$(curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token --data-urlencode 'grant_type=password' --data-urlencode 'client_id=admin-cli' --data-urlencode  'username='$KEYCLOAK_ADMIN'' --data-urlencode 'password='$KEYCLOAK_ADMIN_PASSWORD'' --header 'Content-Type: application/x-www-form-urlencoded' | grep access_token | awk -F ':' '{print $2}' | awk -F '\"' '{print $2}')
curl -X POST http://localhost:8080/admin/realms --header 'Authorization: Bearer '$ACCESS_TOKEN'' --header 'Content-Type: application/json' -T /tmp/scripts/spring-realm.json
