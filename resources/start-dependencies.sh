#!/usr/bin/env bash
set -Eeuo pipefail

readonly script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly mongo_init_script="${script_dir}/docker/mongo-init.js"

if ! command -v docker >/dev/null 2>&1; then
    printf 'Error: Docker no está instalado o no está disponible en PATH.\n' >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    printf 'Error: no se puede conectar con el daemon de Docker. Inicia Docker y vuelve a intentarlo.\n' >&2
    exit 1
fi

if [[ ! -f "${mongo_init_script}" ]]; then
    printf 'Error: no se encontró el inicializador de MongoDB: %s\n' "${mongo_init_script}" >&2
    exit 1
fi

ensure_container() {
    local name="$1"
    local image="$2"
    shift 2

    if docker container inspect "${name}" >/dev/null 2>&1; then
        local existing_image
        local running
        existing_image="$(docker inspect --format '{{.Config.Image}}' "${name}")"
        if [[ "${existing_image}" != "${image}" ]]; then
            printf 'Error: el contenedor %s ya existe con la imagen %s (se esperaba %s).\n' \
                "${name}" "${existing_image}" "${image}" >&2
            return 1
        fi

        running="$(docker inspect --format '{{.State.Running}}' "${name}")"
        if [[ "${running}" != "true" ]]; then
            docker start "${name}" >/dev/null
        fi
        printf 'Contenedor %s existente reutilizado; Docker no actualiza su configuración ni sus volúmenes.\n' \
            "${name}"
        return
    fi

    docker run --detach \
        --name "${name}" \
        --restart unless-stopped \
        "$@" \
        "${image}"
}

wait_for_container_command() {
    local name="$1"
    shift

    local attempt
    for ((attempt = 1; attempt <= 30; attempt++)); do
        if docker exec "${name}" "$@" >/dev/null 2>&1; then
            return 0
        fi
        sleep 2
    done

    printf 'Error: el contenedor %s no quedó listo a tiempo.\n' "${name}" >&2
    docker logs --tail 40 "${name}" >&2
    return 1
}

ensure_container springboot-v4-mongodb mongo:7.0.4 \
    --publish 127.0.0.1:27017:27017 \
    --volume springboot-v4-mongodb-data:/data/db \
    --volume "${mongo_init_script}:/docker-entrypoint-initdb.d/10-product-db.js:ro" \
    --env MONGO_INITDB_ROOT_USERNAME=root \
    --env MONGO_INITDB_ROOT_PASSWORD=password \
    --env MONGO_INITDB_DATABASE=product-service

ensure_container springboot-v4-mysql mysql:8.4 \
    --publish 127.0.0.1:3306:3306 \
    --volume springboot-v4-mysql-data:/var/lib/mysql \
    --env MYSQL_ROOT_PASSWORD=password \
    --env MYSQL_ROOT_HOST=% \
    --env MYSQL_DATABASE=inventory_service

ensure_container springboot-v4-postgres postgres:17-alpine \
    --publish 127.0.0.1:5432:5432 \
    --volume springboot-v4-postgres-data:/var/lib/postgresql/data \
    --env POSTGRES_USER=postgres \
    --env POSTGRES_PASSWORD=password \
    --env POSTGRES_DB=order_service

ensure_container rabbitmq rabbitmq:4-management \
    --publish 127.0.0.1:5672:5672 \
    --publish 127.0.0.1:15672:15672 \
    --volume springboot-v4-rabbitmq-data:/var/lib/rabbitmq \
    --env RABBITMQ_DEFAULT_USER=admin \
    --env RABBITMQ_DEFAULT_PASS=password

wait_for_container_command springboot-v4-mongodb \
    mongosh --quiet --username root --password password --authenticationDatabase admin \
    --eval 'quit(db.adminCommand("ping").ok ? 0 : 1)'
docker exec springboot-v4-mongodb mongosh --quiet \
    --username root --password password --authenticationDatabase admin \
    --eval 'const productDb = db.getSiblingDB("product-service"); if (!productDb.getCollectionNames().includes("product")) productDb.createCollection("product");'

wait_for_container_command springboot-v4-mysql \
    mysqladmin ping --host=127.0.0.1 --user=root --password=password --silent
docker exec springboot-v4-mysql mysql --user=root --password=password \
    --execute='CREATE DATABASE IF NOT EXISTS inventory_service'

wait_for_container_command springboot-v4-postgres \
    pg_isready --username=postgres --dbname=postgres
postgres_database_exists="$(docker exec springboot-v4-postgres psql \
    --username=postgres --dbname=postgres --tuples-only --no-align \
    --command="SELECT 1 FROM pg_database WHERE datname = 'order_service'")"
if [[ "${postgres_database_exists}" != "1" ]]; then
    docker exec springboot-v4-postgres createdb --username=postgres order_service
fi

wait_for_container_command rabbitmq rabbitmq-diagnostics --quiet ping
docker exec rabbitmq rabbitmqctl authenticate_user admin password >/dev/null

printf '\nDependencias Docker iniciadas.\n'
printf 'MongoDB:   localhost:27017 (base product-service)\n'
printf 'MySQL:     localhost:3306 (base inventory_service)\n'
printf 'PostgreSQL: localhost:5432 (base order_service)\n'
printf 'RabbitMQ:  localhost:5672 (panel: http://localhost:15672)\n'
