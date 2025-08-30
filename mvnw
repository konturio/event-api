#!/usr/bin/env bash
set -euo pipefail

# Configure proxy settings based on standard environment variables and expose
# them both via a generated Maven settings file and JVM system properties.
MAVEN_OPTS="${MAVEN_OPTS:-}"
set_proxy() {
  local proxy=$1
  local prefix=$2
  if [ -n "$proxy" ]; then
    proxy=${proxy#http://}
    proxy=${proxy#https://}
    local host=${proxy%%:*}
    local port=${proxy##*:}
    local non_proxy=${NO_PROXY:-localhost|127.0.0.1|::1}
    echo "    <proxy>" >> "$SETTINGS"
    echo "      <id>${prefix,,}</id>" >> "$SETTINGS"
    echo "      <active>true</active>" >> "$SETTINGS"
    echo "      <protocol>${prefix,,}</protocol>" >> "$SETTINGS"
    echo "      <host>$host</host>" >> "$SETTINGS"
    echo "      <port>$port</port>" >> "$SETTINGS"
    echo "      <nonProxyHosts>$non_proxy</nonProxyHosts>" >> "$SETTINGS"
    echo "    </proxy>" >> "$SETTINGS"
    MAVEN_OPTS="-D${prefix,,}.proxyHost=$host -D${prefix,,}.proxyPort=$port -D${prefix,,}.nonProxyHosts=$non_proxy $MAVEN_OPTS"
  fi
}
SETTINGS="$(dirname "$0")/.mvn/settings.xml"
mkdir -p "$(dirname "$SETTINGS")"
cat > "$SETTINGS" <<'XML'
<settings>
  <proxies>
XML
set_proxy "${HTTPS_PROXY:-${https_proxy:-}}" https
set_proxy "${HTTP_PROXY:-${http_proxy:-}}" http
cat >> "$SETTINGS" <<'XML'
  </proxies>
</settings>
XML
export MAVEN_OPTS
exec mvn -s "$SETTINGS" "$@"
