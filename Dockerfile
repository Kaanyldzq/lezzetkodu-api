# ---------- 1. asama: derleme ----------
# Maven ve JDK iceren imajda projeyi derleyip .jar uretiyoruz.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Once sadece pom.xml'i kopyalayip bagimliliklari indiriyoruz.
# Kod degisse bile pom.xml degismediyse Docker bu katmani onbellekten kullanir -> sonraki build'ler cok hizli.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- 2. asama: calistirma ----------
# Sadece JRE iceren kucuk imaj. Maven, kaynak kod ve derleme araclari son imaja girmez.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Uygulamayi root olmayan bir kullaniciyla calistir (guvenlik).
RUN useradd --system --uid 1001 appuser
COPY --from=build /app/target/lezzetkodu-api-*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
