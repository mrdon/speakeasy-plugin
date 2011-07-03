mvn clean refapp:run -Dproduct=confluence -Djvmargs="-agentlib:jprofilerti=port=8849  -Xbootclasspath/a:/home/mrdon/local/jprofiler5/bin/agent.jar -XX:MaxPermSize=256m -Xmx512m"
