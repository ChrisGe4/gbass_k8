package org.cg.pojo;

public class PeerNodePort {

  private final int peerPort;
  private final int eventPort;
  private final int chaincodePort;

  public PeerNodePort(int peerPort, int eventPort, int chaincodePort) {
    this.peerPort = peerPort;
    this.eventPort = eventPort;
    this.chaincodePort = chaincodePort;
  }


  public int getPeerPort() {
    return peerPort;
  }

  public int getChaincodePort() {
    return chaincodePort;
  }

  public int getEventPort() {
    return eventPort;
  }
}
