.PHONY: docs
docs:
	rm -rf ./build/dokka
	./gradlew dokkaHtmlMultiModule

.PHONY: deploy-docs
deploy-docs: docs
	./scripts/deploy-docs.sh
