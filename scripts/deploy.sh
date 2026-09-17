#!/usr/bin/env bash
# 배포 스크립트 스텁. 사용법: ./scripts/deploy.sh <dev|prod> <image-tag>
# TODO: 배포 대상 AWS 서비스(EC2 + docker compose / ECS / Elastic Beanstalk) 확정 후 구현
set -euo pipefail

ENVIRONMENT="${1:?환경(dev|prod)을 지정하세요}"
IMAGE_TAG="${2:?이미지 태그를 지정하세요}"

case "$ENVIRONMENT" in
  dev|prod) ;;
  *) echo "알 수 없는 환경: $ENVIRONMENT" >&2; exit 1 ;;
esac

echo "[deploy] env=${ENVIRONMENT} image=${ECR_REPO:-<ECR_REPO>}:${IMAGE_TAG}"
echo "[deploy] TODO: 배포 대상 확정 전이라 실제 배포는 수행하지 않습니다." >&2
exit 1
