#Use the official AWS SAM base image for Java 25
FROM public.ecr.aws/sam/build-java25@sha256:35c398d9eaadd173262573be05ba7e1c3eda552a5bd320659b7b1442632b47df

#Install GraalVM dependencies
RUN curl -4 -L curl https://download.oracle.com/graalvm/25/latest/graalvm-jdk-25_linux-x64_bin.tar.gz | tar -xvz
RUN mv graalvm-jdk-25.* /usr/lib/graalvm

#Make native image and mvn available on CLI
RUN ln -s /usr/lib/graalvm/bin/native-image /usr/bin/native-image
RUN ln -s /usr/lib/maven/bin/mvn /usr/bin/mvn

#Set GraalVM as default
ENV JAVA_HOME=/usr/lib/graalvm
ENV PATH=/usr/lib/graalvm/bin:$PATH
