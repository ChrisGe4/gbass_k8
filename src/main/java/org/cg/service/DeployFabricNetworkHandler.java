package org.cg.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.cg.core.AppConfiguration;
import org.cg.pojo.NetworkConfig;
import org.cg.pojo.OrgConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @author Chris.Ge
 */

@RestController
@RequestMapping("/gbaas")
public class DeployFabricNetworkHandler {

  private static final Logger log = LoggerFactory.getLogger(DeployFabricNetworkHandler.class);
  private final AppConfiguration properties;
  private final DeploymentService service;
  private final ObjectMapper objectMapper;

  @Autowired
  public DeployFabricNetworkHandler(AppConfiguration properties, DeploymentService service,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.service = service;
    this.objectMapper = objectMapper;
  }

  @PostMapping(value = "/fabric", headers = "Accept=application/json")
  public String deployFabricNetwork(
      @RequestParam(value = "delete_deployment", defaultValue = "false", required = false)
          Boolean deleteDeployment,
      @RequestParam(value = "delete_service", defaultValue = "false", required = false)
          Boolean deleteService,
      @RequestParam(value = "delete_pvc", defaultValue = "false", required = false)
          Boolean deletePVC,
      @RequestParam(value = "delete_namespace", defaultValue = "false", required = false)
          Boolean deleteNameSpace,
      @RequestBody NetworkConfig config) {

    service.deployFabric(config, deleteDeployment, deleteService, deletePVC, deleteNameSpace);

    service.runScript();
    return "succeed";
  }


  @PostMapping(value = "/fabric/delete_deployments", headers = "Accept=application/json")
  public String deleteDeployment(
      @RequestParam(value = "namespace")
          List<String> namespaces,
      @RequestParam(value = "project_name")
          String projectName
  ) {
    service.deleteDeployments(namespaces, projectName);
    service.runScript();
    return "succeed";
  }

  @PostMapping(value = "/composer", headers = "Accept=application/json")
  public String deployComposer(@RequestBody NetworkConfig config) {
    //  service.deployComposer(config, service.getInstanceNameIPMap(config));
    service.runScript();
    return "succeed";
  }

  @GetMapping("/sample")
  public String sampleConfig() throws JsonProcessingException {

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

    return objectMapper.writeValueAsString(config);
  }


}
