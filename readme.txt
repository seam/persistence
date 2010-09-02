To run the tests you can use either a remote or managed jboss instance. 

To use a managed instance set the JBOSS_HOME environment variable and run

mvn install -Pjbossas-managed-6

To use a remote jboss instance start jboss and then run 

mvn install -Pjbossas-remote-6
