# Use the official AWS SAM base image for Java 25
FROM public.ecr.aws/sam/build-java25@sha256:946721502784887fcb0aa75b252f2aa88f170815cbd2e7a7afa8d107a1f1ec61

# Install GraalVM dependencies
RUN curl -4 -L https://download.oracle.com/graalvm/25/latest/graalvm-jdk-25_linux-x64_bin.tar.gz | tar -xvz
RUN mv graalvm-jdk-25.* /usr/lib/graalvm

# Make native image and mvn available on CLI
RUN ln -s /usr/lib/graalvm/bin/native-image /usr/bin/native-image
RUN ln -s /usr/lib/maven/bin/mvn /usr/bin/mvn

# Set GraalVM as default
ENV JAVA_HOME=/usr/lib/graalvm
ENV PATH=/usr/lib/graalvm/bin:$PATH
