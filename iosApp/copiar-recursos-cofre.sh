#!/bin/bash
set -euo pipefail

# O host usa o framework pré-compilado, mas ainda precisa dos recursos que o
# embedAndSignAppleFrameworkForXcode normalmente sincronizaria para o app.
recursos="$SRCROOT/../shared/build/ios-keychain-resources/compose-resources"
test -d "$recursos/composeResources/org.br.dietamais.recursos/font"
mkdir -p "$BUILT_PRODUCTS_DIR/$UNLOCALIZED_RESOURCES_FOLDER_PATH"
cp -R "$recursos" "$BUILT_PRODUCTS_DIR/$UNLOCALIZED_RESOURCES_FOLDER_PATH/"
