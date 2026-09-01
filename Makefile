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

.PHONY: app
app:
	./gradlew :javasample:assembleApp1Release

.PHONY: app2
app2:
	./gradlew :javasample:assembleApp2Release

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

.PHONY: build-aab-app2
build-aab-app2:
	bundle exec fastlane build_aab_app2 \
		VERSION_CODE:$(shell date +%s) \
		STORE_FILE_APP2:$(STORE_FILE_APP2) \
		STORE_PASSWORD_APP2:$(STORE_PASSWORD_APP2) \
		KEY_ALIAS_APP2:$(KEY_ALIAS_APP2) \
		KEY_PASSWORD_APP2:$(KEY_PASSWORD_APP2)

.PHONY:	upload-aab-app2
upload-aab-app2:
	bundle exec fastlane upload_aab_app2 \
		json_key:$(GOOGLE_SERVICE_ACCOUNT_KEY_JSON_FILE)
