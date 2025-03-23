.PHONY: docs
docs:
	rm -rf ./build/dokka
	./gradlew dokkaHtmlMultiModule

.PHONY: deploy-docs
deploy-docs: docs
	./scripts/deploy-docs.sh

.PHONY: sdk
sdk:
	./gradlew :sdk:assembleRelease

.PHONY:	sdk-okhttp
sdk-okhttp:
	./gradlew :sdk-okhttp:assembleRelease

.PHONY: build-aab
build-aab:
	bundle exec fastlane build_aab \
		VERSION_CODE:$(shell date +%s) \
		STORE_FILE:$(STORE_FILE) \
		STORE_PASSWORD:$(STORE_PASSWORD) \
		KEY_ALIAS:$(KEY_ALIAS) \
		KEY_PASSWORD:$(KEY_PASSWORD)

.PHONY:	upload-aab
upload-aab:
	bundle exec fastlane upload_aab \
		json_key:$(GOOGLE_SERVICE_ACCOUNT_KEY_JSON_FILE)
