# Base image
FROM python:3.11

WORKDIR /home/skraper

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    mlocate \
    ffmpeg \
    software-properties-common \
    openjdk-17-jdk \
    maven \
    chromium \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy application files
COPY . .

# Build the application
RUN ./mvnw clean package -DskipTests=true \
    && mkdir -p /usr/local/skraper \
    && cp /home/skraper/cli/target/cli.jar /usr/local/skraper/

# Create executable script
RUN echo '#!/bin/bash\njava -jar /usr/local/skraper/cli.jar "$@"' > /usr/local/bin/skraper \
    && chmod +x /usr/local/bin/skraper

# Set the entry point (optional)
# ENTRYPOINT ["skraper"]
# CMD ["--help"]