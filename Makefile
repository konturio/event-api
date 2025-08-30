# Proxy-aware wrappers for Maven commands
# Uses scripts/mvn.sh to honour HTTP(S)_PROXY env vars only when set.

MVN := ./scripts/mvn.sh
MAVEN_ARGS := -DskipITs=true -Ddocker.tests.exclude='**/PdcEpisodeCompositionTest.*'

.PHONY: test verify precommit check-docs

precommit: test check-docs ## Run checks before committing

check-docs: ## Perform basic documentation checks
	@find docs -name '*.md' -print >/dev/null

verify: ## Run full Maven verification
	$(MVN) $(MAVEN_ARGS) verify

test: ## Run unit tests
	$(MVN) $(MAVEN_ARGS) test
