#!/usr/bin/env bash
# Copia de seguridad de PostgreSQL. Se guarda comprimida y se borran las de más de 14 días.
# Programarla todos los días a las 3 a. m. con cron:
#   0 3 * * * /opt/mealupp/ops/backup.sh >> /var/log/mealupp-backup.log 2>&1
set -euo pipefail
cd "$(dirname "$0")/.."

BACKUP_DIR="${BACKUP_DIR:-./backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"
mkdir -p "$BACKUP_DIR"

set -a
# shellcheck disable=SC1091
source .env
set +a

FILE="$BACKUP_DIR/mealup-$(date +%Y%m%d-%H%M%S).sql.gz"
docker compose exec -T db pg_dump -U "$DB_USERNAME" mealup | gzip > "$FILE"
find "$BACKUP_DIR" -name 'mealup-*.sql.gz' -mtime +"$KEEP_DAYS" -delete
echo "Copia creada: $FILE"

# Para guardar también una copia fuera del servidor (recomendado), sube el archivo a tu
# almacenamiento de objetos, por ejemplo con rclone o aws-cli:
#   rclone copy "$FILE" mi-bucket:copias-mealupp/
