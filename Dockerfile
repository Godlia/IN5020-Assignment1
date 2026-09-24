# ---- stage 1: compile with Maven ----
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /in5020
COPY target ./target
COPY exercise_1_dataset.csv ./
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]