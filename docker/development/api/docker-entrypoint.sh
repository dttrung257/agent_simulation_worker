#!/bin/sh
#gradle build
#java -jar build/libs/agent_simulation_worker-0.0.1-SNAPSHOT.jar

java -XX:MaxRAMPercentage=80.0 -XX:+UseZGC -jar /app/api.jar
