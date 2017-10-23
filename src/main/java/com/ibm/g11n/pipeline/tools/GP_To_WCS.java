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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.ibm.g11n.pipeline.client.ServiceException;

/***
 * Globalization Pipeline to Watson Conversation Utility
 * 
 * @author Harpreet Chawla (hchawla@us.ibm.com)
 *
 */
public class GP_To_WCS extends BaseUtility {

  @Override
  protected void _execute() throws Exception {

    // Parse Command Line Params
    String sourceWorkspaceId = getSourceworkspaceId();
    String targetLanguage = getTargetLanguage();
    String bundleIdPrefix = getBundleId();

    if (targetLanguage != null) {
      String[] langs = targetLanguage.split(",");
      if (langs.length > 1) {
        throw new WCSWorkspaceException("\n Please provide only one target language for GP_To_WCS Utility");
      }
    } else {
      throw new WCSWorkspaceException("Please provide at least one target language for GP_To_WCS Utility");
    }
    // Get Workspace in JSON format from WCS API
    JsonObject jsonWCSPayload = getConvWorkspace();

    if (jsonWCSPayload.size() > 0) {
      final String intents = "-intents";
      final String entities = "-entities";
      final String dialogs = "-dialogs";

      // Fetch Bundles from GP
      String intentsBundleName = null;
      String entitiesBundleName = null;
      String dialogsBundleName = null;

      if (bundleIdPrefix != null) {
        intentsBundleName = bundleIdPrefix + intents;
        intentsBundleName = bundleIdPrefix + entities;
        intentsBundleName = bundleIdPrefix + dialogs;

      } else {
        intentsBundleName = sourceWorkspaceId + intents;
        entitiesBundleName = sourceWorkspaceId + entities;
        dialogsBundleName = sourceWorkspaceId + dialogs;
      }

      FileWriter fw = new FileWriter("gp_to_wcs.log");
      BufferedWriter bw = new BufferedWriter(fw);

      // Get Intent Resource Strings
      Map<String, String> intentsBundle = checkExistingBundles(intentsBundleName, targetLanguage);
      if (intentsBundle == null || intentsBundle.isEmpty()) {
        System.out.println("  **No Intents Bundles Exist on GP....");
      } else {
        // update Intents in source conversation workspace with translated
        // content
        bw.write("*********************************************************\n");
        bw.write("************************ INTENTS ************************\n");
        bw.write("*********************************************************\n");
        bw.write("\n");
        bw.flush();
        updateIntents(jsonWCSPayload, intentsBundle, bw);
      }

      // Get Entity Resource Strings
      Map<String, String> entitiesBundle = checkExistingBundles(entitiesBundleName, targetLanguage);
      if (entitiesBundle == null || entitiesBundle.isEmpty()) {
        System.out.println("  **No Entities Bundles Exist on GP....");
      } else {
        // update Entities in source conversation workspace with translated
        // content
        bw.write("*********************************************************\n");
        bw.write("************************ ENTITIES ***********************\n");
        bw.write("*********************************************************\n");
        bw.write("\n");
        bw.flush();
        updateEntities(jsonWCSPayload, entitiesBundle, bw);
      }

      // Get Dialogs Resource Strings
      Map<String, String> dialogsBundle = checkExistingBundles(dialogsBundleName, targetLanguage);
      if (dialogsBundle == null || dialogsBundle.isEmpty()) {
        System.out.println("  **No Dialogs Bundles Exist on GP....");
      } else {
        // update Dialog Nodes in source conversation workspace with
        // translated content
        bw.write("*********************************************************\n");
        bw.write("************************ DIALOGS ***********************\n");
        bw.write("*********************************************************\n");
        bw.write("\n");
        bw.flush();
        updateDialogs(jsonWCSPayload, dialogsBundle, bw, entitiesBundle);
      }

      if ((intentsBundle != null && !intentsBundle.isEmpty())
          || (entitiesBundle != null && !entitiesBundle.isEmpty())
          || (dialogsBundle != null && !dialogsBundle.isEmpty())) {
        // Set target Language from source conversation workspace
        String newName = null;
        if (!jsonWCSPayload.isJsonNull()) {
          jsonWCSPayload.addProperty("language", targetLanguage);
          // Set New name for workspace
          newName = jsonWCSPayload.get("name").getAsString();
          if (newName != null && !newName.isEmpty()) {
            newName = newName + "_" + targetLanguage;
            jsonWCSPayload.addProperty("name", newName);
          }
        } else {
          throw new WCSWorkspaceException("No Workspace retrieved from WCS");
        }

        // Create or Update conversation workspace with translated content
        checkWCSWorkspace(jsonWCSPayload);
      } else {
        throw new WCSWorkspaceException("No Workspace created for WCS. Nothing retreived from GP");
      }
    }
  }

  /***
   * Create a new workspace on WCS if target workspace id is not provided Update
   * an existing workspace if target workspace id is provided
   * 
   * @param jsonWCSPayload
   * @return
   * @throws IOException
   */
  private void checkWCSWorkspace(JsonObject jsonWCSPayload) throws IOException {

    // Fetch Command Line Params
    String targetWorkspaceId = getTargetworkspaceID();
    String versionDate = getVersionDate();

    // Get WCS Creds
    String wcsCreds = getWCSCreds();
    String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((wcsCreds).getBytes());

    // Create or Update Watson Conv Workspace
    WCSUtils.putWCSWorkspace(targetWorkspaceId, authorizationHeader, versionDate, jsonWCSPayload);
  }

  /***
   * Parse Intents in source workspace and update it with translated content
   * 
   * @param jsonWCSPayload
   * @param intentsBundle
   * @param bw
   * @return
   * @throws IOException
   */
  private void updateIntents(JsonObject jsonWCSPayload, Map<String, String> intentsBundle, BufferedWriter bw)
      throws IOException {
    // Fetch Intents
    System.out.println("\n");
    System.out.println("*** Updating Intents on WCS with Translatable Contents ***");
    JsonArray intents = jsonWCSPayload.getAsJsonArray("intents");
    String keyTobeInserted = null;
    for (JsonElement intentsArray : intents) {
      JsonObject intentObj = intentsArray.getAsJsonObject();
      JsonArray intentEx = (JsonArray) intentObj.get("examples");
      Set<String> intentSet = new HashSet<String>(intentEx.size());
      List<Integer> removeElements = new ArrayList<Integer>();
      int i = 0;
      for (JsonElement intentsText : intentEx) {
        JsonObject intentTextObj = intentsText.getAsJsonObject();
        String textIntent = intentTextObj.get("text").getAsString();
        if (textIntent != null && textIntent.length() > 0) {
          int textIntentHash = textIntent.hashCode();
          String result = Integer.toHexString(textIntentHash);
          String append = null;
          if (textIntent.length() < 5) {
            append = textIntent.substring(0, textIntent.length());
          } else {
            append = textIntent.substring(0, 4);
          }
          result = append + "_" + result;
          if (intentsBundle.containsKey(result)) {
            keyTobeInserted = intentsBundle.get(result);
            if (!keyTobeInserted.isEmpty()) {
              String newKey = keyTobeInserted.toLowerCase();
              if (!intentSet.contains(newKey)) {
                intentSet.add(newKey);
                intentTextObj.addProperty("text", keyTobeInserted);
              } else {
                removeElements.add(i);
              }
            } else {
              bw.write(String.format("Intent Text -> %s was not translated by GP\n", textIntent));
            }
          }
          i++;
        }
      }
      // Remove duplicate JSON elements
      for (int k = removeElements.size() - 1; k >= 0; k--) {
        intentEx.remove(intentEx.get(removeElements.get(k)));
      }
      removeElements.clear();
    }
    System.out.println("    Intents Updated");
    bw.write("\n");
    bw.flush();
  }

  /***
   * Parse Entities in source workspace and update it with translated content
   * 
   * @param jsonResponseObject
   * @param entityMap
   * @param bw
   * @return
   * @throws IOException
   */
  private void updateEntities(JsonObject jsonResponseObject, Map<String, String> entityMap, BufferedWriter bw)
      throws IOException {
    // Fetch Entities
    System.out.println("\n");
    System.out.println("*** Updating Entities on WCS with Translatable Contents ***");
    JsonArray entities = jsonResponseObject.getAsJsonArray("entities");
    for (JsonElement entitiesArray : entities) {
      JsonObject entityObj = entitiesArray.getAsJsonObject();
      JsonArray valuesEx = (JsonArray) entityObj.get("values");
      // get entity
      String entityValue = entityObj.get("entity").getAsString();

      Set<String> intentSet = new HashSet<String>(valuesEx.size());
      List<Integer> removeElements = new ArrayList<Integer>();
      int i = 0;
      for (JsonElement entitiesText : valuesEx) {
        JsonObject entityValObj = entitiesText.getAsJsonObject();
        // get value
        String entityVal = entityValObj.get("value").getAsString();
        String entityKey = entityValue + ":" + entityVal;
        if (entityMap.containsKey(entityKey)) {
          String keyTobeInserted = entityMap.get(entityKey);
          if (!keyTobeInserted.isEmpty()) {
            String newKey = keyTobeInserted.toLowerCase();
            if (keyTobeInserted.length() > 64) {
              bw.write(String.format("Entity -> %s Value -> %s exceeds limit of 64 characters in target language \n",
                  entityValue, entityVal));
            } else {
              if (!intentSet.contains(newKey)) {
                intentSet.add(newKey);
                entityValObj.addProperty("value", keyTobeInserted);
              } else {
                removeElements.add(i);
              }
            }
          } else {
            bw.write(String.format("Entity value -> %s was not translated by GP\n", entityVal));
          }
        }
        entityKey = null;
        // get synonym
        JsonArray synonymsVal = (JsonArray) entityValObj.get("synonyms");
        if (synonymsVal.size() > 0) {
          Set<String> synSet = new HashSet<String>(valuesEx.size());
          List<Integer> removeSyn = new ArrayList<Integer>();
          JsonArray entitySet = new JsonArray(synonymsVal.size());
          int j = 0;
          for (JsonElement synonymText : synonymsVal) {
            String synonymVal = synonymText.getAsString();
            entityKey = entityValue + ":" + synonymVal;
            if (entityMap.containsKey(entityKey)) {
              String key = entityMap.get(entityKey);
              if (!key.isEmpty()) {
                String newKey = key.toLowerCase();
                if (!synSet.contains(newKey)) {
                  synSet.add(newKey);
                  entitySet.add(key);
                } else {
                  removeSyn.add(j);
                }
              } else {
                bw.write(String.format("Entity Synonym -> %s was not translated by GP\n", synonymVal));
              }
            }
            if (entitySet.size() > 0) {
              entityValObj.add("synonyms", entitySet);
            }
            entityKey = null;
            j++;
          }
          if (removeSyn.size() > 0) {
            for (int k = removeSyn.size() - 1; k >= 0; k--) {
              synonymsVal.remove(removeSyn.get(k));
            }
          }
        } else {
          entityValObj.remove("synonyms");
        }
        i++;
      }
      // Remove duplicate JSON elements
      for (int k = removeElements.size() - 1; k >= 0; k--) {
        valuesEx.remove(valuesEx.get(removeElements.get(k)));
      }
      removeElements.clear();
    }
    System.out.println("    Entities Updated");
    bw.write("\n");
    bw.flush();
  }

  /***
   * Parse Dialogs in source workspace and update it with translated content
   * 
   * @param jsonResponseObject
   * @param dialogMap
   * @param bw
   * @param entitiesBundle
   * @return
   * @throws IOException
   */
  private void updateDialogs(JsonObject jsonResponseObject, Map<String, String> dialogMap, BufferedWriter bw,
      Map<String, String> entitiesBundle) throws IOException {
    // Fetch Dialog Nodes
    System.out.println("\n");
    System.out.println("*** Updating Dialog on WCS with Translatable Contents ***");
    JsonArray dialogNodes = jsonResponseObject.getAsJsonArray("dialog_nodes");
    for (JsonElement dialogArray : dialogNodes) {
      JsonObject dialogNodeObj = dialogArray.getAsJsonObject();
      JsonElement element = dialogNodeObj.get("output");
      if (!(element instanceof JsonNull)) {
        JsonObject outputEx = (JsonObject) element;
        JsonElement valuesTextEx = (JsonElement) outputEx.get("text");
        if (!(valuesTextEx instanceof JsonNull)) {
          if (valuesTextEx != null) {
            // text is String, Json Array or Json Object
            if (valuesTextEx.isJsonPrimitive()) {
              if (valuesTextEx.getAsJsonPrimitive().isString()) {
                String textVal = valuesTextEx.getAsString();
                if (textVal != null && textVal.length() > 0) {
                  int textHashCode = textVal.hashCode();
                  String result = Integer.toHexString(textHashCode);
                  String append = null;
                  if (textVal.length() < 5) {
                    append = textVal.substring(0, textVal.length());
                  } else {
                    append = textVal.substring(0, 4);
                  }
                  result = append + "_" + result;
                  if (dialogMap.containsKey(result)) {
                    String keyTobeInserted = dialogMap.get(result);
                    if (!keyTobeInserted.isEmpty()) {
                      outputEx.addProperty("text", keyTobeInserted);
                    } else {
                      bw.write(String.format("Dialog Text -> %s was not translated by GP\n", textVal));
                    }
                  }
                }
              }
              // else -> text will always be present as a String. ignore
            } else {
              if (valuesTextEx.isJsonArray()) {
                // Text is Json Array
                JsonArray jsonNodes = valuesTextEx.getAsJsonArray();
                JsonArray textArrayToBeReplaced = new JsonArray();
                for (JsonElement jsonArray : jsonNodes) {
                  String jsonText = jsonArray.getAsString();
                  if (jsonText != null && jsonText.length() > 0) {
                    int jsonHashCode = jsonText.hashCode();
                    String result = Integer.toHexString(jsonHashCode);
                    String append = null;
                    if (jsonText.length() < 5) {
                      append = jsonText.substring(0, jsonText.length());
                    } else {
                      append = jsonText.substring(0, 4);
                    }
                    result = append + "_" + result;
                    if (dialogMap.containsKey(result)) {
                      String keyTobeInserted = dialogMap.get(result);
                      if (!keyTobeInserted.isEmpty()) {
                        textArrayToBeReplaced.add(keyTobeInserted);
                        outputEx.add("text", textArrayToBeReplaced);
                      } else {
                        bw.write(String.format("Dialog Text -> %s was not translated by GP\n", jsonText));
                      }
                    }
                  }
                }
              } else {
                if (valuesTextEx.isJsonObject()) {
                  // Text contains values as JsonObject
                  JsonObject jsonTextObj = (JsonObject) valuesTextEx;
                  JsonArray valuesNodes = jsonTextObj.getAsJsonArray("values");
                  JsonArray textArrayToBeReplaced = new JsonArray();
                  for (JsonElement valuesArray : valuesNodes) {
                    String valuesText = valuesArray.getAsString();
                    if (valuesText != null && valuesText.length() > 0) {
                      int valuesHashCode = valuesText.hashCode();
                      String result = Integer.toHexString(valuesHashCode);
                      String append = null;
                      if (valuesText.length() < 5) {
                        append = valuesText.substring(0, valuesText.length());
                      } else {
                        append = valuesText.substring(0, 4);
                      }
                      result = append + "_" + result;
                      if (dialogMap.containsKey(result)) {
                        String keyTobeInserted = dialogMap.get(result);
                        if (!keyTobeInserted.isEmpty()) {
                          textArrayToBeReplaced.add(keyTobeInserted);
                        } else {
                          bw.write(String.format("Dialog Text -> %s was not translated by GP\n", valuesText));
                        }
                      }
                    }
                  }
                  jsonTextObj.remove("values");
                  jsonTextObj.add("values", textArrayToBeReplaced);
                }
              }
            }
          }
        }
      }
      JsonElement condition = dialogNodeObj.get("conditions");
      if (!(condition instanceof JsonNull)) {
        String getCondition = condition.getAsString();
        if (getCondition.contains("@") && getCondition.contains(":")) {
          String[] getStrings = getCondition.split(" ");
          for (int i = 0; i < getStrings.length; i++) {
            if (getStrings[i].contains("@") && getStrings[i].contains(":")) {
              String getEntity = getStrings[i].substring(getStrings[i].indexOf('@') + 1, getStrings[i].length());
              getEntity = getEntity.replaceAll("[^a-zA-Z0-9:_]", "");
              if (entitiesBundle.containsKey(getEntity)) {
                String beforeColon = getStrings[i].substring(0, getStrings[i].indexOf(":"));
                String keyTobeInserted = entitiesBundle.get(getEntity);
                if (!keyTobeInserted.isEmpty()) {
                  getStrings[i] = beforeColon + ":" + keyTobeInserted;
                } else {
                  bw.write(String.format("Dialog Condition -> %s was not translated by GP \n", getEntity));
                }
              } else {
                bw.write(String.format("GP does not contain Entity Value -> %s \n", getEntity));
              }
            }
          }
          String str = String.join(" ", getStrings);
          dialogNodeObj.addProperty("conditions", str);
        }
      }
    }
    System.out.println("    Dialog Nodes Updated");
    bw.write("\n");
    bw.flush();
  }

  /***
   * Get source conversation workspace
   * 
   * @param wcsCreds
   * @param workspaceId
   * @param versionDate
   * @return jsonResponse
   */
  private JsonObject getConvWorkspace() {
    // Get Watson Conv Creds
    String wcsCreds = getWCSCreds();
    // Get Params
    String workspaceId = getSourceworkspaceId();
    String versionDate = getVersionDate();

    // Get Watson Conv Workspace
    JsonObject jsonResponse = WCSUtils.getWCSWorkspace(wcsCreds, workspaceId, versionDate);
    return jsonResponse;
  }

  /***
   * Check if Bundle exists on GP. If Bundle exists on GP then fetch all
   * resource strings in target language from the bundle
   * 
   * @param intentsBundleName
   * @param targetLanguage
   * @return intentMap
   * @throws ServiceException
   * @throws WCSWorkspaceException
   */
  private Map<String, String> checkExistingBundles(String intentsBundleName, String targetLanguage)
      throws ServiceException, WCSWorkspaceException {
    Set<String> gpBundleIds = getGPClient().getBundleIds();
    Map<String, String> resourceStrings = new HashMap<String, String>();
    Map<String, String> intentMap = new HashMap<String, String>();
    if (gpBundleIds == null || gpBundleIds.size() == 0) {
      return null;
    } else {
      if (gpBundleIds.size() > 0) {
        // Parse Bundle Names
        // Some bundle exist on GP. Parse them
        Set<String> bundleNames = new HashSet<String>();
        boolean checkLang = true;
        for (String str : gpBundleIds) {
          if (str.equals(intentsBundleName) || str.contains(intentsBundleName)) {
            bundleNames.add(str);
          }
        }
        if (bundleNames.size() > 0) {
          for (String gpBundle : bundleNames) {
            Set<String> gpTargetLanguage = getGPClient().getBundleInfo(gpBundle).getTargetLanguages();
            if (gpTargetLanguage.contains(targetLanguage)) {
              resourceStrings = getGPClient().getResourceStrings(gpBundle, targetLanguage, false);
            } else {
              System.out.println("Target language does not exist on GP for intents bundle");
              checkLang = false;
            }
            intentMap.putAll(resourceStrings);
            resourceStrings.clear();
          }
        }
        if (checkLang == false) {
          throw new WCSWorkspaceException("\n Target language should exist on GP for intents, entities and dialogs");
        }
      }
    }
    return intentMap;
  }
}
