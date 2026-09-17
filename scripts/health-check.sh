#!/usr/bin/env bash
# 배포 후 헬스체크. 사용법: ./scripts/health-check.sh <develop|main>
# 대상 URL 은 Jenkins Credentials/파라미터로 HEALTH_CHECK_URL_DEV, HEALTH_CHECK_URL_PROD 환경변수에 주입한다.
set -euo pipefail

BRANCH="${1:?브랜치(develop|main)를 지정하세요}"

case "$BRANCH" in
  develop) BASE_URL="${HEALTH_CHECK_URL_DEV:?HEALTH_CHECK_URL_DEV 가 필요합니다}" ;;
  main)    BASE_URL="${HEALTH_CHECK_URL_PROD:?HEALTH_CHECK_URL_PROD 가 필요합니다}" ;;
  *) echo "헬스체크 대상이 아닌 브랜치: $BRANCH" >&2; exit 1 ;;
esac

RETRIES="${HEALTH_CHECK_RETRIES:-20}"
INTERVAL="${HEALTH_CHECK_INTERVAL_SECONDS:-6}"

for ((i = 1; i <= RETRIES; i++)); do
  if curl -fsS "${BASE_URL}/actuator/health" | grep -q '"status":"UP"'; then
    echo "[health-check] UP (${i}/${RETRIES})"
    exit 0
  fi
  echo "[health-check] 대기 중... (${i}/${RETRIES})"
  sleep "$INTERVAL"
done

echo "[health-check] 실패: ${BASE_URL}/actuator/health" >&2
exit 1
