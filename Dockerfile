FROM google/cloud-sdk
#RUN export CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)" && \
#
#echo "deb http://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && \
#curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add - && \
#sudo apt-get update && sudo apt-get install google-cloud-sdk && \
#gcloud init

#ARG JAR_FILE
#COPY ${JAR_FILE} app.jar
#ENTRYPOINT ["java","-jar","app.jar"]


RUN set -ex && \
    echo 'deb http://deb.debian.org/debian jessie-backports main' \
      > /etc/apt/sources.list.d/jessie-backports.list && \
    apt update -y && \
    apt install -t \
     jessie-backports \
      openjdk-8-jre-headless \
      ca-certificates-java -y

#alpine
RUN apk --update add openjdk8-jre
#RUN gcloud components install  kubectl


RUN apt-get install -y ca-certificates software-properties-common
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -

RUN apt-key fingerprint 0EBFCD88

RUN add-apt-repository \
       "deb [arch=amd64] https://download.docker.com/linux/debian \
       $(lsb_release -cs) \
       stable"
RUN apt-get update
RUN apt-get install -y docker-ce



RUN curl -sSL http://bit.ly/2ysbOFE  -o bootstrap.sh

RUN chmod u+x bootstrap.sh

RUN bash bootstrap.sh 1.2.0  -s -d