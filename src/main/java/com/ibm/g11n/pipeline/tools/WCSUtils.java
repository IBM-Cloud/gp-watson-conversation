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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/***
 * Utils class to GET or POST Watson Conversation Workspace
 * 
 * @author Harpreet Chawla (hchawla@us.ibm.com)
 *
 */
final class WCSUtils {

  public final static int READ_TIMEOUT = 10000;
  public final static int CONNECT_TIMEOUT = 15000;

  /***
   * GET Call to get workspace from WCS
   * 
   * @param wcsCreds
   * @param workspaceId
   * @param versionDate
   * @return
   */
  public static JsonObject getWCSWorkspace(String wcsCreds, String workspaceId, String versionDate) {
    // WCS API URL
    String CONVERSATION_API_URL = "https://watson-api-explorer.mybluemix.net/conversation/api/v1/workspaces/%s?version=%s&export=true";

    // Encode username and Password to Base64 using Basic Auth for WCS API
    String authorizationHeader = "Basic "
        + Base64.getEncoder().encodeToString((wcsCreds).getBytes(StandardCharsets.UTF_8));

    String urlStr = String.format(CONVERSATION_API_URL, workspaceId, versionDate);
    System.out.println("GET " + urlStr);
    BufferedReader in = null;
    JsonObject jsonResponseObject = new JsonObject();

    try {
      URL targetWCSUrl = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection) targetWCSUrl.openConnection();
      conn.setRequestMethod("GET");
      conn.setReadTimeout(READ_TIMEOUT);
      conn.setConnectTimeout(CONNECT_TIMEOUT);
      conn.setRequestProperty("Authorization", authorizationHeader);

      // receiving response
      int responseCode = conn.getResponseCode();
      System.out.println("Response Code : " + responseCode);

      if (responseCode == 200) {
        in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();

        // parse and store as JSON
        JsonParser responseBodyParser = new JsonParser();
        jsonResponseObject = (JsonObject) responseBodyParser.parse(response.toString());

      } else {
        System.out.println("\n\n");
        System.out.println("Failed to communicate to WCS API endpoint " + responseCode);
        throw new WCSWorkspaceException("Please check your WCS Credentials and Command Line Arguments");
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // close the reader
      if (in != null) {
        try {
          in.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    }
    return jsonResponseObject;
  }

  /***
   * POST New workspace on WCS or update existing one
   * 
   * @param targetWorkspaceId
   * @param authorizationHeader
   * @param versionDate
   * @param jsonWCSPayload
   * @throws IOException 
   */
  public static void putWCSWorkspace(String targetWorkspaceId, String authorizationHeader, String versionDate,
      JsonObject jsonWCSPayload) throws IOException {

    String POST_WCS_API_URL = "https://watson-api-explorer.mybluemix.net/conversation/api/v1/workspaces?version=%s";
    String UPDATE_WCS_API_URL = "https://watson-api-explorer.mybluemix.net/conversation/api/v1/workspaces/%s?version=%s";

    String urlStr = null;
    if (targetWorkspaceId == null) {
      // Create new workspace on WCS
      urlStr = String.format(POST_WCS_API_URL, versionDate);
      System.out.println("\n \n*** POST new WCS Workspace ***");
    } else {
      // update already existing WCS workspace
      urlStr = String.format(UPDATE_WCS_API_URL, targetWorkspaceId, versionDate);
      System.out.println("\n \n*** UPDATE existing WCS Workspace ***");
    }

    System.out.println("POST " + urlStr);
    BufferedReader br = null;
    try {
      URL targetWCSUrl = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection) targetWCSUrl.openConnection();
      conn.setReadTimeout(READ_TIMEOUT);
      conn.setConnectTimeout(CONNECT_TIMEOUT);
      conn.setDoOutput(true);
      conn.setDoInput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Authorization", authorizationHeader);
      conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

      String jsonPayload = jsonWCSPayload.toString();
      OutputStream os = conn.getOutputStream();
      os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
      os.flush();
      os.close();

      int resCode = conn.getResponseCode();
      System.out.println("Response Code: " + resCode);
      if (targetWorkspaceId == null) {
        if (resCode == 201) {
          br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
          String output;
          System.out.println("\n Output from WCS .... \n");
          while ((output = br.readLine()) != null) {
            System.out.println(output);
          }
        } else {
          System.out.println("\n");
          System.out.println("Failed to communicate to WCS API endpoint " + resCode);
          throw new WCSWorkspaceException("Please check your WCS Credentials and JSON Payload");
        }
      } else {
        if (resCode == 200) {
          br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
          String output;
          System.out.println("\n Output from WCS .... \n");
          while ((output = br.readLine()) != null) {
            System.out.println(output);
          }
        } else {
          System.out.println("\n");
          System.out.println("Failed to communicate to WCS API endpoint " + resCode);
          throw new WCSWorkspaceException("Please check your WCS Credentials and JSON Payload");
        }
      }
    } catch (WCSWorkspaceException e) {
      e.printStackTrace();
    } finally {
      // close the reader
      if (br != null) {
        try {
          br.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    }
  }
}
