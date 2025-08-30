					SHELL := /bin/bash

MAVEN_PROXY_ARGS := $(shell \
        proxy="$$HTTPS_PROXY"; \
        if [ -n "$$proxy" ]; then \
                proxy=$${proxy#http://}; \
                proxy=$${proxy#https://}; \
                host=$${proxy%%:*}; \
                port=$${proxy##*:}; \
                printf '%q %q ' "-Dhttps.proxyHost=$$host" "-Dhttps.proxyPort=$$port"; \
        fi; \
        proxy="$$HTTP_PROXY"; \
        if [ -n "$$proxy" ]; then \
                proxy=$${proxy#http://}; \
                proxy=$${proxy#https://}; \
                host=$${proxy%%:*}; \
                port=$${proxy##*:}; \
                printf '%q %q ' "-Dhttp.proxyHost=$$host" "-Dhttp.proxyPort=$$port"; \
        fi)

MAVEN_ARGS := -DskipITs=true $(MAVEN_PROXY_ARGS)

.PHONY: test verify precommit check-docs

precommit: test check-docs ## Run checks before committing

check-docs: ## Perform basic documentation checks
	@find docs -name '*.md' -print >/dev/null

verify: ## Run full Maven verification
	@MAVEN_OPTS="$(MAVEN_PROXY_ARGS) $$MAVEN_OPTS" mvn $(MAVEN_ARGS) verify

test: ## Run unit tests
	@MAVEN_OPTS="$(MAVEN_PROXY_ARGS) $$MAVEN_OPTS" mvn $(MAVEN_ARGS) test
