# Use the official AWS SAM base image for Java 25
FROM public.ecr.aws/sam/build-java25@sha256:de6f2085626c1b391a339ec871336b59bca0c909466b8ccdce61df8d3f1866c3

# Install GraalVM dependencies
RUN curl -4 -L https://download.oracle.com/graalvm/25/latest/graalvm-jdk-25_linux-x64_bin.tar.gz | tar -xvz
RUN mv graalvm-jdk-25.* /usr/lib/graalvm

# Make native image and mvn available on CLI
RUN ln -s /usr/lib/graalvm/bin/native-image /usr/bin/native-image
RUN ln -s /usr/lib/maven/bin/mvn /usr/bin/mvn

# Set GraalVM as default
ENV JAVA_HOME=/usr/lib/graalvm
ENV PATH=/usr/lib/graalvm/bin:$PATH
