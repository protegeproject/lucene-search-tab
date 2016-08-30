#!/bin/bash
PROTEGE=/Users/rgoncalves/Documents/workspace-intellij/protege/protege-desktop/target/protege-5.0.1-SNAPSHOT-platform-independent/Protege-5.0.1-SNAPSHOT
cd /Users/rgoncalves/Documents/workspace-intellij/lucene-search-tab
mvn clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Dsource.skip=true
PLUGINS_DIR=${PROTEGE}/plugins
if [ ! -d "$PLUGINS_DIR" ]; then
  mkdir "$PLUGINS_DIR"
fi
cp target/*.jar ${PROTEGE}/plugins
sh ${PROTEGE}/run.command