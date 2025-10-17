# Use the official AWS SAM base image for Java 21
FROM public.ecr.aws/sam/build-java21@sha256:e12c501aa7d3d3dc04be0d900115a0339c73f523ad73a9aecd9e99499413c465

# Install GraalVM dependencies
RUN curl -4 -L https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_linux-x64_bin.tar.gz | tar -xvz
RUN mv graalvm-jdk-21.* /usr/lib/graalvm

# Make native image and mvn available on CLI
RUN ln -s /usr/lib/graalvm/bin/native-image /usr/bin/native-image
RUN ln -s /usr/lib/maven/bin/mvn /usr/bin/mvn

# Set GraalVM as default
ENV JAVA_HOME=/usr/lib/graalvm
ENV PATH=/usr/lib/graalvm/bin:$PATH
