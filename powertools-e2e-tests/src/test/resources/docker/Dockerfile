# Use the official AWS SAM base image for Java 25
FROM public.ecr.aws/sam/build-java25@sha256:badfeb1beb0ae397f9b170a5139c49bff9bd0912c8debdb8c4c17a53e70b6195

# Install GraalVM dependencies
RUN curl -4 -L https://download.oracle.com/graalvm/25/latest/graalvm-jdk-25_linux-x64_bin.tar.gz | tar -xvz
RUN mv graalvm-jdk-25.* /usr/lib/graalvm

# Make native image and mvn available on CLI
RUN ln -s /usr/lib/graalvm/bin/native-image /usr/bin/native-image
RUN ln -s /usr/lib/maven/bin/mvn /usr/bin/mvn

# Set GraalVM as default
ENV JAVA_HOME=/usr/lib/graalvm
ENV PATH=/usr/lib/graalvm/bin:$PATH
