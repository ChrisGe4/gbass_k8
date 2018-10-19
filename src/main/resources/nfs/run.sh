
###########################################################################################################
# run from the console through this command `sh ./run.sh`
###########################################################################################################

# Make sure to have kubernetes with that version else errors related to DNS will occur
kubectl version
# Client Version: version.Info{Major:"1", Minor:"8", GitVersion:"v1.8.6", GitCommit:"6260bb08c46c31eea6cb538b34a9ceb3e406689c", GitTreeState:"clean", BuildDate:"2017-12-21T06:34:11Z", GoVersion:"go1.8.3", Compiler:"gc", Platform:"linux/amd64"}
# Server Version: version.Info{Major:"1", Minor:"8+", GitVersion:"v1.8.6-gke.0", GitCommit:"ee9a97661f14ee0b1ca31d6edd30480c89347c79", GitTreeState:"clean", BuildDate:"2018-01-05T03:36:42Z", GoVersion:"go1.8.3b4", Compiler:"gc", Platform:"linux/amd64"}
## PS: by default, the kubectl on GKE is 1.7.11-gke.1
## So, upgrade the GKE cluster to v1.8.6-gke.0

# create a GCE persistent disk
gcloud compute disks create --size=10GB --zone=us-east4-b gce-nfs-disk


kubectl create -f deployment.yml # have a look on https://kubernetes.io/docs/concepts/storage/volumes/#gcepersistentdisk
kubectl create -f service.yml
kubectl create -f volume.yml
