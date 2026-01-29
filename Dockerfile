FROM ubuntu:22.04

# 1. Install Dependencies (OpenJDK 17, wget, unzip, git)
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# 2. Set Environment Variables
ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
ENV ANDROID_HOME="/sdk"
ENV PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

# 3. Download Android Command Line Tools
# (Version 11076708 - latest as of late 2024/early 2025 roughly, ensuring relatively new tools)
RUN mkdir -p $ANDROID_HOME/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp/ \
    && mv /tmp/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

# 4. Accept Licenses and Install SDK Components
# We install "platforms;android-36" explicitly as required by the project
RUN yes | sdkmanager --licenses \
    && sdkmanager "platform-tools" "platforms;android-36" "build-tools;35.0.0"

# 5. Copy Project Files
WORKDIR /app
COPY . .

# 5.5 Sanitize local.properties (Remove Windows-specific sdk.dir but keep secrets)
RUN if [ -f local.properties ]; then sed -i '/sdk.dir/d' local.properties; fi

# 6. Fix Line Endings and Permissions
# Install dos2unix to fix Windows line endings in gradlew
RUN apt-get update && apt-get install -y dos2unix && rm -rf /var/lib/apt/lists/*
RUN dos2unix ./gradlew
RUN chmod +x ./gradlew

# 7. Run Build
CMD ["./gradlew", "assembleDebug"]
