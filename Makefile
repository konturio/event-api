SHELL := /bin/bash

MAVEN_ARGS := -DskipITs=true

define run_mvn
if [ -n "$$HTTP_PROXY$$HTTPS_PROXY" ]; then \
        proxy=$${HTTPS_PROXY:-$$HTTP_PROXY}; \
        proxy=$${proxy#http://}; \
        proxy=$${proxy#https://}; \
        host=$${proxy%%:*}; \
        port=$${proxy##*:}; \
        settings=$$(mktemp); \
        printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">' \ \
            '  <proxies>' \ \
            '    <proxy>' \ \
            '      <active>true</active>' \ \
            '      <protocol>http</protocol>' \ \
            "      <host>$$host</host>" \ \
            "      <port>$$port</port>" \ \
            '    </proxy>' \ \
            '  </proxies>' \ \
            '</settings>' > $$settings; \
        MAVEN_OPTS="-Dhttp.proxyHost=$$host -Dhttp.proxyPort=$$port -Dhttps.proxyHost=$$host -Dhttps.proxyPort=$$port $$MAVEN_OPTS" mvn --settings $$settings $(MAVEN_ARGS) $(1); \
        rm $$settings; \
    else \
        mvn $(MAVEN_ARGS) $(1); \
    fi
endef

.PHONY: test verify precommit check-docs

precommit: test check-docs ## Run checks before committing

check-docs: ## Perform basic documentation checks
	@find docs -name '*.md' -print >/dev/null

verify: ## Run full Maven verification
	@$(call run_mvn,verify)

test: ## Run unit tests
	@$(call run_mvn,test)
