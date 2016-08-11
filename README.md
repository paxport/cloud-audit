Cloud Audit
===================

Infrastructure for logging request/response pairs and other debugging/auditing info.

First off logging into Google BigQuery.

Uses Spring.

## To Release new version to Bintray

    mvn clean release:prepare -Darguments="-Dmaven.javadoc.skip=true"
    mvn release:perform -Darguments="-Dmaven.javadoc.skip=true"


