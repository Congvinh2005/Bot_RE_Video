# Backend: Java 21 + ffmpeg full (libass burn subtitle, freetype overlay) + DejaVu font.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -q dependency:go-offline || true
COPY src src
RUN mvn -q -DskipTests package && cp target/*.jar app.jar

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/* \
    && ffmpeg -hide_banner -h filter=subtitles > /dev/null \
    && ffmpeg -hide_banner -h filter=drawtext > /dev/null
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
