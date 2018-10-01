package org.cg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.cg.core.AppConfiguration;
import org.cg.error.CommandFailedToRunException;
import org.cg.pojo.NetworkConfig;
import org.cg.pojo.OrgConfig;
import org.cg.pojo.PeerNodePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.cg.core.ServiceConstant.*;

/**
 * @author Chris.Ge
 */
@Component
public class DeploymentService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);

    private final ObjectMapper mapper;
    private final AppConfiguration appConfiguration;
    public String cryptoGenCmd;
    private String workingDir;
    private String scriptFile;
    private String cryptoPath;
    private String composerPath;
    private String idemixPath;
    private String yamlFileDir = "k8_nfs";
    private String nfsFileDir;

    @Autowired
    public DeploymentService(ObjectMapper mapper, AppConfiguration appConfiguration) {
        this.mapper = mapper;
        this.appConfiguration = appConfiguration;

    }

    // ************For testing purpose****************

    public static void main(String[] args) {

        ObjectMapper mapper = new ObjectMapper();
        AppConfiguration appConfiguration = new AppConfiguration();
        OrgConfig property1 = new OrgConfig();
        property1.setOrg("org1");
        property1.setNumOfPeers(2);
        OrgConfig property2 = new OrgConfig();
        property2.setOrg("org2");
        property2.setNumOfPeers(2);
        NetworkConfig config = new NetworkConfig();
        config.setGcpProjectName("hyperledger-poc");
        config.setOrdererName("orderer");
        config.setGcpZoneName("us-east1-b");
        config.setChannelName("test");
        config.setDomain("test");
        config.setNfsIp("10.63.253.112");
        config.setNfsNamespace("default");
        config.setStorageBucket("hyperledger-poc");
        config.setUseGcs(false);
        config.setOrgConfigs(Lists.newArrayList(property1, property2));
        DeploymentService ds = new DeploymentService(mapper, appConfiguration);

        Map<String, List<String>> orgPeerMap = ds.deployFabric(config, false, false, false, false);

        ds.runScript();

    }


    public Map<String, List<String>> deployFabric(NetworkConfig config, boolean deleteDeployment,
        boolean deleteService, boolean deletePVC, boolean deleteNameSpace) {
        initialEnvVariables(config);
        Map<String, List<String>> orgPeerMap = getOrgPeerMap(config);

        initService(config, orgPeerMap);
        deleteExistingApplication(config, orgPeerMap, deleteDeployment, deleteService, deletePVC,
            deleteNameSpace);

        createCrypto(orgPeerMap, config);
        Map<String, String> orgPk = createCryptoFiles(orgPeerMap, config);
        createConfigtxYaml(orgPeerMap, config);
        createConfigtxFiles(orgPeerMap, config);
        copyChaincode(orgPeerMap, config);
        copyGeneratedFilesToStorage(orgPeerMap, config);

        createNamespaceDeploymentYamlFiles(config, orgPeerMap);
        createServiceDeploymentYamlFiles(config, orgPeerMap);
        String ordererYamlFile = createOrdererDeploymentYamlFiles(config);
        List<String> peerYamlFiles = createPeerDeploymentYamlFiles(config, orgPeerMap);

        createChannelBlock(config, orgPeerMap);
        joinChannel(config, orgPeerMap);
        updateAnchorPeer(config, orgPeerMap);
        installChaincode(config, orgPeerMap);
        return orgPeerMap;
    }

    private void initialEnvVariables(NetworkConfig config) {

        workingDir = System.getenv(WORKING_DIR_PROPERTY);
        if (Strings.isNullOrEmpty(workingDir)) {
            workingDir = appConfiguration.WORKING_DIR + config.getDomain() + "/";
        }

        composerPath = workingDir + "composer/";
        cryptoGenCmd = "~/bin/cryptogen generate --output=" + workingDir + "crypto-config --config="
            + workingDir + "cryptogen.yaml";
        scriptFile = workingDir + "script.sh";
        cryptoPath = workingDir + CRYPTO_DIR;
        idemixPath = cryptoPath + "idemix";
        // idemixIssuerKeysCmd = "~/bin/idemixgen ca-keygen";
        // idemixSignerKeysCmd = "~/bin/idemixgen -u OU1 -e OU1 -r 1";
        nfsFileDir = appConfiguration.NFS_MOUNT_PATH + config.getDomain() + "/";
        if (config.getUseGcs()) {
            yamlFileDir = "k8_gcs";
        }
    }

    public void initService(NetworkConfig config, Map<String, List<String>> orgPeerMap) {

        try {
            deleteFolders(workingDir);
            createDir(workingDir);
            createScriptFile(config.getGcpProjectName());
        } catch (Throwable t) {
            throw new RuntimeException("Cannot init the service ", t);
        }

    }

    private void createScriptFile(String projectName) {
        try {
            Files.deleteIfExists(Paths.get(scriptFile));
            Files.write(Paths.get(scriptFile),
                ("export PATH=$PATH:" + appConfiguration.GCLOUD_DIR + "\n").getBytes(),
                StandardOpenOption.CREATE);
            appendToFile(scriptFile, SET_PROJECT + projectName);

            log.info("script file created");
            appendToFile(scriptFile, "");
        } catch (Throwable t) {
            throw new RuntimeException("Cannot create script file ", t);
        }
    }

    public ImmutableMap<String, List<String>> getOrgPeerMap(NetworkConfig config) {

        List<OrgConfig> orgConfigList = config.getOrgConfigs();
        Builder<String, List<String>> orgPeerMapBuilder = ImmutableMap.builder();
        for (OrgConfig orgConfig : orgConfigList) {

            int num = orgConfig.getNumOfPeers();
            String org = orgConfig.getOrg();
            List<String> peers =
                IntStream.range(0, num).mapToObj(n -> "peer" + n).collect(toList());
            orgPeerMapBuilder.put(org, peers);
        }
        return orgPeerMapBuilder.build();
    }

    public void deleteDeployments(List<String> namespaces, String projectName) {

        createScriptFile(projectName);
        namespaces.stream().forEach(n -> {

            runDeleteCommand(String.format(DELETE_DEPLOYMENT_CMD, n));
            appendToFile(scriptFile, "sleep 15");
            runDeleteCommand(String.format(DELETE_PODS_CMD, n));
            appendToFile(scriptFile, "sleep 5");

        });
    }

    private void deleteExistingApplication(NetworkConfig config,
        Map<String, List<String>> orgPeerMap, boolean deleteDeployment, boolean deleteService,
        boolean deletePVC, boolean deleteNameSpace) {

        List<String> namespaces = Lists.newArrayList(orgPeerMap.keySet());
        namespaces.add(config.getDomain());

        namespaces.stream().forEach(n -> {

            if (deleteDeployment) {
                runDeleteCommand(String.format(DELETE_DEPLOYMENT_CMD, n));
                appendToFile(scriptFile, "sleep 15");
                runDeleteCommand(String.format(DELETE_PODS_CMD, n));
                appendToFile(scriptFile, "sleep 5");
            }
            if (deleteService) {
                runDeleteCommand(DELETE_SERVICE_CMD + n);
            }
            if (deletePVC) {
                runDeleteCommand(String.format(DELETE_PVC_CMD, n + "-pv", n));
                runDeleteCommand(String.format(DELETE_PV_CMD, n + "-pv", n));
            }
            if (deleteNameSpace) {
                runDeleteCommand(DELETE_NAMESPACE_CMD + n);
            }
        });

        if (deleteNameSpace) {
            runDeleteCommand(DELETE_NAMESPACE_CMD + config.getDomain());
        }
        //todo: gcs clean up if needed
        if (!config.getUseGcs()) {
            //    appendToFile(scriptFile, String.format(DELETE_FROM_NFS_CMD, nfsFileDir));
            appendToFile(scriptFile, String
                .format(DELETE_FROM_NFS_POD, config.getNfsNamespace(), config.getNfsNamespace(),
                    config.getDomain()));
        }
        appendToFile(scriptFile, "sleep 5");

    }


    private void runDeleteCommand(String cmd) {

        appendToFile(scriptFile, "echo " + cmd);
        appendToFile(scriptFile, cmd);
        appendToFile(scriptFile, "sleep 5");

    }


    public void createCrypto(Map<String, List<String>> orgPeerMap, NetworkConfig config) {
        try {
            log.info("Creating cryptogen.yaml");
            String ordererTemplate = new String(Files.readAllBytes(Paths
                .get(Resources.getResource("template/cryptogentemplate-orderer.yaml").toURI())));

            String peerTemplate = new String(Files.readAllBytes(
                Paths.get(Resources.getResource("template/cryptogentemplate-peer.yaml").toURI())));

            Set<String> orgs = orgPeerMap.keySet();
            String peersConfig = orgs.stream().filter(o -> !o.equals(config.getOrdererName())).map(
                o -> peerTemplate.replaceAll("ORG", o)
                    .replace("COUNT", String.valueOf(orgPeerMap.get(o).size())))
                .collect(Collectors.joining("\n"));
            String path = workingDir + "cryptogen.yaml";
            Files.write(Paths.get(path), ordererTemplate.replace(":PEERS", peersConfig)
                .replaceAll("DOMAIN", config.getDomain()).getBytes(), StandardOpenOption.CREATE);
            log.info("Cryptogen.yaml created");

        } catch (Throwable e) {
            throw new RuntimeException("Cannot create crypto yaml file ", e);
        }

    }

    public Map<String, String> createCryptoFiles(Map<String, List<String>> orgPeerMap,
        NetworkConfig config) {
        try {
            log.info("Generating crypto files");
            deleteFolders(workingDir + "crypto-config");
            CommandRunner.runCommand(Lists.newArrayList("/bin/bash", "-c", cryptoGenCmd), log);
            Set<String> orgs = Sets.newHashSet(orgPeerMap.keySet());
            orgs.remove(config.getOrdererName());
            Map<String, String> orgDomainPkMap = new HashMap<>(orgs.size());

            for (String org : orgs) {
                String path = cryptoPath + "peerOrganizations/" + org + "/ca/";
                Optional<String> skFile =
                    Files.list(Paths.get(path)).map(f -> f.getFileName().toString())
                        .filter(p -> p.endsWith("_sk")).findFirst();
                if (skFile.isPresent()) {
                    orgDomainPkMap.put(org, skFile.get());
                } else {
                    throw new RuntimeException("Cannot file ca files ");
                }
            }
            log.info("Crypto files were created");

            return orgDomainPkMap;

        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Cannot create crypto  files ", t);
        }
    }

    public void copyChaincode(Map<String, List<String>> orgPeerMap, NetworkConfig config) {
        log.info("Calling distributeChaincode method");
        try {

            FileUtils.copyDirectory(new File(Resources.getResource("chaincode").toURI()),
                new File(Paths.get(workingDir, "chaincode").toUri()), false);

        } catch (Throwable t) {
            throw new RuntimeException("Cannot copy chaincode to from resource", t);
        }
    }

    public void copyGeneratedFilesToStorage(Map<String, List<String>> orgPeerMap,
        NetworkConfig config) {

        appendToFile(scriptFile, "echo copying generated files to storage");
        if (config.getUseGcs()) {
            copyFolderToGCS(workingDir, config.getStorageBucket(), config.getDomain());
        } else {
            //copyFolderToNFS(workingDir, nfsFileDir);
            appendToFile(scriptFile,
                String.format(COPY_TO_NFS_POD, workingDir, config.getNfsNamespace()));
        }

    }


    public void createConfigtxYaml(Map<String, List<String>> orgPeerMap, NetworkConfig config) {

        try {
            String orgTemplate = new String(Files.readAllBytes(
                Paths.get(Resources.getResource("template/configtx-orgtemplate.yaml").toURI())));

            String template = new String(Files.readAllBytes(
                Paths.get(Resources.getResource("template/configtxtemplate.yaml").toURI())));
            Set<String> orgs = orgPeerMap.keySet();
            String orgNames = orgs.stream().filter(o -> !o.equals(config.getOrdererName()))
                .map(o -> ORG_NAMES + o).collect(Collectors.joining("\n"));

            String orgConfigs =
                orgs.stream().filter(o -> !o.equals(config.getOrdererName())).map(o ->

                    orgTemplate.replaceAll("ORG", o)

                ).collect(Collectors.joining(System.lineSeparator()));
            String configtxfPath = workingDir + "configtx.yaml";
            deleteFolders(configtxfPath);
            String content =
                template.replace(":ORG-CONFIGS", orgConfigs).replace("ORGNAMES", orgNames)
                    .replaceAll("DOMAIN", config.getDomain())
                    .replace("CHANNEL_NAME", config.getChannelName());

            Files.write(Paths.get(configtxfPath), content.getBytes(), StandardOpenOption.CREATE);

        } catch (Throwable e) {
            throw new RuntimeException("Cannot create configtx yaml file ", e);
        }


    }

    public void createConfigtxFiles(Map<String, List<String>> orgPeerMap, NetworkConfig config) {

        try {
            String dir = workingDir + "channel/";
            deleteFolders(dir);
            createDir(dir);

            appendToFile(scriptFile, "export FABRIC_CFG_PATH=" + workingDir);
            log.info("Generating channel config transaction for %s channel",
                config.getChannelName());
            appendToFile(scriptFile, String
                .join("", "~/bin/configtxgen -profile OrdererGenesis -outputBlock ", dir,
                    "genesis.block", " -channelID ", config.getDomain(), "-orderer-syschan "));
            appendToFile(scriptFile, String
                .join("", "~/bin/configtxgen -profile ", config.getChannelName(),
                    " -outputCreateChannelTx ", dir, config.getChannelName(), ".tx -channelID ",
                    config.getChannelName()));

            Set<String> orgs = Sets.newHashSet(orgPeerMap.keySet());
            orgs.remove(config.getOrdererName());

            for (String org : orgs) {
                appendToFile(scriptFile, String
                    .join("", "~/bin/configtxgen -profile ", config.getChannelName(),
                        " -outputAnchorPeersUpdate ", dir, org, "MSPanchors.tx -channelID ",
                        config.getChannelName(), " -asOrg ", org, "MSP"));
            }

        } catch (Throwable e) {
            throw new RuntimeException("Cannot create channel related files ", e);
        }

    }

    public void copyTxToStorage(NetworkConfig config) {
        log.info("Distributing tx files");

        try {
            String path = workingDir + "channel";
            appendToFile(scriptFile,
                "echo copying channel folder to bucket " + config.getStorageBucket());
            copyFolderToGCS(path, config.getStorageBucket(), config.getDomain());
        } catch (Throwable t) {
            throw new RuntimeException("Cannot write to script file ", t);
        }
    }

    public void createCaServerYaml(Map<String, Map<String, String>> orgNameIpMap,
        NetworkConfig config) {
        log.info("Creating Ca Server Yaml files");

        try {
            String caTemplate = new String(Files.readAllBytes(Paths.get(
                Resources.getResource("template/fabric-ca-server-configtemplate.yaml").toURI())));

            Set<String> orgs = Sets.newHashSet(orgNameIpMap.keySet());
            orgs.remove(config.getOrdererName());

            for (String org : orgs) {
                String fileName = "fabric-ca-server-config-" + org + ".yaml";
                String caServerYamlFilePath = workingDir + fileName;
                String content = caTemplate.replaceAll("ORG", org);
                Files.write(Paths.get(caServerYamlFilePath), content.getBytes(),
                    StandardOpenOption.CREATE);
                for (String instance : orgNameIpMap.get(org).keySet()) {
                    copyFileToGcpVm(caServerYamlFilePath, PROJECT_VM_DIR + fileName, instance,
                        config);

                }
            }


        } catch (Throwable e) {
            throw new RuntimeException("Cannot create ca server yaml file ", e);
        }

    }

    public List<String> createServiceDeploymentYamlFiles(NetworkConfig config,
        Map<String, List<String>> orgPeersMap) {
        log.info("Creating Service yaml file");
        List<String> serviceYamlFiles = Lists.newArrayList();

        try {
            String serviceTemplate = new String(Files.readAllBytes(Paths.get(
                Resources.getResource(yamlFileDir + "/fabric_k8_template_service.yaml").toURI())));

            List<String> orgList = new ArrayList<>(orgPeersMap.keySet());

            for (int i = 0; i < orgList.size(); i++) {
                String org = orgList.get(i);
                List<String> peerList = orgPeersMap.get(org);
                for (int j = 0; j < peerList.size(); j++) {

                    String peer = peerList.get(j);
                    PeerNodePort peerNodePort = getPeerNodePort(i, j);
                    String content = serviceTemplate
                        .replaceAll(PEER_PORT_PLACEHOLDER, appConfiguration.PEER_PORT)
                        .replaceAll(PEER_EVENT_PORT_PLACEHOLDER, appConfiguration.PEER_EVENT_PORT)
                        .replaceAll(PEER_CHAINCODE_PORT_PLACEHOLDER,
                            appConfiguration.PEER_CHAINCODE_PORT)
                        .replaceAll(PEER_NAME_PLACEHOLDER, peer)
                        .replaceAll(NODEPORT_PEER_PLACEHOLDER,
                            String.valueOf(peerNodePort.getPeerPort()))
                        .replaceAll(NODEPORT_PEER_EVENT_PLACEHOLDER,
                            String.valueOf(peerNodePort.getEventPort()))
                        .replaceAll(NODEPORT_PEER_CHAINCODE_PLACEHOLDER,
                            String.valueOf(peerNodePort.getChaincodePort()))
                        .replaceAll(ORG_PLACEHOLDER, org);

                    String yamlFileName =
                        String.join("", "fabric_k8_", org, "-", peer, "-service.yaml");
                    String fileName = String.join("", workingDir, yamlFileName);
                    Files.write(Paths.get(fileName), content.getBytes(), StandardOpenOption.CREATE);
                    appendToFile(scriptFile, "echo apply file " + fileName);
                    appendToFile(scriptFile, K8_DEPLOY_CMD + fileName);
                    appendToFile(scriptFile, "sleep 5");
                    serviceYamlFiles.add(yamlFileName);
                }
            }

            return serviceYamlFiles;
        } catch (Throwable e) {
            throw new RuntimeException("Cannot create orderer k8 yaml file ", e);
        }
    }


    public String createOrdererDeploymentYamlFiles(NetworkConfig config) {
        log.info("Creating Orderer yaml file");

        try {
            String ordererTemplate = new String(Files.readAllBytes(Paths.get(
                Resources.getResource(yamlFileDir + "/fabric_k8_template_orderer.yaml").toURI())));

            //create orderer k8 yaml file
            String orderer =
                ordererTemplate.replaceAll(ORDERER_PORT_PLACEHOLDER, appConfiguration.ORDERER_PORT)

                    .replaceAll(DOMAIN_PLACEHOLDER, config.getDomain())
                    .replaceAll(ORDERER_NAME_PLACEHOLDER, config.getOrdererName())
                    .replaceAll(NODEPORT_PLACEHOLDER, String.valueOf(getOrdererNodePort(1)))
                    .replaceAll(GCP_PROJECT_NAME_PLACEHOLDER, config.getGcpProjectName())
                    .replaceAll(BUCKET_PLACEHOLDER, config.getStorageBucket());

            String fileName = String.join("", workingDir, "fabric_k8_orderer.yaml");
            Files.write(Paths.get(fileName), orderer.getBytes(), StandardOpenOption.CREATE);
            appendToFile(scriptFile, "echo apply file " + fileName);
            appendToFile(scriptFile, K8_DEPLOY_CMD + fileName);
            appendToFile(scriptFile, "sleep 5");

            return fileName;
        } catch (Throwable e) {
            throw new RuntimeException("Cannot create orderer k8 yaml file ", e);
        }


    }

    public List<String> createPeerDeploymentYamlFiles(NetworkConfig config,
        Map<String, List<String>> orgPeersMap) {
        List<String> peersYamlFiles = Lists.newArrayList();
        log.info("Creating peers yaml files");

        try {
            String peerTemplate = new String(Files.readAllBytes(Paths.get(
                Resources.getResource(yamlFileDir + "/fabric_k8_template_peer.yaml").toURI())));
            //creating peer k8 yaml files

            List<String> orgList = new ArrayList<>(orgPeersMap.keySet());

            for (int i = 0; i < orgList.size(); i++) {
                String org = orgList.get(i);
                List<String> peerList = orgPeersMap.get(org);
                for (int j = 0; j < peerList.size(); j++) {

                    String peer = peerList.get(j);
                    String content =
                        peerTemplate.replaceAll(PEER_PORT_PLACEHOLDER, appConfiguration.PEER_PORT)
                            .replaceAll(PEER_EVENT_PORT_PLACEHOLDER,
                                appConfiguration.PEER_EVENT_PORT)
                            .replaceAll(PEER_CHAINCODE_PORT_PLACEHOLDER,
                                appConfiguration.PEER_CHAINCODE_PORT)
                            .replaceAll(PEER_NAME_PLACEHOLDER, peer)
                            .replaceAll(DOMAIN_PLACEHOLDER, config.getDomain())
                            .replaceAll(ORG_PLACEHOLDER, org)
                            .replaceAll(BUCKET_PLACEHOLDER, config.getStorageBucket())
                            .replaceAll(COUCHDB_PORT_PLACEHOLDER, appConfiguration.COUDH_DB_PORT)
                            .replaceAll(GCP_PROJECT_NAME_PLACEHOLDER, config.getGcpProjectName())
                            .replaceAll(BUCKET_PLACEHOLDER, config.getStorageBucket())
                            //todo: hardcode for now
                            .replaceAll(ADMIN_USER_PLACEHOLDER, "Admin");
                    // .replaceAll(PEER_NAME_PLACEHOLDER, peer).replaceAll("CA_PRIVATE_KEY",
                    //     orgDomainPkMap.get(org + "." + config.getDomain());

                    String yamlFileName = String.join("", "fabric_k8_", org, "-", peer, ".yaml");
                    String fileName = String.join("", workingDir, yamlFileName);
                    Files.write(Paths.get(fileName), content.getBytes(), StandardOpenOption.CREATE);
                    appendToFile(scriptFile, "echo set service cluster ip to " + fileName);
                    appendToFile(scriptFile, String.format(REPLACE_SERVICE_CLUSTER_IP_CMD,
                        String.format(GET_SERVICE_CLUSTER_IP_CMD, peer, org), fileName));
                    appendToFile(scriptFile, "echo apply file " + fileName);
                    appendToFile(scriptFile, K8_DEPLOY_CMD + fileName);
                    peersYamlFiles.add(yamlFileName);
                }
            }
            return peersYamlFiles;
        } catch (Throwable e) {
            throw new RuntimeException("Cannot create peer k8 yaml file ", e);
        }
    }


    public void createNamespaceDeploymentYamlFiles(NetworkConfig config,
        Map<String, List<String>> orgPeersMap) {
        log.info("Creating namespace yaml files");

        try {
            String namespaceTemplate = new String(Files.readAllBytes(Paths.get(
                Resources.getResource(yamlFileDir + "/fabric_k8_template_namespace.yaml")
                    .toURI())));

            Set<String> orgs = new HashSet<>(orgPeersMap.keySet());
            orgs.add(config.getDomain());

            for (String org : orgs) {

                //create orderer k8 yaml file
                String namespace = namespaceTemplate.replaceAll(ORG_PLACEHOLDER, org)
                    .replaceAll(NFS_IP_PLACEHOLDER, config.getNfsIp());

                String fileName = String.join("", workingDir, org, "_namespace.yaml");
                Files.write(Paths.get(fileName), namespace.getBytes(), StandardOpenOption.CREATE);
                appendToFile(scriptFile, "echo apply file " + fileName);
                appendToFile(scriptFile, K8_DEPLOY_CMD + fileName);
            }

            appendToFile(scriptFile, "sleep 5");
        } catch (Throwable e) {
            throw new RuntimeException("Cannot create namespace k8 yaml file ", e);
        }


    }

    public void createChannelBlock(NetworkConfig config, Map<String, List<String>> orgPeersMap) {
        log.info("Create channel block");
        appendToFile(scriptFile, "sleep 20");

        String org = orgPeersMap.keySet().stream().findFirst().get();

        String createChannelCmd = String.format(CREATE_CHANNEL_CMD, org, org, config.getDomain());
        log.info("Create channel command " + createChannelCmd);

        appendToFile(scriptFile, "echo  Create channel " + config.getChannelName());
        appendToFile(scriptFile, createChannelCmd);
        appendToFile(scriptFile, "sleep 5");

    }

    public void joinChannel(NetworkConfig config, Map<String, List<String>> orgPeersMap) {
        log.info("Joining channel");
        appendToFile(scriptFile, "echo Joining channel...");
        for (String org : orgPeersMap.keySet()) {
            List<String> peerList = orgPeersMap.get(org);
            for (int i = 0; i < peerList.size(); i++) {
                String joinChannelCmd = String
                    .format(JOIN_CHANNEL_CMD, org, String.valueOf(i), org, config.getChannelName());
                log.info("Joining channel command " + joinChannelCmd);
                appendToFile(scriptFile, joinChannelCmd);

            }
        }
        appendToFile(scriptFile, "sleep 5");

    }

    public void updateAnchorPeer(NetworkConfig config, Map<String, List<String>> orgPeersMap) {
        log.info("Updating anchorPeer...");
        appendToFile(scriptFile, "echo Updating anchorPeer...");
        for (String org : orgPeersMap.keySet()) {
            String updateAnchorPeerCmd = UPDATE_ANCHOR_PEER_CMD.replaceAll(ORG_PLACEHOLDER, org)
                .replaceAll(DOMAIN_PLACEHOLDER, config.getDomain())
                .replaceAll(CHANNEL_PLACEHOLDER, config.getChannelName());
            log.info("Update anchorPeer command " + updateAnchorPeerCmd);

            appendToFile(scriptFile, updateAnchorPeerCmd);
        }
        appendToFile(scriptFile, "sleep 5");

    }

    public void installChaincode(NetworkConfig config, Map<String, List<String>> orgPeersMap) {

        log.info("Install chaincode");

        appendToFile(scriptFile, "echo Install chaincode...");

        for (String org : orgPeersMap.keySet()) {
            List<String> peerList = orgPeersMap.get(org);
            for (int i = 0; i < peerList.size(); i++) {

                String installChaincode =
                    String.format(INSTALL_CHAINCODE_CMD, org, String.valueOf(i), org);
                log.info("Install chaincode command " + installChaincode);

                appendToFile(scriptFile, installChaincode);
            }
        }
    }


    private String readCaCert(String file) throws IOException {

        return Files.readAllLines(Paths.get(file)).stream().collect(joining("\n")) + "\n";
    }


    public void createComposerAdminCard(Map<String, Map<String, String>> orgNameIpMap,
        NetworkConfig config) {
        try {
            log.info("Generating composer admin card files");
            // deleteFolders(COMPOSER_CARD_FOLDER);
            // createDir(COMPOSER_CARD_FOLDER);

            Set<String> orgs = Sets.newHashSet(orgNameIpMap.keySet());
            orgs.remove(config.getOrdererName());
            String networkName = String.join("-", orgs) + "-network";

            // String fileNameTemplate =
            //     CONNECTION_FILE_NAME_TEMPLATE.replace("NETWORKNAME", networkName);
            for (String org : orgs) {

                //String connectionFile = fileNameTemplate.replace("ORG", org);
                String connectionFile = appConfiguration.COMPOSER_CONNECTION_FILE;
                String adminPemFileFolder =
                    CRYPTO_FOLDER_FOR_COMPOSER.replaceAll(ORG_PLACEHOLDER, org)
                        .replaceAll(DOMAIN_PLACEHOLDER, config.getDomain());

                // copyFileToGcpVm(cardFile, PROJECT_VM_DIR + "connection.yaml", host, config);
                String cardName = CONNECTION_FILE_NAME_TEMPLATE.replace("NETWORKNAME", networkName)
                    .replace("ORG", org);
                String createCardCommand =
                    CREATE_CARD_CMD.replace("CONNECTION_JSON", connectionFile + ".json")
                        .replace("ADMIN_PEM", adminPemFileFolder + "signcerts/A*.pem")
                        .replace("SK_FILE", adminPemFileFolder + "keystore/*_sk")
                        .replace("NAME", cardName);
                //.replace("FOLDER", COMPOSER_CARD_FOLDER);
                String deleteOldCardCmd = COMPOSER_DELETE_CARD_CMD.replace("NAME", cardName);

                String cardFile = "PeerAdmin@" + cardName;
                // CommandRunner.runCommand(Lists.newArrayList("/bin/bash", "-c", COMMPOSER_FW_DIR
                //         + "composer card create -p ~/blockchain/artifacts/composer/boa-google-network-connection-boa.json -u PeerAdmin -c ~/blockchain/artifacts/crypto-config/peerOrganizations/boa.sample.com/users/Admin@boa.sample.com/msp/signcerts/Admin@boa.sample.com-cert.pem -k ~/blockchain/artifacts/crypto-config/peerOrganizations/boa.sample.com/users/Admin@boa.sample.com/msp/keystore/*_sk -r PeerAdmin -r ChannelAdmin -f ~/blockchain/artifacts/composer/PeerAdmin@boa.card"),
                //     log);

                String host = String.join("-", org, "peer0");
                appendToFile(scriptFile, "echo Deleting file " + cardFile + " in " + host);
                appendToFile(scriptFile, String
                    .join("", SSH, host, " --zone ", config.getGcpZoneName(), " --command \" ",
                        deleteOldCardCmd, "\""));
                String gcpCmd = String
                    .join("", SSH, host, " --zone ", config.getGcpZoneName(), " --command \"",
                        createCardCommand, "\"");
                appendToFile(scriptFile,
                    "echo creating file " + cardFile + ".card" + " in " + host);
                appendToFile(scriptFile, gcpCmd);
                String importCardCmd = COMPOSER_IMPORT_CARD_CMD.replaceAll("NAME", cardFile);
                appendToFile(scriptFile, String
                    .join("", SSH, host, " --zone ", config.getGcpZoneName(), " --command \" ",
                        importCardCmd, "\""));
            }
            log.info("Composer admin cards were created");


        } catch (Throwable t) {
            throw new RuntimeException("Cannot Composer admin cards files ", t);
        }
    }


    public void runScript() {
        try {
            log.info("Running script file created");
            CommandRunner.runCommand(Lists.newArrayList("/bin/bash", "-c",
                String.join(" ", "chmod u+x", workingDir + "script.sh")), log);

            //CommandRunner.runCommand(Lists.newArrayList("/bin/sh", "-c", workingDir + "script.sh"), log);
            CommandRunner
                .runCommand(Lists.newArrayList("/bin/bash", "-c", workingDir + "script.sh"), log);

        } catch (Throwable t) {
            throw new RuntimeException("Cannot run script file ", t);
        }

    }


    public void copyFileToGcpVm(String file, String destFileName, String peerName,
        NetworkConfig config) {

        try {

            appendToFile(scriptFile, SCP.replace("GCP_PROJECT", config.getGcpProjectName())
                .replace("ZONE", config.getGcpZoneName()).replace("PEERNAME", peerName)
                .replace("DEST_FILE", destFileName).replace("FILE", file));
        } catch (Throwable t) {

            throw new CommandFailedToRunException("Cannot copy " + file + " to " + peerName, t);
        }

    }

    public void copyVmFileToGCP(String file, String destFileName, String peerName,
        NetworkConfig config) {

        try {
            appendToFile(scriptFile,
                SCP_TO_SERVER.replace("GCP_PROJECT", config.getGcpProjectName())
                    .replace("ZONE", config.getGcpZoneName()).replace("PEERNAME", peerName)
                    .replace("DEST", destFileName).replace("FILE", file));
        } catch (Throwable t) {

            throw new CommandFailedToRunException("Cannot copy " + file + " to " + peerName, t);
        }

    }

    public void copyFolderToGCS(String sourceFolder, String bucket, String domain) {

        try {
            appendToFile(scriptFile,
                String.format(COPY_TO_GCS_CMD, sourceFolder, bucket + "/" + domain));
        } catch (Throwable t) {

            throw new CommandFailedToRunException(
                "Cannot copy " + sourceFolder + " to bucket" + bucket, t);
        }

    }

    public void copyFolderToNFS(String sourceFolder, String nfsDir) {

        try {
            appendToFile(scriptFile, String.format(COPY_TO_NFS_CMD, sourceFolder, nfsDir));
        } catch (Throwable t) {

            throw new CommandFailedToRunException(
                "Cannot copy " + sourceFolder + " to nfs dir " + nfsDir, t);
        }

    }


    public void appendToFile(String fileName, String cmd) {

        try (BufferedWriter bw = Files
            .newBufferedWriter(Paths.get(fileName), StandardOpenOption.APPEND)) {
            bw.append(cmd);
            bw.newLine();
        } catch (Throwable t) {
            log.info(String.join(":", "Cannot add ", cmd, " to file", fileName));
            throw new RuntimeException(t);
        }
    }

    public void createDir(String dir) {
        try {
            if (!Files.exists(Paths.get(dir))) {
                Files.createDirectories(Paths.get(dir));
            }
        } catch (Throwable t) {
            log.info(String.join(":", "Cannot create folder", dir, t.toString()));
            throw new RuntimeException(t);
        }

    }

    public void deleteFolders(String dir) {

        try {
            if (Paths.get(dir).toFile().exists()) {
                Files.walk(Paths.get(dir)).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            System.err.println("Cannot delete folder " + dir);
            throw new RuntimeException(e);
        }

    }

    private PeerNodePort getPeerNodePort(int orgIndex, int peerIndex) {

        return new PeerNodePort(calculatePeerNodePort(orgIndex, peerIndex, 1),
            calculatePeerNodePort(orgIndex, peerIndex, 2),
            calculatePeerNodePort(orgIndex, peerIndex, 3));

    }

    private int calculatePeerNodePort(int orgIndex, int peerIndex, int type) {
        return NODEPORT_FACTOR + orgIndex * NODEPORT_INTERVAL + 3 * peerIndex + type;
    }

    private int getOrdererNodePort(int ordererIndex) {

        return ORDERER_BASE_NODEPORT + ordererIndex;
    }


}
