#!/bin/sh

#rm -rf build/dokka
#./gradlew dokkaHtmlMultiModule

cd build/dokka/htmlMultiModule || exit 1
mv -- -modules.html index.html

git init
git checkout -b gh-pages
git add .
git commit -m "Deploy documentation"
git remote add authgear https://github.com/authgear/authgear-sdk-android
git push authgear gh-pages --force

