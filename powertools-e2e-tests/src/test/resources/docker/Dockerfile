# Use the official AWS SAM base image for Java 25
FROM public.ecr.aws/sam/build-java25@sha256:2c8e723a001e71aec45872dfe645aa3c7b681ea5214c3ff0970aa465b7d86c8e

# Install GraalVM dependencies
RUN curl -4 -L https://download.oracle.com/graalvm/25/latest/graalvm-jdk-25_linux-x64_bin.tar.gz | tar -xvz
RUN mv graalvm-jdk-25.* /usr/lib/graalvm

# Make native image and mvn available on CLI
RUN ln -s /usr/lib/graalvm/bin/native-image /usr/bin/native-image
RUN ln -s /usr/lib/maven/bin/mvn /usr/bin/mvn

# Set GraalVM as default
ENV JAVA_HOME=/usr/lib/graalvm
ENV PATH=/usr/lib/graalvm/bin:$PATH
