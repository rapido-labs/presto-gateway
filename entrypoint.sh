#!/bin/bash
set -eux

check_mysql_connection()
{
    connected=0
    counter=0

    echo "Wait 60 seconds for connection to MySQL"
    while [[ ${counter} -lt 60 ]]; do
        {
            /usr/bin/mysql -u"${DB_USER}" -p"${DB_PASS}" -h "${DB_HOST}" --port="${DB_PORT}" -e "SELECT 1" \
            | echo "Connecting to MySQL" &&
            connected=1

        } || {
            let counter=$counter+3
            sleep 3
        }
        if [[ ${connected} -eq 1 ]]; then
            echo "Connected"
            break;
        fi
    done

    if [[ ${connected} -eq 0 ]]; then
        echo "MySQL process failed."
        exit;
    fi
}

setup_mysql_dev_schema()
{
    check_mysql_connection

    echo "Setting up DB: mysql"
    /usr/bin/mysql -u"${DB_USER}" -p"${DB_PASS}" -h "${DB_HOST}" --port="${DB_PORT}" -D ${DB_NAME} < /var/lib/presto-gateway/gateway-ha-persistence.sql
}

check_mysql_connection
setup_mysql_dev_schema
echo "Generating config to /tmp/gateway-ha-config.yml"

# TODO: This is a hack to get the config to work. this is to avoid db config hardcoded in the config file.
envsubst < /var/lib/presto-gateway/config/gateway-ha-config.yml.template > /tmp/gateway-ha-config.yml
cat /tmp/gateway-ha-config.yml
java -jar /var/lib/presto-gateway/gateway-ha.jar server /tmp/gateway-ha-config.yml

