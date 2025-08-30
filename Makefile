SHELL := /bin/bash

MAVEN_ARGS :=
MAVEN_JAVA_OPTS := -Djava.net.useSystemProxies=true -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false

define run_mvn
settings=$$(mktemp); \
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">' > $$settings; \
if [ -n "$$HTTP_PROXY$$HTTPS_PROXY" ]; then \
        proxy=$${HTTPS_PROXY:-$$HTTP_PROXY}; \
        proxy=$${proxy#http://}; \
        proxy=$${proxy#https://}; \
        host=$${proxy%%:*}; \
        port=$${proxy##*:}; \
        printf '%s\n' '  <proxies>' \
            '    <proxy>' \
            '      <active>true</active>' \
            '      <protocol>http</protocol>' \
            "      <host>$$host</host>" \
            "      <port>$$port</port>" \
            '    </proxy>' \
            '  </proxies>' >> $$settings; \
        MAVEN_OPTS="$(MAVEN_JAVA_OPTS) -Dhttp.proxyHost=$$host -Dhttp.proxyPort=$$port -Dhttps.proxyHost=$$host -Dhttps.proxyPort=$$port $$MAVEN_OPTS"; \
    else \
        MAVEN_OPTS="$(MAVEN_JAVA_OPTS) $$MAVEN_OPTS"; \
    fi; \
 printf '%s\n' '</settings>' >> $$settings; \
 MAVEN_OPTS="$$MAVEN_OPTS" mvn --settings $$settings -Dmaven.repo.local=$(CURDIR)/.m2 $(MAVEN_ARGS) $(1); \
 rm $$settings
endef

.PHONY: all clean test verify precommit check-docs

all: test ## Default target runs unit tests

clean: ## Remove build artifacts
	rm -rf target .m2

precommit: test check-docs ## Run checks before committing

check-docs: ## Perform basic documentation checks
	@if ! find docs -name '*.md' -print | grep -q .; then \
		echo "No documentation files found in docs/" >&2; \
		exit 1; \
	fi

verify: ## Run full Maven verification
	@$(call run_mvn,verify)

test: ## Run unit tests
	@$(call run_mvn,test -DskipITs=true)

