package org.cg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.net.URISyntaxException;
import org.cg.auth.GCPAuthClient;
import org.cg.config.AppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

public class K8DeploymentService {

  private final ObjectMapper mapper;
  private final AppConfiguration appConfiguration;

  @Autowired
  public K8DeploymentService(ObjectMapper mapper, AppConfiguration appConfiguration) {
    this.mapper = mapper;
    this.appConfiguration = appConfiguration;

  }




  public static void main(String[] args) throws IOException, URISyntaxException {

    GCPAuthClient gcpAuthClient = new GCPAuthClient();
    GoogleCredentials googleCredential = gcpAuthClient.getGoogleCredential();

    Storage storage = StorageOptions.newBuilder().setCredentials(googleCredential).build()
        .getService();

    // List all your buckets
    System.out.println("My buckets:");
    for (Bucket bucket : storage.list().iterateAll()) {
      System.out.println(bucket);

      // List all blobs in the bucket
      System.out.println("Blobs in the bucket:");
      for (Blob blob : bucket.list().iterateAll()) {
        System.out.println(blob);
      }
    }


  }


}
