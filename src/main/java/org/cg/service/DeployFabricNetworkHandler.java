package org.cg.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.cg.config.AppConfiguration;
import org.cg.pojo.NetworkConfig;
import org.cg.pojo.OrgConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


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
        @RequestParam(value = "create_instance", defaultValue = "false", required = false)
            Boolean createInstance,
        @RequestParam(value = "install_software", defaultValue = "false", required = false)
            Boolean installSoftware,
        @RequestBody NetworkConfig config) {

        service.deployFabric(config, createInstance, installSoftware);

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
        config.setOrgConfigs(Lists.newArrayList(property1, property2));

        return objectMapper.writeValueAsString(config);
    }




}
