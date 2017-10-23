/**
 * Copyright 2017 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.g11n.pipeline.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.ibm.g11n.pipeline.client.ServiceAccount;
import com.ibm.g11n.pipeline.client.ServiceClient;

/***
 * The root class of Watson Conversation Globalization.
 * 
 * @author Harpreet Chawla (hchawla@us.ibm.com)
 *
 */
abstract class BaseUtility {

  @Parameter(names = { "-s",
      "--sourceworkspaceId" }, description = "Watson Conversation Source Workspace ID", required = true)
  private String sourceworkspaceId;

  @Parameter(names = { "-v", "--versionDate" }, description = "Watson Conversation API version Date", required = true)
  private String versionDate;

  @Parameter(names = { "-b", "--bundleId" }, description = "Bundle Id Prefix for Globalization Pipeline")
  private String bundleId;

  @Parameter(names = { "-j", "--jsonWCSCreds" }, description = "Watson Conversation Credentials file", required = true)
  private String jsonWCSCreds;

  @Parameter(names = { "-g",
      "--jsonGPCreds" }, description = "Globalization Pipeline Credentials file", required = true)
  private String jsonGPCreds;

  @Parameter(names = { "-t", "--targetLanguage" }, description = "Target Language for WCS")
  private String targetLanguage;

  @Parameter(names = { "-w", "--targetworkspaceID" }, description = "Target Workspace ID for WCS")
  private String targetworkspaceID;

  private int splitSize;

  protected abstract void _execute() throws WCSWorkspaceException, Exception;

  // class to fetch GP credentials
  static class JsonGPCredentials {
    String url;
    String instanceId;
    String userId;
    String password;
  }

  // class to fetch WCS credentials
  static class JsonWCSCredentials {
    String username;
    String password;
  }

  public void execute() throws Exception {
    try {
      _execute();
    } catch ( WCSWorkspaceException e){
      e.printStackTrace();
      System.exit(1);
    }
  }

  protected ServiceClient getGPClient() {
    String url = null;
    String instanceId = null;
    String userId = null;
    String password = null;

    if (jsonGPCreds != null) {
      JsonGPCredentials creds;
      try {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonGPCreds), StandardCharsets.UTF_8);
        Gson gson = new Gson();
        creds = gson.fromJson(reader, JsonGPCredentials.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      url = creds.url;
      instanceId = creds.instanceId;
      userId = creds.userId;
      password = creds.password;

      if (url == null || url.isEmpty() || password == null || password.isEmpty() || instanceId == null
          || instanceId.isEmpty() || userId == null || userId.isEmpty()) {
        System.out.println("Please provide Credentials for Globalization Pipeline Service");
        System.exit(1);
      }

    }
    ServiceAccount account = ServiceAccount.getInstance(url, instanceId, userId, password);
    return ServiceClient.getInstance(account);
  }

  protected String getWCSCreds() {

    String username = null;
    String password = null;
    if (jsonWCSCreds != null) {
      JsonWCSCredentials creds;
      try {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonWCSCreds), StandardCharsets.UTF_8);
        Gson gson = new Gson();
        creds = gson.fromJson(reader, JsonWCSCredentials.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      username = creds.username;
      password = creds.password;

      if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
        System.out.println("Please provide Credentials for Watson Conversation Service");
        System.exit(1);
      }
    }
    String creds = username + ":" + password;
    return creds;
  }

  public String getSourceworkspaceId() {
    return sourceworkspaceId;
  }

  public void setSourceworkspaceId(String sourceworkspaceId) {
    this.sourceworkspaceId = sourceworkspaceId;
  }

  public String getVersionDate() {
    return versionDate;
  }

  public void setVersionDate(String versionDate) {
    this.versionDate = versionDate;
  }

  public String getBundleId() {
    return bundleId;
  }

  public void setBundleId(String bundleId) {
    this.bundleId = bundleId;
  }

  public String getTargetLanguage() {
    return targetLanguage;
  }

  public void setTargetLanguage(String targetLanguage) {
    this.targetLanguage = targetLanguage;
  }

  public String getTargetworkspaceID() {
    return targetworkspaceID;
  }

  public void setTargetworkspaceID(String targetworkspaceID) {
    this.targetworkspaceID = targetworkspaceID;
  }

  public int getSplitSize() {
    return splitSize;
  }

  public void setSplitSize(int splitSize) {
    this.splitSize = splitSize;
  }

}
