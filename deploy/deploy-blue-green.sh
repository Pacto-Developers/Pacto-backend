#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR=/opt/pacto
COMPOSE_FILE=$APP_DIR/docker-compose.deploy.yml
STATE_FILE=$APP_DIR/current-color
UPSTREAM_FILE=/etc/nginx/conf.d/pacto-upstream.conf
AWS_REGION=ap-southeast-2
ECR_REGISTRY=829279763575.dkr.ecr.ap-southeast-2.amazonaws.com
DOMAIN=pacto-api.duckdns.org

exec 9>/var/lock/pacto-deploy.lock
flock -n 9 || {
  echo "Another deployment is already running."
  exit 1
}

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <ECR_IMAGE_URI>"
  exit 1
fi

IMAGE_URI=$1

case "$IMAGE_URI" in
  "$ECR_REGISTRY/pacto-api:"*) ;;
  *)
    echo "Invalid image URI: $IMAGE_URI"
    exit 1
    ;;
esac

CURRENT_COLOR=$(cat "$STATE_FILE")

case "$CURRENT_COLOR" in
  blue)
    CURRENT_PORT=8081
    NEXT_COLOR=green
    NEXT_PORT=8082
    ;;
  green)
    CURRENT_PORT=8082
    NEXT_COLOR=blue
    NEXT_PORT=8081
    ;;
  *)
    echo "Invalid current color: $CURRENT_COLOR"
    exit 1
    ;;
esac

compose() {
  local color=$1
  local port=$2
  shift 2

  IMAGE_URI="$IMAGE_URI" \
  APP_COLOR="$color" \
  APP_PORT="$port" \
    docker-compose \
      -p "pacto-$color" \
      -f "$COMPOSE_FILE" \
      "$@"
}

echo "Deploying $IMAGE_URI to $NEXT_COLOR:$NEXT_PORT"

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

docker pull "$IMAGE_URI"

compose "$NEXT_COLOR" "$NEXT_PORT" down --remove-orphans || true
compose "$NEXT_COLOR" "$NEXT_PORT" up -d --force-recreate

HEALTHY=false

for attempt in {1..24}; do
  RESPONSE=$(curl -fsS --max-time 3 \
    "http://127.0.0.1:$NEXT_PORT/actuator/health" || true)

  if [[ "$RESPONSE" == *'"status":"UP"'* ]]; then
    HEALTHY=true
    break
  fi

  echo "Health check $attempt/24 failed. Retrying..."
  sleep 5
done

if [[ "$HEALTHY" != true ]]; then
  docker logs --tail 100 "pacto-api-$NEXT_COLOR" || true
  compose "$NEXT_COLOR" "$NEXT_PORT" down
  echo "Deployment failed. Traffic was not switched."
  exit 1
fi

cp "$UPSTREAM_FILE" "$UPSTREAM_FILE.backup"

cat > "$UPSTREAM_FILE" <<EOF
upstream pacto_backend {
    server 127.0.0.1:$NEXT_PORT;
    keepalive 32;
}
EOF

if ! nginx -t; then
  mv "$UPSTREAM_FILE.backup" "$UPSTREAM_FILE"
  compose "$NEXT_COLOR" "$NEXT_PORT" down
  echo "Nginx validation failed."
  exit 1
fi

systemctl reload nginx

if ! curl -fsS --max-time 5 \
  --resolve "$DOMAIN:443:127.0.0.1" \
  "https://$DOMAIN/actuator/health" | grep -q '"status":"UP"'; then
  mv "$UPSTREAM_FILE.backup" "$UPSTREAM_FILE"
  nginx -t
  systemctl reload nginx
  compose "$NEXT_COLOR" "$NEXT_PORT" down
  echo "Proxy health check failed. Rolled back."
  exit 1
fi

rm -f "$UPSTREAM_FILE.backup"
echo "$NEXT_COLOR" > "$STATE_FILE"

sleep 30
compose "$CURRENT_COLOR" "$CURRENT_PORT" down || true

echo "Deployment completed: $CURRENT_COLOR -> $NEXT_COLOR"
