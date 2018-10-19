FROM gcr.io/google-appengine/openjdk:8

ARG CLOUD_SDK_VERSION=219.0.1
ENV CLOUD_SDK_VERSION=$CLOUD_SDK_VERSION

COPY libs /app/libs/
COPY resources /app/resources/
COPY classes /app/classes/

#RUN apt-get update
#RUN apt-get install -y curl
##RUN apt-get install -y git
#
## Set the Kubernetes version as found in the UCP Dashboard or API
#ENV k8sversion v1.8.11
#
## Get the kubectl binary.
#RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/$k8sversion/bin/darwin/amd64/kubectl
#
## Make the kubectl binary executable.
#RUN chmod +x ./kubectl
#
## Move the kubectl executable to /usr/local/bin.
#RUN  mv ./kubectl /bin/kubectl
RUN apt-get -qqy update && apt-get install -qqy \
        curl \
        gcc \
        python-dev \
        python-setuptools \
        apt-transport-https \
        lsb-release \
        openssh-client \
        git \
        gnupg \
        software-properties-common


RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -

RUN apt-key fingerprint 0EBFCD88

RUN add-apt-repository \
       "deb [arch=amd64] https://download.docker.com/linux/debian \
       $(lsb_release -cs) \
       stable"
RUN apt-get update
RUN apt-get install -y docker-ce
#
#RUN groupadd docker
#RUN usermod -aG docker $USER

RUN  easy_install -U pip && \
    pip install -U crcmod   && \
    export CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)" && \
    echo "deb https://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" > /etc/apt/sources.list.d/google-cloud-sdk.list && \
    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add - && \
    apt-get install -y google-cloud-sdk=${CLOUD_SDK_VERSION}-0 \
    kubectl && \
    gcloud config set core/disable_usage_reporting true && \
    gcloud config set component_manager/disable_update_check true && \
    gcloud config set metrics/environment github_docker_image && \
    gcloud --version && \
    docker --version && kubectl version --client
VOLUME ["/root/.config"]



RUN curl -sSL http://bit.ly/2ysbOFE  -o bootstrap.sh

RUN chmod u+x bootstrap.sh

RUN bash bootstrap.sh 1.2.0  -s -d

#RUN  cp -r /root/fabric-samples/bin/ /root/bin
RUN curl -sSL https://sdk.cloud.google.com | bash


EXPOSE 9000
ENTRYPOINT ["java","-Xms512m","-Xdebug","-cp","/app/resources:/app/classes:/app/libs/*","org.cg.GbaasApplication"]