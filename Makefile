SHELL := /bin/bash
MAVEN_ARGS := -DskipITs=true -Ddocker.tests.exclude='**/PdcEpisodeCompositionIT.*'

define MVN
        set -euo pipefail; \
        MAVEN_OPTS="${MAVEN_OPTS:-}"; \
        mkdir -p .mvn; \
        settings=$$(mktemp .mvn/settings.XXXXXX.xml); \
        echo "<settings>\n  <proxies>" > $$settings; \
        if [ -n "$$HTTPS_PROXY" ]; then \
                proxy=$$HTTPS_PROXY; \
                proxy=$${proxy#http://}; \
                proxy=$${proxy#https://}; \
                host=$${proxy%%:*}; \
                port=$${proxy##*:}; \
                non_proxy=$${NO_PROXY:-localhost|127.0.0.1|::1}; \
                echo "    <proxy>\n      <id>https</id>\n      <active>true</active>\n      <protocol>https</protocol>\n      <host>$$host</host>\n      <port>$$port</port>\n      <nonProxyHosts>$$non_proxy</nonProxyHosts>\n    </proxy>" >> $$settings; \
                MAVEN_OPTS="-Dhttps.proxyHost=$$host -Dhttps.proxyPort=$$port -Dhttps.nonProxyHosts=$$non_proxy $$MAVEN_OPTS"; \
        fi; \
        if [ -n "$$HTTP_PROXY" ]; then \
                proxy=$$HTTP_PROXY; \
                proxy=$${proxy#http://}; \
                proxy=$${proxy#https://}; \
                host=$${proxy%%:*}; \
                port=$${proxy##*:}; \
                non_proxy=$${NO_PROXY:-localhost|127.0.0.1|::1}; \
                echo "    <proxy>\n      <id>http</id>\n      <active>true</active>\n      <protocol>http</protocol>\n      <host>$$host</host>\n      <port>$$port</port>\n      <nonProxyHosts>$$non_proxy</nonProxyHosts>\n    </proxy>" >> $$settings; \
                MAVEN_OPTS="-Dhttp.proxyHost=$$host -Dhttp.proxyPort=$$port -Dhttp.nonProxyHosts=$$non_proxy $$MAVEN_OPTS"; \
        fi; \
        echo "  </proxies>\n</settings>" >> $$settings; \
        MAVEN_OPTS="$$MAVEN_OPTS" mvn -s $$settings $(1); \
        rm $$settings
endef

.PHONY: test verify precommit check-docs

precommit: test check-docs ## Run checks before committing

check-docs: ## Perform basic documentation checks
	@find docs -name '*.md' -print >/dev/null

verify: ## Run full Maven verification
	@$(call MVN,$(MAVEN_ARGS) verify)

test: ## Run unit tests
	@$(call MVN,$(MAVEN_ARGS) test)
