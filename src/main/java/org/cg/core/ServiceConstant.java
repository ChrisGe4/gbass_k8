package org.cg.core;

/**
 * @author Chris.Ge
 */
public class ServiceConstant {
    public static final String DELETE_PODS_CMD = "kubectl delete pod --all -n %s ";
    public static final String SET_PROJECT = "gcloud config set project ";
    public static final String SCP = "bash "
        + "gcloud compute scp --recurse --project GCP_PROJECT --zone ZONE FILE PEERNAME:~/DEST_FILE";
    public static final String SCP_TO_SERVER = "bash "
        + "gcloud compute scp --recurse --project GCP_PROJECT --zone ZONE  PEERNAME:~/FILE DEST";
    public static final String SSH = "bash " + "gcloud compute ssh ";
    public static final String COPY_TO_GCS_CMD = "gsutil -m cp -r %s gs://%s";
    public static final String COPY_TO_NFS_CMD = "cp -r %s %s";
    public static final String DELETE_FROM_NFS_CMD = "rm -r %s";
    public static final String MOUNT_NFS_CMD =
        "sudo mount -t nfs -o proto=tcp,port=2049 %s:/exports %s";
    public static final String UNMOUNT_TO_NFS_CMD = "sudo umount -f -l ";
    public static final String CHANAGE_NFS_DIR_OWNER_CMD = "sudo chown %s:%s %s";

    public static final String COUCHDB_PORT_PLACEHOLDER = "COUDH_DB_PORT";
    public static final int ORDERER_BASE_NODEPORT = 32000;
    public static final String NODEPORT_PLACEHOLDER = "NODEPORT";
    public static final String NODEPORT_PEER_PLACEHOLDER = "NODEPORT_PEER";
    public static final String NODEPORT_PEER_EVENT_PLACEHOLDER = "NODEPORT_EVENT";
    public static final String NODEPORT_PEER_CHAINCODE_PLACEHOLDER = "NODEPORT_CHAINCODE";
    public static final String PEER_CHAINCODE_PORT_PLACEHOLDER = "PEER_CHAINCODE_PORT";
    public static final String BUCKET_PLACEHOLDER = "GCS_BUCKET";
    public static final String GCP_PROJECT_NAME_PLACEHOLDER = "GCP_PROJECT_NAME";
    public static final String ADMIN_USER_PLACEHOLDER = "ADMIN_USER";
    public static final String DELETE_SERVICE_CMD = "kubectl delete service --all -n ";
    public static final String DELETE_NAMESPACE_CMD = "kubectl delete namespace ";
    public static final String DELETE_DEPLOYMENT_CMD = "kubectl delete deployment --all -n %s ";

    public static final String K8_DEPLOY_CMD = "kubectl apply -f ";
    public static final String DELETE_PV_CMD = "kubectl delete pv %s -n %s";
    public static final String DELETE_PVC_CMD = "kubectl delete pvc %s -n %s";
    public static final String GET_SERVICE_CLUSTER_IP_CMD =
        "kubectl get svc %s -n %s -o yaml | grep clusterIP:|awk '{$1=$1};1'| cut -d' ' -f 2";
    public static final String SERVICE_CLUSTER_IP_PLACEHOLDER = "SERVICE_CLUSTER_IP";
    public static final String REPLACE_SERVICE_CLUSTER_IP_CMD =
        "sed -i \"s/SERVICE_CLUSTER_IP/$(%s)/g\" %s";
    public static final String GET_POD_NAME_CMD =
        "podname = $(kubectl get pods -n %s  --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}')";
    public static final String CREATE_CHANNEL_CMD =
        "kubectl exec -c cli  $(kubectl get pods -n %s  --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}')   -n %s   -- peer channel create -o orderer.%s:7050 -c test -f /opt/gopath/src/org/peer/channel-artifacts/test.tx --tls --cafile /etc/hyperledger/crypto/orderer/tls/ca.crt";
    public static final String JOIN_CHANNEL_CMD =
        "kubectl exec -c cli $(kubectl get pods -n %s  --field-selector=status.phase=Running -o jsonpath='{.items[%s].metadata.name}') -n %s  -- peer channel join -b %s.block";
    public static final String UPDATE_ANCHOR_PEER_CMD =
        "kubectl exec -c cli $(kubectl get pods -n ORGNIZATION  --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}') -n ORGNIZATION  -- peer channel update -o orderer.DOMAIN:7050 -c CHANNEL -f ORGNIZATIONMSPanchors.tx --tls --cafile /etc/hyperledger/crypto/orderer/tls/ca.crt";
    public static final String INSTALL_CHAINCODE_CMD =
        "kubectl exec -c cli $(kubectl get pods -n %s  --field-selector=status.phase=Running -o jsonpath='{.items[%s].metadata.name}') -n %s -- peer chaincode install -n mycc -v 1.0 -p chaincode_example02";

    public static String ORG_NAMES = "                    - *";
    public static String PROJECT_VM_DIR = "gbaas/";
    public static String CRYPTO_DIR = "crypto-config/";
    public static String CONTAINER_WORKING_DIR = "/etc/hyperledger/artifacts/";
    public static String DOCKER_CMD =
        "docker-compose -f ./gbaas/docker-compose.yaml run CLI bash -c \\\"COMMAND\\\"";
    public static String MSP_SUFFIX = "MSP";
    public static String PEER_CA_FILE = "peerOrganizations/ORG.DOMAIN/peers/peer0.ORG/tls/";
    public static String CRYPTO_FOLDER_FOR_COMPOSER =
        PROJECT_VM_DIR + CRYPTO_DIR + "peerOrganizations/ORG.DOMAIN/users/Admin@ORG/msp/";
    public static String ORG_PLACEHOLDER = "ORGNIZATION";
    public static String NFS_IP_PLACEHOLDER = "NFS_IP";
    public static String DOMAIN_PLACEHOLDER = "DOMAIN";
    public static String CHANNEL_PLACEHOLDER = "CHANNEL";
    public static String ORDERER_NAME_PLACEHOLDER = "ORDERER_NAME";
    public static String ORDERER_PORT_PLACEHOLDER = "ORDERER_PORT";
    public static String PEER_PORT_PLACEHOLDER = "PEER_PORT";
    public static String PEER_NAME_PLACEHOLDER = "PEER_NAME";
    public static String PEER_EVENT_PORT_PLACEHOLDER = "PEER_EVENT_PORT";
    public static String CONNECTION_FILE_NAME_TEMPLATE = "NETWORKNAME-connection-ORG";
    public static String ANCHORPEER_CMD =
        "peer channel update -o ORDERER_HOST:ORDERER_PORT -c CHANNEL_NAME -f ANCHOR_FILE --tls --cafile ORDERER_CA";
    public static String ORDERER_CA_IN_CONTAINER = "/etc/hyperledger/crypto/orderer/tls/ca.crt";
    public static String ANCHOR_FILE_SUFFIX = "anchors.tx";
    public static String CREATE_CARD_CMD =
        "composer card create -p CONNECTION_JSON -u PeerAdmin -c ADMIN_PEM -k SK_FILE -r PeerAdmin -r ChannelAdmin -f PeerAdmin@NAME.card";
    public static String COMPOSER_IMPORT_CARD_CMD =
        "composer card import -f PeerAdmin@NAME.card --card PeerAdmin@NAME";
    public static String COMPOSER_DELETE_CARD_CMD = "composer card delete -c PeerAdmin@NAME";
    public static String ORDERER_CA_FILE =
        "ordererOrganizations/DOMAIN/orderers/orderer.DOMAIN/tls/";
    public static String CRYPTO_DIR_PEER = "peerOrganizations/";
    public static String CRYPTO_DIR_ORDERER = "ordererOrganizations/";
    public static String CREATE_FOLDER_CMD = "mkdir -p -m u+x %s";
    public static int NODEPORT_FACTOR = 30000;
    //which also limits the number of peers per org can have
    public static int NODEPORT_INTERVAL = 100;
    public static String WORKING_DIR_PROPERTY="WORKING_DIR";

    public static String COPY_TO_NFS_POD ="kubectl cp -n %s  %s $(kubectl get pods -n %s  --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}'):exports";
    public static String DELETE_FROM_NFS_POD ="kubectl exec  $(kubectl get pods -n %s  --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}') -n %s -- rm -rf exports/%s";
    public static String GET_CREDENTIAL="gcloud container clusters get-credentials %s --zone=%s";

    private ServiceConstant() {
    }
}
