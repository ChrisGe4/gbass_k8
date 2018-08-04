package org.cg.service;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.io.FileUtils;
import org.cg.config.AppConfiguration;
import org.cg.error.CommandFailedToRunException;
import org.cg.pojo.NetworkConfig;
import org.cg.pojo.OrgConfig;
import org.cg.pojo.PeerNodePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Chris.Ge
 */
@Component
public class DeploymentService {

  public static final String LIST_INSTANCES = "bash gcloud compute instances list";
  public static final String SET_PROJECT = "gcloud config set project ";
  public static final String SCP = "bash "
      + "gcloud compute scp --recurse --project GCP_PROJECT --zone ZONE FILE PEERNAME:~/DEST_FILE";
  public static final String SCP_TO_SERVER = "bash "
      + "gcloud compute scp --recurse --project GCP_PROJECT --zone ZONE  PEERNAME:~/FILE DEST";
  public static final String SSH = "bash " + "gcloud compute ssh ";
  public static final String COPY_TO_STORAGE_CMD = "gsutil -m cp -r %s gs://%s";

  private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);
  private static final String COUCHDB_PORT_PLACEHOLDER = "COUDH_DB_PORT";
  public static String ORG_NAMES = "                    - *";
  public static String PROJECT_VM_DIR = "gbaas/";
  public static String CRYPTO_DIR = "crypto-config/";
  public static String PEER_NAME_SUFFIX = "-peer";
  public static String EXTRA_HOSTS_PREFIX = "       - ";
  public static String CONTAINER_WORKING_DIR = "/etc/hyperledger/artifacts/";
  public static String DOCKER_CMD =
      "docker-compose -f ./gbaas/docker-compose.yaml run CLI bash -c \\\"COMMAND\\\"";
  public static String MSP_SUFFIX = "MSP";
  public static String PEER_CA_FILE = "peerOrganizations/ORG.DOMAIN/peers/peer0.ORG/tls/";
  public static String CRYPTO_FOLDER_FOR_COMPOSER =
      PROJECT_VM_DIR + CRYPTO_DIR + "peerOrganizations/ORG.DOMAIN/users/Admin@ORG/msp/";
  public static String ORG_PLACEHOLDER = "ORG";
  public static String DOMAIN_PLACEHOLDER = "DOMAIN";
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
  public static final int ORDERER_BASE_NODEPORT = 32000;
  public static final String NODEPORT_PLACEHOLDER = "NODEPORT";
  public static final String NODEPORT_PEER_PLACEHOLDER = "NODEPORT_PEER";
  public static final String NODEPORT_PEER_EVENT_PLACEHOLDER = "NODEPORT_EVENT";
  public static final String NODEPORT_PEER_CHAINCODE_PLACEHOLDER = "NODEPORT_CHAINCODE";
  public static final String PEER_CHAINCODE_PORT_PLACEHOLDER = "PEER_CHAINCODE_PORT";
  public static final String BUCKET_PLACEHOLDER = "GCS_BUCKET";
  public static final String GCP_PROJECT_NAME_PLACEHOLDER = "GCP_PROJECT_NAME";

  public static int NODEPORT_FACTOR = 30000;
  //which also limits the number of peers per org can have
  public static int NODEPORT_INTERVAL = 100;
  private final ObjectMapper mapper;
  private final AppConfiguration appConfiguration;
  public String cryptoGenCmd;
  private String workingDir;
  private String scriptFile;
  private String cryptoPath;
  private String composerPath;


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
    property1.setOrg("google");
    property1.setNumOfPeers(2);
    OrgConfig property2 = new OrgConfig();
    property2.setOrg("boa");
    property2.setNumOfPeers(2);
    NetworkConfig config = new NetworkConfig();
    config.setGcpProjectName("hyperledger-poc");
    config.setOrdererName("orderer-google-boa");
    config.setGcpZoneName("us-east1-b");
    config.setChannelName("common");
    config.setDomain("test");
    config.setStorageBucket("hyperledger-poc");
    config.setOrgConfigs(Lists.newArrayList(property1, property2));
    DeploymentService ds = new DeploymentService(mapper, appConfiguration);

    Map<String, List<String>> orgPeerMap = ds.deployFabric(config, false, false);

    //for testing
    //Map<String, Map<String, String>> orgNameIpMap = ds.getInstanceNameIPMap(config);
    //
    // ds.createComposerConnectionFile(orgNameIpMap, config);
    // ds.createComposerAdminCard(orgNameIpMap, config);
    ds.runScript();

  }


  public Map<String, List<String>> deployFabric(NetworkConfig config,
      boolean createInstance, boolean installSoftware) {
    initialEnvVariables(config);
    // if (createInstance) {
    //   createInstances(config);
    // }
    // Map<String, Map<String, String>> orgNameIpMap = getInstanceNameIPMap(config);
    // log.info("The instance list is " + orgNameIpMap);
    initService(config);
    Map<String, List<String>> orgPeerMap = getOrgPeerMap(config);
    // copyInstallScripts(orgNameIpMap, config);
    // copyInstallScripts(orgNameIpMap, config);
    // setupVm(orgNameIpMap, config);
    // if (installSoftware) {
    //   setupDocker(orgNameIpMap, config);
    //   setupComposer(orgNameIpMap, config);
    // }

    createCrypto(orgPeerMap, config);
    Map<String, String> orgPk = createCryptoFiles(orgPeerMap, config);
    createConfigtxYaml(orgPeerMap, config);
    createConfigtxFiles(orgPeerMap, config);
    copyChaincode(orgPeerMap, config);
    copyGeneratedFilesToStorage(orgPeerMap, config);
    // copyTxToStorage(config);

    String ordererYamlFile = createOrdererDeploymentYamlFiles(config);
    List<String> peerYamlFiles = createPeerDeploymentYamlFiles(config, orgPeerMap);
    createNamespaceDeploymentYamlFiles(config, orgPeerMap);
    // List<String> cliYamlFiles = createCliDeploymentYamlFiles(config, orgPeerMap);
    // createCaServerYaml(orgNameIpMap, config);

    // startOrderer(orgNameIpMap, config);

    // startContainers(orgNameIpMap, config);
    //  createChannelBlock(orgNameIpMap, config);
    //  distributeChannelBlock(orgNameIpMap, config);
    //  joinChannel(orgNameIpMap, config);
    //  updateAnchorPeer(orgNameIpMap, config);
    // if (installExampleChaincode) {
    //   packageChaincode(orgNameIpMap, config);
    //   distributeExampleChainCodePak(orgNameIpMap, config);
    //   installChaincode(orgNameIpMap, config);
    // }
    return orgPeerMap;
  }

  // public void deployComposer(NetworkConfig config,
  //     Map<String, Map<String, String>> orgNameIpMap) {
  //   initialEnvVariables(config);
  //   createScriptFile(config);
  //   createComposerConnectionFile(orgNameIpMap, config);
  //   createComposerAdminCard(orgNameIpMap, config);
  //
  // }

  private void initialEnvVariables(NetworkConfig config) {

    workingDir = appConfiguration.WORKING_DIR + config.getDomain() + "/";
    composerPath = workingDir + "composer/";
    cryptoGenCmd = "~/bin/cryptogen generate --output=" + workingDir + "crypto-config --config="
        + workingDir + "cryptogen.yaml";
    scriptFile = workingDir + "script.sh";
    cryptoPath = workingDir + CRYPTO_DIR;

  }

  public void initService(NetworkConfig config) {

    try {
      deleteFolders(workingDir);
      createDir(workingDir);
      createScriptFile(config);
      // Files.copy(Paths.get(Resources.getResource("template/base.yaml").toURI()),
      //     Paths.get(workingDir, "base.yaml"), StandardCopyOption.REPLACE_EXISTING);
      // Files.copy(Paths.get(Resources.getResource("template/init-docker.sh").toURI()),
      //     Paths.get(workingDir, "init-docker.sh"), StandardCopyOption.REPLACE_EXISTING);
      // Files.copy(Paths.get(Resources.getResource("template/setup.sh").toURI()),
      //     Paths.get(workingDir, "setup.sh"), StandardCopyOption.REPLACE_EXISTING);
      // Files.copy(Paths.get(Resources.getResource("template/install-composer.sh").toURI()),
      //     Paths.get(workingDir, "install-composer.sh"), StandardCopyOption.REPLACE_EXISTING);
    } catch (Throwable t) {
      throw new RuntimeException("Cannot init the service ", t);
    }

  }

  private void createScriptFile(NetworkConfig config) {
    try {
      Files.deleteIfExists(Paths.get(scriptFile));
      Files.write(Paths.get(scriptFile),
          ("export PATH=$PATH:" + appConfiguration.GCLOUD_DIR + "\n").getBytes(),
          StandardOpenOption.CREATE);
      appendToFile(scriptFile, SET_PROJECT + config.getGcpProjectName());

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
      List<String> peers = IntStream.range(0, num).mapToObj(n -> "peer" + n)
          .collect(toList());
      orgPeerMapBuilder.put(org, peers);
    }
    return orgPeerMapBuilder.build();
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
              .replaceAll("DOMAIN", config.getDomain()).getBytes(),
          StandardOpenOption.CREATE);
      //TODO: NOT SURE if this is needed
      //copyFileToGcpVm(path, "cryptogen.yaml", config.getOrdererName(), config);

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
       // String domain = String.join(".", org, config.getDomain());
        String path = cryptoPath + "peerOrganizations/" + org + "/ca/";
        Optional<String> skFile =
            Files.list(Paths.get(path)).map(f -> f.getFileName().toString())
                .filter(p -> p.endsWith("_sk"))
                //.map(f -> f.substring(0, f.indexOf('_')))
                .findFirst();
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

  public void copyChaincode(Map<String, List<String>> orgPeerMap,
      NetworkConfig config) {
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
    copyFolderToStorage(workingDir, config.getStorageBucket(), config.getDomain());

    // //orderer
    // try {
    //   appendToFile(scriptFile, "echo copying orderer crypto folder");
    //   String path = CRYPTO_DIR_ORDERER + config.getDomain();
    //   copyFolderToStorage(cryptoPath + path, config.getStorageBucket(), config.getDomain());
    //
    //   //copy ca of peers from other org
    //   //todo: fuse orderer path to peer
    //
    // } catch (Throwable t) {
    //   throw new RuntimeException(
    //       "Cannot write commands of distributing certs to orderer to the script file ", t);
    // }
    //
    // //peers
    // try {
    //   for (String org : orgs) {
    //     for (String instance : orgPeerMap.get(org)) {
    //
    //       appendToFile(scriptFile, "echo copying crypto folder to peer " + instance);
    //
    //       String orgDomain = org + "." + config.getDomain() + "/";
    //
    //       String path = CRYPTO_DIR_PEER + orgDomain;
    //
    //       copyFolderToStorage(cryptoPath + path, config.getStorageBucket(),
    //           config.getDomain());
    //
    //     }
    //   }
    //
    // } catch (Throwable t) {
    //   throw new RuntimeException(
    //       "Cannot write commands of distributing certs to peers to the script file ", t);
    // }
  }


  public void createConfigtxYaml(Map<String, List<String>> orgPeerMap,
      NetworkConfig config) {

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
      //TODO: NOT SURE if this is needed
      //copyFileToGcpVm(configtxfPath, "configtx.yaml", config.getOrdererName(), config);

    } catch (Throwable e) {
      throw new RuntimeException("Cannot create configtx yaml file ", e);
    }


  }

  public void createConfigtxFiles(Map<String, List<String>> orgPeerMap,
      NetworkConfig config) {

    try {
      String dir = workingDir + "channel/";
      deleteFolders(dir);
      createDir(dir);

      appendToFile(scriptFile, "export FABRIC_CFG_PATH=" + workingDir);
      log.info("Generating channel config transaction for %s channel",
          config.getChannelName());
      appendToFile(scriptFile, String
          .join("", "~/bin/configtxgen -profile OrdererGenesis -outputBlock ", dir,
              "genesis.block"));
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
      copyFolderToStorage(path, config.getStorageBucket(), config.getDomain());
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

  public String createOrdererDeploymentYamlFiles(
      NetworkConfig config) {
    log.info("Creating Orderer yaml file");

    try {
      String ordererTemplate = new String(Files.readAllBytes(Paths.get(
          Resources.getResource("k8/fabric_k8_template_orderer.yaml").toURI())));

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
      appendToFile(scriptFile, "echo copying file " + fileName);
      return fileName;
    } catch (Throwable e) {
      throw new RuntimeException("Cannot create orderer k8 yaml file ", e);
    }


  }

  public List<String> createPeerDeploymentYamlFiles(
      NetworkConfig config, Map<String, List<String>> orgPeersMap) {
    List<String> peersYamlFiles = Lists.newArrayList();
    log.info("Creating peers yaml files");

    try {
      String peerTemplate = new String(Files.readAllBytes(Paths
          .get(Resources.getResource("k8/fabric_k8_template_peer.yaml").toURI())));
      //creating peer k8 yaml files

      List<String> orgList = new ArrayList<>(orgPeersMap.keySet());

      for (int i = 0; i < orgList.size(); i++) {
        String org = orgList.get(i);
        List<String> peerList = orgPeersMap.get(org);
        for (int j = 0; j < peerList.size(); j++) {

          String peer = peerList.get(j);
          PeerNodePort peerNodePort = getPeerNodePort(i, j);
          String content =
              peerTemplate.replaceAll(PEER_PORT_PLACEHOLDER, appConfiguration.PEER_PORT)
                  .replaceAll(PEER_EVENT_PORT_PLACEHOLDER, appConfiguration.PEER_EVENT_PORT)
                  .replaceAll(PEER_CHAINCODE_PORT_PLACEHOLDER, appConfiguration.PEER_CHAINCODE_PORT)
                  .replaceAll(PEER_NAME_PLACEHOLDER, peer)
                  .replaceAll(NODEPORT_PEER_PLACEHOLDER, String.valueOf(peerNodePort.getPeerPort()))
                  //.replaceAll(NODEPORT_PEER_EVENT_PLACEHOLDER, String.valueOf(peerNodePort.getEventPort()))
                  .replaceAll(NODEPORT_PEER_CHAINCODE_PLACEHOLDER,
                      String.valueOf(peerNodePort.getChaincodePort()))
                  .replaceAll(DOMAIN_PLACEHOLDER, config.getDomain())
                  .replaceAll(ORG_PLACEHOLDER, org)
                  .replaceAll(BUCKET_PLACEHOLDER, config.getStorageBucket())
                  .replaceAll(COUCHDB_PORT_PLACEHOLDER, appConfiguration.COUDH_DB_PORT)
                  .replaceAll(GCP_PROJECT_NAME_PLACEHOLDER, config.getGcpProjectName())
                  .replaceAll(BUCKET_PLACEHOLDER, config.getStorageBucket());
          // .replaceAll(PEER_NAME_PLACEHOLDER, peer).replaceAll("CA_PRIVATE_KEY",
          //     orgDomainPkMap.get(org + "." + config.getDomain());

          String yamlFileName =
              String.join("", "fabric_k8_", org, "-", peer, ".yaml");
          String fileName = String.join("", workingDir, yamlFileName);
          Files.write(Paths.get(fileName), content.getBytes(), StandardOpenOption.CREATE);
          log.info(yamlFileName + " created.");
          peersYamlFiles.add(yamlFileName);
        }
      }
      return peersYamlFiles;
    } catch (Throwable e) {
      throw new RuntimeException("Cannot create peer k8 yaml file ", e);
    }
  }


  public void createNamespaceDeploymentYamlFiles(
      NetworkConfig config, Map<String, List<String>> orgPeersMap) {
    log.info("Creating namespace yaml files");

    try {
      String namespaceTemplate = new String(Files.readAllBytes(Paths.get(
          Resources.getResource("k8/fabric_k8_template_namespace.yaml").toURI())));

      Set<String> orgs = new HashSet<>(orgPeersMap.keySet());
      orgs.add(config.getDomain());

      for (String org : orgs) {

        //create orderer k8 yaml file
        String namespace = namespaceTemplate.replaceAll(ORG_PLACEHOLDER, org);

        String fileName = String.join("", workingDir, org, "_namespace.yaml");
        Files.write(Paths.get(fileName), namespace.getBytes(), StandardOpenOption.CREATE);
        appendToFile(scriptFile, "echo copying file " + fileName);
      }
    } catch (Throwable e) {
      throw new RuntimeException("Cannot create namespace k8 yaml file ", e);
    }


  }


  public List<String> createCliDeploymentYamlFiles(
      NetworkConfig config, Map<String, List<String>> orgPeersMap) {
    List<String> cliYamlFiles = Lists.newArrayList();
    log.info("Creating peers yaml files");

    try {
      String peerTemplate = new String(Files.readAllBytes(Paths
          .get(Resources.getResource("k8/fabric_k8_template_cli.yaml").toURI())));

      for (String org : orgPeersMap.keySet()) {
        for (String peer : orgPeersMap.get(org)) {

          String content =
              peerTemplate
                  .replaceAll(PEER_NAME_PLACEHOLDER, peer)
                  .replaceAll(DOMAIN_PLACEHOLDER, config.getDomain())
                  .replaceAll(ORG_PLACEHOLDER, org);
          // .replaceAll(PEER_NAME_PLACEHOLDER, peer).replaceAll("CA_PRIVATE_KEY",
          //     orgDomainPkMap.get(org + "." + config.getDomain());

          String yamlFileName =
              String.join("", "fabric_k8_", peer, "_cli.yaml");
          String fileName = String.join("", workingDir, yamlFileName);
          Files.write(Paths.get(fileName), content.getBytes(), StandardOpenOption.CREATE);
          log.info(yamlFileName + " created.");
          cliYamlFiles.add(yamlFileName);
        }
      }
      return cliYamlFiles;
    } catch (Throwable e) {
      throw new RuntimeException("Cannot create peer k8 yaml file ", e);
    }
  }

  public void createChannelBlock(Map<String, Map<String, String>> orgNameIpMap,
      NetworkConfig config) {
    log.info("calling createChannelBlock method");

    String org = orgNameIpMap.keySet().stream().filter(o -> !o.equals(config.getOrdererName()))
        .findFirst().get();
    String instance = (String) orgNameIpMap.get(org).keySet().toArray()[0];
    //todo: make channel name a variable
    //String domain = String.join(".", org, config.getDomain());

    String peerCmd = String
        .join("", "peer channel create -o orderer.", config.getDomain(), ":",
            appConfiguration.ORDERER_PORT, " -c ", config.getChannelName(), " -f ",
            CONTAINER_WORKING_DIR, "channel/", config.getChannelName(), ".tx --tls --cafile ",
            ORDERER_CA_IN_CONTAINER);

    String dockerCmd = DOCKER_CMD.replace("CLI", "cli." + org).replace("COMMAND", peerCmd);
    String cmd = String
        .join("", SSH, instance, " --zone ", config.getGcpZoneName(), " --command \"",
            dockerCmd, "\"");

    appendToFile(scriptFile, cmd);
    appendToFile(scriptFile, "echo " + config.getChannelName() + ".block created");
    appendToFile(scriptFile, "sleep 2");

    dockerCmd = String.join("", "docker cp ", "cli." + org, ":", CONTAINER_WORKING_DIR,
        config.getChannelName(), ".block ~/");
    cmd = String.join("", SSH, instance, " --zone ", config.getGcpZoneName(), " --command \"",
        dockerCmd, "\"");
    appendToFile(scriptFile, cmd);

    appendToFile(scriptFile, "sleep 2");
    appendToFile(scriptFile, String.join("", SSH, instance, " --zone ", config.getGcpZoneName(),
        " --command \" chmod u+x ~/", config.getChannelName(), ".block\""));

    appendToFile(scriptFile,
        "echo " + config.getChannelName() + ".block copied from container");

    //todo: make channel name a variable
    copyVmFileToGCP(config.getChannelName() + ".block", workingDir, instance, config);
    appendToFile(scriptFile, "echo " + config.getChannelName() + ".block copied to server");

  }

  public void distributeChannelBlock(Map<String, Map<String, String>> orgNameIpMap,
      NetworkConfig config
      // , String instance
  ) {
    log.info("calling distributeChannelBlock method");

    orgNameIpMap.values().stream().map(Map::keySet).forEach(ins -> ins.stream().forEach(vm -> {
      appendToFile(scriptFile, "echo senting " + config.getChannelName() + ".block to " + vm);

      copyFileToGcpVm(workingDir + config.getChannelName() + ".block",
          PROJECT_VM_DIR + config.getChannelName() + ".block", vm, config);
    }));

  }

  public void copyChannelBlockToContainer(Map<String, Map<String, String>> orgNameIpMap,
      NetworkConfig config) {
    log.info("calling copyChannelBlockToContainer method");

    Set<String> orgs = Sets.newHashSet(orgNameIpMap.keySet());
    orgs.remove(config.getOrdererName());
    for (String org : orgs) {
      //String domain = String.join(".", org, config.getDomain());

      for (String peer : orgNameIpMap.get(org).keySet()) {
        String dockerCmd = String
            .join("", "docker cp ~/", config.getChannelName(), ".block", " cli." + org,
                ":", CONTAINER_WORKING_DIR);
        String cmd = String
            .join("", SSH, peer, " --zone ", config.getGcpZoneName(), " --command \"",
                dockerCmd, "\"");

        appendToFile(scriptFile, cmd);
      }
    }

  }

  public void joinChannel(Map<String, Map<String, String>> orgNameIpMap, NetworkConfig config) {
    appendToFile(scriptFile, "echo Joining channel...");

    Set<String> orgs = Sets.newHashSet(orgNameIpMap.keySet());
    orgs.remove(config.getOrdererName());
    for (String org : orgs) {
      String domain = String.join(".", org, config.getDomain());

      for (String peer : orgNameIpMap.get(org).keySet()) {
        String peerCmd = "peer channel join -b " + config.getChannelName() + ".block";
        String dockerCmd =
            DOCKER_CMD.replace("CLI", "cli." + domain).replace("COMMAND", peerCmd);
        String cmd = String
            .join("", SSH, peer, " --zone ", config.getGcpZoneName(), " --command \"",
                dockerCmd, "\"");

        appendToFile(scriptFile, cmd);
      }
    }


  }

  public void updateAnchorPeer(Map<String, Map<String, String>> orgNameIpMap,
      NetworkConfig config) {
    log.info("Updating anchorPeer...");
    appendToFile(scriptFile, "echo Updating anchorPeer...");

    Set<String> orgs = Sets.newHashSet(orgNameIpMap.keySet());
    orgs.remove(config.getOrdererName());
    for (String org : orgs) {
   //   String domain = String.join(".", org, config.getDomain());

      for (String peer : orgNameIpMap.get(org).keySet()) {
        if (!peer.contains("peer0")) {
          continue;
        }
        String updateAnchorCmd =
            ANCHORPEER_CMD.replace("ORDERER_HOST", "orderer." + config.getDomain())
                .replace("CHANNEL_NAME", config.getChannelName())
                .replace("ORDERER_PORT", appConfiguration.ORDERER_PORT)
                .replace("ANCHOR_FILE", "channel/" + org + MSP_SUFFIX + ANCHOR_FILE_SUFFIX)
                .replace("ORDERER_CA", ORDERER_CA_IN_CONTAINER);
        String dockerCmd =
            DOCKER_CMD.replace("CLI", "cli." + org).replace("COMMAND", updateAnchorCmd);
        String cmd = String
            .join("", SSH, peer, " --zone ", config.getGcpZoneName(), " --command \"",
                dockerCmd, "\"");

        appendToFile(scriptFile, cmd);
      }
    }

  }

  public void installChaincode(Map<String, Map<String, String>> orgNameIpMap,
      NetworkConfig config) {

    Set<String> orgs = Sets.newHashSet(orgNameIpMap.keySet());
    orgs.remove(config.getOrdererName());
    for (String org : orgs) {
  //    String domain = String.join("-", org, config.getDomain());

      for (String peer : orgNameIpMap.get(org).keySet()) {
        String peerCmd = "peer chaincode install -n test -v 1.0 -p chaincode_example02 ";
        String dockerCmd =
            DOCKER_CMD.replace("CLI", "cli." + org).replace("COMMAND", peerCmd);
        String cmd = String
            .join("", SSH, peer, " --zone ", config.getGcpZoneName(), " --command \"",
                dockerCmd, "\"");

        appendToFile(scriptFile, cmd);
      }
    }

  }

  // public void createComposerConnectionFile(Map<String, Map<String, String>> orgNameIpMap,
  //     NetworkConfig config) {
  //
  //   try {
  //     deleteFolders(composerPath);
  //     createDir(composerPath);
  //     String connectionTemplate = new String(Files.readAllBytes(
  //         Paths.get(Resources.getResource("template/connection-template.json").toURI())));
  //
  //     String orderer = String.join("", "orderer.", config.getDomain());
  //     String ordererIp =
  //         orgNameIpMap.get(config.getOrdererName()).get(config.getOrdererName());
  //     Set<String> orgs = Sets.newHashSet(orgNameIpMap.keySet());
  //     orgs.remove(config.getOrdererName());
  //
  //     Map<String, List<String>> OrghostsListMap = new HashMap<>(orgs.size());
  //     Map<String, String> hostsIpMap = new HashMap<>();
  //     Map<String, String> hostOrgMap = new HashMap<>();
  //     for (String org : orgs) {
  //
  //       Map<String, String> hostIpMapEntry = orgNameIpMap.get(org);
  //       hostsIpMap.putAll(hostIpMapEntry);
  //
  //       hostIpMapEntry.entrySet().stream().forEach(e -> hostOrgMap.put(e.getKey(), org));
  //
  //       OrghostsListMap.put(org, hostIpMapEntry.entrySet().stream().map(e -> String
  //           .join("", e.getKey().split("-")[1], ".", org, ".", config.getDomain()))
  //           .collect(Collectors.toList()));
  //
  //
  //     }
  //
  //     String peerChannelConfigs = hostsIpMap.keySet().stream().map(host -> {
  //
  //       try {
  //         String peerChannelConfigJson =
  //             mapper.writeValueAsString(new ChannelPeerConfig(true, true, true));
  //
  //         return String.join("", "\"", String
  //             .join(".", host.split("-")[1], hostOrgMap.get(host),
  //                 config.getDomain()), "\":", peerChannelConfigJson);
  //       } catch (JsonProcessingException e) {
  //         throw new RuntimeException(e);
  //       }
  //
  //     }).collect(joining("," + System.lineSeparator()));
  //
  //     String orgConfigs = OrghostsListMap.keySet().stream().map(org -> {
  //
  //       try {
  //
  //         String orgConfigJson = mapper.writeValueAsString(
  //             new OrgConfig(org + MSP_SUFFIX, OrghostsListMap.get(org), Lists
  //                 .newArrayList(String.join(".", "ca", org, config.getDomain()))));
  //
  //         return String.join("", "\"", org, "\":", orgConfigJson);
  //       } catch (JsonProcessingException e) {
  //         throw new RuntimeException(e);
  //       }
  //
  //
  //     }).collect(joining("," + System.lineSeparator()));
  //
  //     OrdererConfig ordererConfig =
  //         new OrdererConfig("grpcs://" + ordererIp + ":" + appConfiguration.ORDERER_PORT,
  //             new GrpcOptions(orderer), new TlsCACerts(readOrdererCaCert()));
  //
  //     String ordererConfigJson =
  //         "\"" + orderer + "\":" + mapper.writeValueAsString(ordererConfig);
  //
  //     String peersConfigJson = hostsIpMap.entrySet().stream().map(e -> {
  //       try {
  //         String host = e.getKey();
  //         // String pemFile = PEER_CA_FILE.replaceAll(ORG_PLACEHOLDER, hostOrgMap.get(host))
  //         //     .replaceAll(DOMAIN_PLACEHOLDER, config.getDomain());
  //         // System.out.println("pemFile = " + pemFile);
  //         PeerConfig peerConfig = new PeerConfig(
  //             "grpcs://" + hostsIpMap.get(host) + ":" + appConfiguration.PEER_PORT,
  //             "grpcs://" + hostsIpMap.get(host) + ":" + appConfiguration.PEER_EVENT_PORT,
  //             new GrpcOptions(e.getKey()),
  //             new TlsCACerts(readPeerCaCert(hostOrgMap.get(host))));
  //         String configJson = mapper.writeValueAsString(peerConfig);
  //
  //         return String.join("", "\"", String
  //             .join(".", host.split("-")[1], hostOrgMap.get(host),
  //                 config.getDomain()), "\":", configJson);
  //
  //
  //       } catch (Throwable t) {
  //         throw new RuntimeException(t);
  //       }
  //
  //     }).collect(joining("," + System.lineSeparator()));
  //
  //     //todo: need to modularize this CA code
  //
  //     String caConfigJson = orgs.stream().map(o -> {
  //
  //       //todo:  hard code for now
  //       String caRealHost = String.join("-", o, "peer0");
  //       String caHost = String.join(".", "ca", o, config.getDomain());
  //       CaConfig caConfig = new CaConfig(
  //           "https://" + hostsIpMap.get(caRealHost) + ":" + appConfiguration.CA_PORT,
  //           caHost, new HttpOptions(false));
  //       try {
  //         return String
  //             .join("", "\"", caHost, "\"", ":", mapper.writeValueAsString(caConfig));
  //       } catch (JsonProcessingException e) {
  //         throw new RuntimeException(e);
  //       }
  //
  //     }).collect(joining("," + System.lineSeparator()));
  //
  //     String networkName = String.join("-", orgs) + "-network";
  //
  //     for (String org : orgs) {
  //
  //       String content = connectionTemplate.replace("CONNECTION_NAME", networkName)
  //           .replaceAll("ORG_NAME", org).replace("ORDERER_NAMES", "\"" + orderer + "\"")
  //           .replace("PEERS_CHANNEL_INFO", peerChannelConfigs)
  //           .replace("ORGS_CONFIG", orgConfigs).replace("ORDERER_CONFIG", ordererConfigJson)
  //           .replace("PEERS_CONFIG", peersConfigJson).replace("CA_CONFIG", caConfigJson)
  //           .replace("CHANNEL_NAME", config.getChannelName());
  //
  //       String fileName = String.join("", composerPath,
  //           CONNECTION_FILE_NAME_TEMPLATE.replace("NETWORKNAME", networkName)
  //               .replace("ORG", org), ".json");
  //       log.info("fileName = " + fileName);
  //       Files.write(Paths.get(fileName), content.getBytes(), StandardOpenOption.CREATE);
  //       String host = String.join("-", org, "peer0");
  //       appendToFile(scriptFile, "echo copying connection.json file to " + host);
  //       copyFileToGcpVm(fileName, appConfiguration.COMPOSER_CONNECTION_FILE + ".json", host,
  //           config);
  //
  //     }
  //
  //   } catch (Throwable e) {
  //     throw new RuntimeException("Cannot create composer connection json file ", e);
  //   }
  //
  //
  // }

  // public String readOrdererCaCert() throws IOException {
  //
  //   String file =
  //       cryptoPath + ORDERER_CA_FILE.replaceAll(DOMAIN_PLACEHOLDER, config.getDomain())
  //           + "ca.crt";
  //   return readCaCert(file);
  // }
  //
  // public String readPeerCaCert(String org) throws IOException {
  //
  //   String file =
  //       cryptoPath + PEER_CA_FILE.replaceAll(DOMAIN_PLACEHOLDER, config.getDomain())
  //           .replaceAll(ORG_PLACEHOLDER, org) + "ca.crt";
  //   return readCaCert(file);
  //
  //
  // }

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
      CommandRunner.runCommand(Lists.newArrayList(workingDir + "script.sh"), log);

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

  public void copyFolderToStorage(String sourceFolder, String bucket, String domain) {

    try {
      appendToFile(scriptFile, String
          .format(COPY_TO_STORAGE_CMD, sourceFolder, bucket + "/" + domain));
    } catch (Throwable t) {

      throw new CommandFailedToRunException(
          "Cannot copy " + sourceFolder + " to bucket" + bucket, t);
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
        calculatePeerNodePort(orgIndex, peerIndex, 2));
    //calculatePeerNodePort(orgIndex, peerIndex, 3));

  }

  private int calculatePeerNodePort(int orgIndex, int peerIndex, int type) {
    return NODEPORT_FACTOR + orgIndex * NODEPORT_INTERVAL + 2 * peerIndex + type;
  }

  private int getOrdererNodePort(int ordererIndex) {

    return ORDERER_BASE_NODEPORT + ordererIndex;
  }


}
