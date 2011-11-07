To run the tests you can use either a remote or managed JBoss AS 7 or embedded Jetty instance

To use a managed JBoss AS instance set the JBOSS_HOME environment variable and run

    mvn clean verify -Darquillian=jbossas-managed-7

To use a remote JBoss AS instance start it and then run 

    mvn clean verify -Darquillian=jbossas-remote-7

To run the embedded Jetty tests with Hibernate run

    mvn clean verify -Darquillian=jetty-embedded-7-hibernate

To run the embedded Jetty tests with OpenJPA run

    mvn clean verify -Darquillian=jetty-embedded-7-openjpa

