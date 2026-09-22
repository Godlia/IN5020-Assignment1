# ---- stage 1: compile with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
# --mount=type=cache keeps Maven's download folder (~/.m2) between builds, so the jars are
# downloaded once and not again after every code change. The cache is not part of the image.
# Use cache to speed up repeated builds
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package
# ---- stage 2: runtime image ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /opt/app

# Copy the dataset and input queries required by the assignment
COPY exercise_1_dataset.csv ./
COPY exercise_1_input.txt ./

# Use a wildcard to copy the snapshot JAR compiled by Maven in stage 1
COPY --from=build /src/target/assignment1.jar ./src.jar

COPY docker/run.sh /usr/local/bin/run
COPY docker/listening.sh /usr/local/bin/listening
RUN chmod +x /usr/local/bin/run /usr/local/bin/listening

ENV APP_CLASSPATH="/opt/app/src.jar"
ENTRYPOINT ["run"]
# no CMD: compose gives the main class per service