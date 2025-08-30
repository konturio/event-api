#!/usr/bin/env bash
set -euo pipefail

MAVEN_OPTS="${MAVEN_OPTS:-}"

# Generate Maven settings.xml based on HTTP(S)_PROXY variables
SETTINGS_DIR="$(dirname "$0")/../.mvn"
SETTINGS="$SETTINGS_DIR/settings.xml"
mkdir -p "$SETTINGS_DIR"

cat > "$SETTINGS" <<'XML'
<settings>
  <proxies>
XML

set_proxy() {
  local proxy=$1
  local protocol=$2
  if [[ -n "$proxy" ]]; then
    proxy=${proxy#http://}
    proxy=${proxy#https://}
    local host=${proxy%%:*}
    local port=${proxy##*:}
    local non_proxy=${NO_PROXY:-localhost|127.0.0.1|::1}
    cat >> "$SETTINGS" <<XML
    <proxy>
      <id>$protocol</id>
      <active>true</active>
      <protocol>$protocol</protocol>
      <host>$host</host>
      <port>$port</port>
      <nonProxyHosts>$non_proxy</nonProxyHosts>
    </proxy>
XML
    MAVEN_OPTS="-D${protocol}.proxyHost=$host -D${protocol}.proxyPort=$port -D${protocol}.nonProxyHosts=$non_proxy $MAVEN_OPTS"
  fi
}

set_proxy "${HTTPS_PROXY:-${https_proxy:-}}" https
set_proxy "${HTTP_PROXY:-${http_proxy:-}}" http

cat >> "$SETTINGS" <<'XML'
  </proxies>
</settings>
XML

export MAVEN_OPTS
exec mvn -s "$SETTINGS" "$@"
