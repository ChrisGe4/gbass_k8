
package org.cg.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "domain",
    "gcpProjectName",
    "gcpZoneName",
    "ordererName",
    "channelName",
    "storageBucket",
    "nfsIp",
    "nfsNamespace",
    "useGcs",
    "orgConfigs"
})
public class NetworkConfig {

  @JsonProperty("domain")
  private String domain;
  @JsonProperty("gcpProjectName")
  private String gcpProjectName;
  @JsonProperty("gcpZoneName")
  private String gcpZoneName;
  @JsonProperty("ordererName")
  private String ordererName;
  @JsonProperty("channelName")
  private String channelName;
  @JsonProperty("storageBucket")
  private String storageBucket;
  @JsonProperty("nfsIp")
  private String nfsIp;
  @JsonProperty(value = "nfsNamespace" ,defaultValue = "default")
  private String nfsNamespace;
  @JsonProperty(value = "useGcs", defaultValue = "false")
  private boolean useGcs;
  @JsonProperty("orgConfigs")
  private List<OrgConfig> orgConfigs = null;

  @JsonProperty("domain")
  public String getDomain() {
    return domain;
  }

  @JsonProperty("domain")
  public void setDomain(String domain) {
    this.domain = domain;
  }


  @JsonProperty("gcpProjectName")
  public String getGcpProjectName() {
    return gcpProjectName;
  }

  @JsonProperty("gcpProjectName")
  public void setGcpProjectName(String gcpProjectName) {
    this.gcpProjectName = gcpProjectName;
  }

  @JsonProperty("gcpZoneName")
  public String getGcpZoneName() {
    return gcpZoneName;
  }

  @JsonProperty("gcpZoneName")
  public void setGcpZoneName(String gcpZoneName) {
    this.gcpZoneName = gcpZoneName;
  }

  @JsonProperty("ordererName")
  public String getOrdererName() {
    return ordererName;
  }

  @JsonProperty("ordererName")
  public void setOrdererName(String ordererName) {
    this.ordererName = ordererName;
  }

  @JsonProperty("channelName")
  public String getChannelName() {
    return channelName;
  }

  @JsonProperty("channelName")
  public void setChannelName(String channelName) {
    this.channelName = channelName;
  }

  @JsonProperty("storageBucket")
  public String getStorageBucket() {
    return storageBucket;
  }

  @JsonProperty("storageBucket")
  public void setStorageBucket(String storageBucket) {
    this.storageBucket = storageBucket;
  }

  @JsonProperty("orgConfigs")
  public List<OrgConfig> getOrgConfigs() {
    return orgConfigs;
  }

  @JsonProperty("orgConfigs")
  public void setOrgConfigs(List<OrgConfig> orgConfigs) {
    this.orgConfigs = orgConfigs;
  }

  @JsonProperty("nfsIp")
  public String getNfsIp() {
    return nfsIp;
  }

  @JsonProperty("nfsIp")
  public void setNfsIp(String nfsIp) {
    this.nfsIp = nfsIp;
  }

  @JsonProperty("nfsNamespace")
  public String getNfsNamespace() {
    return nfsNamespace;
  }
  @JsonProperty("nfsNamespace")
  public void setNfsNamespace(String nfsNamespace) {
    this.nfsNamespace = nfsNamespace;
  }

  @JsonProperty("useGcs")
  public boolean getUseGcs() {
    return useGcs;
  }

  @JsonProperty("useGcs")
  public void setUseGcs(boolean useGcs) {
    this.useGcs = useGcs;
  }
}
