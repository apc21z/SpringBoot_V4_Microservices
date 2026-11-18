#!/usr/bin/env bash
set -Eeuo pipefail

if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
    printf 'Error: Docker no está instalado o el daemon no está disponible.\n' >&2
    exit 1
fi

if docker container inspect rabbitmq >/dev/null 2>&1; then
    existing_image="$(docker inspect --format '{{.Config.Image}}' rabbitmq)"
    if [[ "${existing_image}" != "rabbitmq:4-management" ]]; then
        printf 'Error: el contenedor rabbitmq ya existe con la imagen %s.\n' "${existing_image}" >&2
        exit 1
    fi

    if [[ "$(docker inspect --format '{{.State.Running}}' rabbitmq)" != "true" ]]; then
        docker start rabbitmq >/dev/null
    fi
    printf 'Contenedor RabbitMQ existente reutilizado; su configuración no se ha modificado.\n'
    exit 0
fi

docker run --detach \
    --name rabbitmq \
    --restart unless-stopped \
    --publish 127.0.0.1:5672:5672 \
    --publish 127.0.0.1:15672:15672 \
    --volume springboot-v4-rabbitmq-data:/var/lib/rabbitmq \
    --env RABBITMQ_DEFAULT_USER=admin \
    --env RABBITMQ_DEFAULT_PASS=password \
    rabbitmq:4-management
