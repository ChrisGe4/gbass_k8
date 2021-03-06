package org.cg.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Chris.Ge
 */
@Component
public class AppConfiguration {

    // Configurable program variables
    @Value("${orderer.port:7050}")
    public String ORDERER_PORT = "7050";
    @Value("${ca.port:7054}")
    public String CA_PORT = "7054";
    @Value("${couchdb.port:5984}")
    public String COUDH_DB_PORT = "5984";
    @Value("${working.dir:/etc/blockchain/artifacts/}")
    //public String WORKING_DIR = "/usr/local/google/home/chrisge/blockchain/artifacts/";
    public String WORKING_DIR = "/Users/chrisge/blockchain/artifacts/";
    @Value("${bin.dir:/bin/}")
    public String BIN_DIR = "~/bin/";
    @Value("${peer.port:7051}")
    public String PEER_PORT = "7051";
    @Value("${peer.event.port:7053}")
    public String PEER_EVENT_PORT = "7053";
    @Value("${peer.chaincode.port:7052}")
    public String PEER_CHAINCODE_PORT = "7052";
    @Value("${composer.connnection.file:composer_connection}")
    public String COMPOSER_CONNECTION_FILE = "composer_connection";
    @Value("${gcloud.dir:/usr/bin/}")
    public String GCLOUD_DIR = "/usr/bin/";
    @Value("${project:hyperledger-poc}")
    public String PROJECT = "hyperledger-poc";
    @Value("${zone:us-east4-a}")
    public String ZONE = "us-east4-a";
    @Value("${cluster:gbaas-test}")
    public String CLUSTER = "gbaas-test";
    @Value("${nfs.mount.path:/usr/local/google/home/chrisge/nfs/}")
    public String NFS_MOUNT_PATH = "/usr/local/google/home/chrisge/nfs/";

}