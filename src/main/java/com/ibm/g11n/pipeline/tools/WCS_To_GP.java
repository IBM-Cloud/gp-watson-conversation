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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.BundleDataChangeSet;
import com.ibm.g11n.pipeline.client.NewBundleData;
import com.ibm.g11n.pipeline.client.NewResourceEntryData;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;

/***
 * Watson Converstaion to Globalization Pipeline Utility
 * 
 * @author Harpreet Chawla (hchawla@us.ibm.com)
 *
 */
public class WCS_To_GP extends BaseUtility {

  @Override
  public void _execute() throws Exception {

    setSplitSize(300);

    // Get Workspace in JSON format from WCS API
    JsonObject jsonWCSPayload = getConvWorkspace();

    if (jsonWCSPayload.size() > 0) {
      // Parse Intents and store it in Intents TreeMap
      Map<String, TreeSet<String>> uniqueBotOutputIntents = parseWCSIntents(jsonWCSPayload);

      // Parse Intents and store it in Entities TreeMap
      Map<String, TreeSet<String>> uniqueBotOutputEntities = parseWCSEntities(jsonWCSPayload);

      // Parse Intents and store it in Dialogs TreeMap
      Map<String, TreeSet<String>> uniqueBotOutputDialogs = parseWCSDialogs(jsonWCSPayload);

      int splitSize = getSplitSize();
      // Create bundles on GP and upload resource string to bundles
      createBundles(uniqueBotOutputIntents, uniqueBotOutputEntities, uniqueBotOutputDialogs, splitSize, jsonWCSPayload);
    }

  }

  /***
   * Create Bundles on GP for Intents, entities and dialogs (if they don't
   * exist). If Bundles already exist on GP then look for modified resource
   * strings in WCS and update GP accordingly
   * 
   * @param uniqueBotOutputIntents
   * @param uniqueBotOutputEntities
   * @param uniqueBotOutputDialogs
   * @param MAP_SPLIT_SIZE
   * @param jsonWCSPayload
   * @return
   * @throws ServiceException
   * @throws Exception
   */
  private void createBundles(Map<String, TreeSet<String>> uniqueBotOutputIntents,
      Map<String, TreeSet<String>> uniqueBotOutputEntities, Map<String, TreeSet<String>> uniqueBotOutputDialogs,
      int splitSize, JsonObject jsonWCSPayload) throws ServiceException, Exception {

    // Get Command Line Params
    String workspaceId = getSourceworkspaceId();
    String targetLanguage = getTargetLanguage();
    String bundlePrefix = getBundleId();

    System.out.println("\n");
    System.out.println("** Creating Bundles on GP **");

    final String intents = "-intents";
    final String entities = "-entities";
    final String dialogs = "-dialogs";

    String intentsBundle = null;
    String entitiesBundle = null;
    String dialogsBundle = null;

    // Choose between BundleId and workspaceID. Preference given to BundleId
    if (bundlePrefix != null) {
      intentsBundle = bundlePrefix + intents;
      entitiesBundle = bundlePrefix + entities;
      dialogsBundle = bundlePrefix + dialogs;

    } else {
      intentsBundle = workspaceId + intents;
      entitiesBundle = workspaceId + entities;
      dialogsBundle = workspaceId + dialogs;
    }

    // Get source Language from WCS JSON Payload
    String gp_sourceLanguage = null;
    NewBundleData newBundleData = null;
    JsonElement gp_srcLanguage = jsonWCSPayload.get("language");
    if (!gp_srcLanguage.isJsonNull()) {
      gp_sourceLanguage = gp_srcLanguage.getAsString();
      if (gp_sourceLanguage != null && !gp_sourceLanguage.isEmpty()) {
        newBundleData = new NewBundleData(gp_sourceLanguage);
      }
    } else {
      throw new WCSWorkspaceException("No Language Specified in WCS");
    }

    Set<String> targetLangs = null;
    if (targetLanguage != null) {
      String[] langs = targetLanguage.split(",");
      targetLangs = new HashSet<String>(langs.length);
      for (int i = 0; i < langs.length; i++) {
        targetLangs.add(langs[i].trim());
      }
      newBundleData.setTargetLanguages(targetLangs);
    }

    Set<String> gpbundleIds = null;
    gpbundleIds = getGPClient().getBundleIds();
    boolean intentBundleExists = false;
    boolean entityBundleExists = false;
    boolean dialogBundleExists = false;

    if (gpbundleIds.size() == 0) {
      // Bundle does not exist
      intentBundleExists = false;
      entityBundleExists = false;
      dialogBundleExists = false;
      System.out.println("Bundles does not exist on GP. Creating them..");
    } else {
      Set<String> intentsGPBundle = new HashSet<String>();
      Set<String> entitiesGPBundle = new HashSet<String>();
      Set<String> dialogsGPBundle = new HashSet<String>();
      // Some bundle already exist on GP. Parse the existing bundle Id's
      for (String str : gpbundleIds) {
        if (str.equals(intentsBundle) || str.contains(intentsBundle)) {
          intentBundleExists = true;
          intentsGPBundle.add(str);
        }
        if (str.equals(entitiesBundle) || str.contains(entitiesBundle)) {
          entityBundleExists = true;
          entitiesGPBundle.add(str);
        }
        if (str.equals(dialogsBundle) || str.contains(dialogsBundle)) {
          dialogBundleExists = true;
          dialogsGPBundle.add(str);
        }
      }

      if (intentsGPBundle.size() > 0) {
        System.out.println("Intent Bundle already exists on GP");
        updateTargetLanguages(intentsGPBundle, targetLangs);
        updatedGPBundle(intentsGPBundle, gp_sourceLanguage, uniqueBotOutputIntents, splitSize, newBundleData);
      }

      if (entitiesGPBundle.size() > 0) {
        System.out.println("Entity Bundle already exists on GP");
        updateTargetLanguages(entitiesGPBundle, targetLangs);
        updatedGPBundle(entitiesGPBundle, gp_sourceLanguage, uniqueBotOutputEntities, splitSize, newBundleData);
      }

      if (dialogsGPBundle.size() > 0) {
        System.out.println("Dialog Bundle already exists on GP");
        updateTargetLanguages(dialogsGPBundle, targetLangs);
        updatedGPBundle(dialogsGPBundle, gp_sourceLanguage, uniqueBotOutputDialogs, splitSize, newBundleData);
      }
    }
    // BundleId's does not exist on GP
    if (intentBundleExists == false) {
      if (uniqueBotOutputIntents.size() > 0) {
        System.out.println("Intent Bundle does not exist on GP. Creating..");
        if (uniqueBotOutputIntents.size() > splitSize) {
          // Break into smaller bundles of size 300
          generateSmallBundles(uniqueBotOutputIntents, splitSize, intentsBundle, newBundleData);
          System.out.println("Uploaded Resource Strings to Intents \n");
        } else {
          // Create Bundle on GP
          intentsBundle = intentsBundle + "-1";
          getGPClient().createBundle(intentsBundle, newBundleData);
          System.out.println("Created " + intentsBundle);

          // Upload Resource Strings to Entities
          uploadToGP(intentsBundle, uniqueBotOutputIntents);
          System.out.println("Uploaded Resource Strings to Intents \n");
        }
      }
    }
    if (entityBundleExists == false) {
      // Create Bundle on GP
      if (uniqueBotOutputEntities.size() > 0) {
        System.out.println("Entity Bundle does not exist on GP. Creating..");
        if (uniqueBotOutputEntities.size() > splitSize) {
          // Break into smaller bundles of size 300
          generateSmallBundles(uniqueBotOutputEntities, splitSize, entitiesBundle, newBundleData);
          System.out.println("Uploaded Resource Strings to Entities \n");
        } else {
          entitiesBundle = entitiesBundle + "-1";
          getGPClient().createBundle(entitiesBundle, newBundleData);
          System.out.println("Created " + entitiesBundle);

          // Upload Resource Strings to Entities
          uploadEntities(entitiesBundle, uniqueBotOutputEntities);
          System.out.println("Uploaded Resource Strings to Entities \n");
        }
      }
    }
    if (dialogBundleExists == false) {
      // Create Bundle on GP
      if (uniqueBotOutputDialogs.size() > 0) {
        System.out.println("Dialog Bundle does not exist on GP. Creating..");
        if (uniqueBotOutputDialogs.size() > splitSize) {
          // Break into smaller bundles of size 300
          generateSmallBundles(uniqueBotOutputDialogs, splitSize, dialogsBundle, newBundleData);
          System.out.println("Uploaded Resource Strings to Dialogs \n");
        } else {
          // Size is less than 300 -> just upload it to GP
          dialogsBundle = dialogsBundle + "-1";
          getGPClient().createBundle(dialogsBundle, newBundleData);
          System.out.println("Created " + dialogsBundle);

          // Upload Resource Strings to Dialog
          uploadToGP(dialogsBundle, uniqueBotOutputDialogs);
          System.out.println("Uploaded Resource Strings to Dialogs \n");
        }
      }
    }
  }

  /***
   * If intents, entities and dialogs size exceeds size 300 then create smaller
   * bundles on GP
   * 
   * @param uniqueBotOutput
   * @param splitSize
   * @param bundleName
   * @param newBundleData
   * @param uniqueBotOutputEntities
   * @return
   * @throws ServiceException
   */
  private void generateSmallBundles(Map<String, TreeSet<String>> uniqueBotOutput, int splitSize, String bundleName,
      NewBundleData newBundleData) throws ServiceException {
    int counter = 1;
    int batch = 1;
    TreeMap<String, TreeSet<String>> newIntentMap = new TreeMap<String, TreeSet<String>>();
    String newBundlename = null;
    for (Entry<String, TreeSet<String>> entry : uniqueBotOutput.entrySet()) {

      TreeSet<String> newIntentSet = entry.getValue();
      String key = entry.getKey();
      newIntentMap.put(key, newIntentSet);

      if (counter == splitSize) {
        newBundlename = bundleName + "-" + batch;
        // Create Bundle on GP
        getGPClient().createBundle(newBundlename, newBundleData);
        System.out.println("Created " + newBundlename);

        // Upload Resource Strings to Intents
        if (newBundlename.contains("-dialogs") || newBundlename.contains("-intents")) {
          uploadToGP(newBundlename, newIntentMap);
        } else {
          uploadEntities(newBundlename, newIntentMap);
        }
        batch++;
        newBundlename = null;
        newIntentMap.clear();
        counter = 0;
      }
      counter++;
    }
    // Handle last bucket
    if (newIntentMap.size() > 0) {
      newBundlename = bundleName + "-" + batch;
      getGPClient().createBundle(newBundlename, newBundleData);
      System.out.println("Created " + newBundlename);
      // Upload Resource Strings
      if (newBundlename.contains("-dialogs") || newBundlename.contains("-intents")) {
        uploadToGP(newBundlename, newIntentMap);
      } else {
        uploadEntities(newBundlename, newIntentMap);
      }
    }
  }

  /***
   * Update target languages
   * 
   * @param intentsGPBundle
   * @param targetLanguages
   * @return
   * @throws ServiceException
   */
  private void updateTargetLanguages(Set<String> bundleNames, Set<String> targetLanguages) throws ServiceException {
    Set<String> updatedTrgLangs = new HashSet<String>();

    for (String name : bundleNames) {
      if (targetLanguages != null) {
        updatedTrgLangs.addAll(targetLanguages);
      }

      ServiceClient client = getGPClient();
      BundleData bundleInfo = client.getBundleInfo(name);
      Set<String> currentTrgLangs = bundleInfo.getTargetLanguages();
      if (currentTrgLangs != null) {
        if (!updatedTrgLangs.equals(currentTrgLangs)) {
          BundleDataChangeSet changes = new BundleDataChangeSet();
          updatedTrgLangs.addAll(currentTrgLangs);
          changes.setTargetLanguages(updatedTrgLangs);
          getGPClient().updateBundle(name, changes);
        }
      } else {
        if (updatedTrgLangs.size() > 0) {
          BundleDataChangeSet changes = new BundleDataChangeSet();
          changes.setTargetLanguages(updatedTrgLangs);
          getGPClient().updateBundle(name, changes);
        }
      }
    }
  }

  /***
   * Get Resource strings of existing Bundles from GP and compare Resource
   * Strings with WCS translatable content If they are identical, don't do
   * anything if not, then update GP with modified WCS translatable content
   * 
   * @param exists
   * @param bundleNames
   * @param src
   * @param splitSize
   * @param newBundleData
   * @param uniqueBotOutputIntents
   * @param uniqueBotOutputEntities
   * @param uniqueBotOutputDialogs
   * @return
   * @throws ServiceException
   * @throws WCSWorkspaceException
   */
  private void updatedGPBundle(Set<String> bundleNames, String src, Map<String, TreeSet<String>> outputMap,
      int splitSize, NewBundleData newBundleData) throws ServiceException, WCSWorkspaceException {

    // Get Bundle Contents from GP
    TreeMap<String, Map<String, String>> intentMap = new TreeMap<String, Map<String, String>>();
    Map<String, String> gpMap = new HashMap<String, String>();
    Iterator<String> setItr = bundleNames.iterator();
    Map<String, String> tempMap = null;
    String name = null;
    while (setItr.hasNext()) {
      name = setItr.next();
      tempMap = getGPClient().getResourceStrings(name, src, false);
      gpMap.putAll(tempMap);
      intentMap.put(name, tempMap);
    }
    // compare resource strings on GP and WCS
    Map<String, String> newMap = getUpdatedStrings(gpMap, name, outputMap);

    if (newMap != null && newMap.size() > 0) {
      // Update Resource Strings on GP
      System.out.println("      ** " + newMap.size() + " Resource Strings has been modified. Updating on GP \n");
      updateBundlesGP(newMap, intentMap, gpMap, splitSize, bundleNames, newBundleData);
    } else {
      System.out.println("      Resource Strings has not changed. Not Updating GP \n");
    }

  }

  /***
   * Update existing Bundles on GP with modified Resource strings
   * 
   * @param newMap
   * @param intentMap
   * @param gpMap
   * @param gpMap
   * @param splitSize
   * @param bundleNames
   * @param splitSize
   * @param bundleNames
   * @param newBundleData
   * @return gpMapUpdated
   * @throws ServiceException
   * @throws WCSWorkspaceException
   */
  private void updateBundlesGP(Map<String, String> newMap, TreeMap<String, Map<String, String>> intentMap,
      Map<String, String> gpMap, int splitSize, Set<String> bundleNames, NewBundleData newBundleData)
      throws ServiceException, WCSWorkspaceException {
    // Add new modifications from WCS
    boolean addEntries = false;
    boolean deleteEntries = false;
    for (Entry<String, String> entrySet : newMap.entrySet()) {
      String newMapKey = entrySet.getKey();
      String newMapVal = entrySet.getValue();
      if (!newMapVal.equals("null-remove")) {
        // Add Resource strings to GP
        addEntries = true;
      } else {
        deleteEntries = true;
        // Delete Resource Strings from GP
        Map<String, String> innerMap = new ConcurrentHashMap<>();
        for (Map.Entry<String, Map<String, String>> entryMap : intentMap.entrySet()) {
          innerMap = entryMap.getValue();
          if (innerMap.containsKey(newMapKey)) {
            innerMap.remove(newMapKey);
            break;
          }
        }
      }
    }
    if (deleteEntries == true) {
      for (Map.Entry<String, Map<String, String>> entryMap : intentMap.entrySet()) {
        String key = entryMap.getKey();
        Map<String, String> updateGPMap = entryMap.getValue();
        if (updateGPMap.size() == 0) {
          // Delete Bundles From GP
          System.out.println("      ** No Resoucre Strings left in bundle " + key + " ...Deleting it");
          getGPClient().deleteBundle(key);
          // break;
        } else {
          // Update Resource Strings on GP
          updateResourceStrings(key, updateGPMap);
        }
      }
    }
    if (addEntries == true) {
      String newstr = null;
      String maxBundleId = null;
      int maxIndex = 1;
      Map<String, String> newBundleGP = new HashMap<String, String>();
      boolean existingEmptyBundle = false;
      for (Map.Entry<String, Map<String, String>> entryMap : intentMap.entrySet()) {
        newstr = entryMap.getKey();
        newBundleGP = entryMap.getValue();

        if (newBundleGP.size() < 300) {
          if (newBundleGP.size() + newMap.size() <= 300) {
            existingEmptyBundle = true;
            break;
          }
        }
      }

      if (existingEmptyBundle == true) {
        newBundleGP.putAll(newMap);
        updateResourceStrings(newstr, newBundleGP);
      } else {
        for (Map.Entry<String, Map<String, String>> entryMap : intentMap.entrySet()) {
          newstr = entryMap.getKey();
          String bundleId = newstr.substring(newstr.lastIndexOf('-') + 1, newstr.length());
          int index = Integer.parseInt(bundleId);
          if (index >= maxIndex) {
            maxIndex = index;
            maxBundleId = newstr;
          }
        }
        if (newMap.size() <= 300) {
          maxIndex++;
          String newBundleIndex = Integer.toString(maxIndex);
          String bundleName = maxBundleId.substring(0, (maxBundleId.lastIndexOf('-')));
          String newGPBundle = bundleName + "-" + newBundleIndex;
          System.out.println("      ** Uploading " + newMap.size() + " resource strings to Bundle " + newGPBundle);

          getGPClient().createBundle(newGPBundle, newBundleData);
          updateResourceStrings(newGPBundle, newMap);
        } else {
          int counter = 1;
          Map<String, String> uploadGPMap = new TreeMap<String, String>();

          for (Map.Entry<String, String> newMapEntrySet : newMap.entrySet()) {
            String newMapValue = newMapEntrySet.getValue();
            String newMapKey = newMapEntrySet.getKey();
            uploadGPMap.put(newMapKey, newMapValue);

            if (counter == splitSize) {
              maxIndex++;
              String newBundleIndex = Integer.toString(maxIndex);
              String bundleName = maxBundleId.substring(0, (maxBundleId.lastIndexOf('-')));
              String newGPBundle = bundleName + "-" + newBundleIndex;
              System.out
                  .println("      ** Uploading " + uploadGPMap.size() + " resource strings to Bundle " + newGPBundle);

              getGPClient().createBundle(newGPBundle, newBundleData);
              updateResourceStrings(newGPBundle, uploadGPMap);
              uploadGPMap.clear();
              maxBundleId = newGPBundle;
              counter = 0;
            }
            counter++;
          }
          // Handle last bucket
          if (uploadGPMap.size() > 0) {
            maxIndex++;
            String newBundleIndex = Integer.toString(maxIndex);
            String bundleName = maxBundleId.substring(0, (maxBundleId.lastIndexOf('-')));
            String newGPBundle = bundleName + "-" + newBundleIndex;
            System.out
                .println("      ** Uploading " + uploadGPMap.size() + " resource strings to Bundle " + newGPBundle);

            getGPClient().createBundle(newGPBundle, newBundleData);
            updateResourceStrings(newGPBundle, uploadGPMap);
          }
        }
      }
    }
    System.out.println("\n");
  }

  /***
   * Compare existing resource strings from WCS to GP
   * 
   * @param gpMap
   * @param bundleID
   * @param uniqueBotOutputIntents
   * @param uniqueBotOutputEntities
   * @param uniqueBotOutputDialogs
   * @return gpMapUpdated
   */
  private Map<String, String> getUpdatedStrings(Map<String, String> gpMap, String bundleID,
      Map<String, TreeSet<String>> botOutput) {

    Map<String, String> wcsMap = null;
    if (bundleID.contains("-intents") || bundleID.contains("-dialogs")) {
      wcsMap = fetchIntents(botOutput);
    } else {
      wcsMap = fetchEntities(botOutput);
    }
    Map<String, String> gpMapUpdated = new HashMap<String, String>();

    // Compare WCS and GP contents
    boolean equal = true;
    // Look for missing or modified values in WCS
    for (Entry<String, String> entry : wcsMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (gpMap.containsKey(key)) {
        String intentVal = gpMap.get(key);
        if (!value.equals(intentVal)) {
          // GP Bundle has the same key but value is modified now
          gpMapUpdated.put(key, value);
          equal = false;
        }
      } else {
        // GP Bundle does not have this key value pair -> add it
        gpMapUpdated.put(key, value);
        equal = false;
      }
    }
    // Look for removed values in GP which no longer exist in WCS
    for (Entry<String, String> entry : gpMap.entrySet()) {
      String key = entry.getKey();
      if (!wcsMap.containsKey(key)) {
        // GP Bundle have this key value pair which is no longer used in
        // WCS -> delete it
        equal = false;
        gpMapUpdated.put(key, "null-remove");
      }
    }

    if (equal == false) {
      return gpMapUpdated;
    } else {
      return null;
    }
  }

  /***
   * Update GP with modified resource strings from WCS
   * 
   * @param bundleID
   * @param updatedMap
   * @return
   */
  private void updateResourceStrings(String bundleID, Map<String, String> updatedMap) {
    try {
      String src = getGPClient().getBundleInfo(bundleID).getSourceLanguage();
      getGPClient().uploadResourceStrings(bundleID, src, updatedMap);

    } catch (ServiceException e) {
      e.printStackTrace();
    }
  }

  /***
   * Fetch Intents or Dialogs
   * 
   * @param bundleID
   * @param uniqueBotOutputIntents
   * @param uniqueBotOutputEntities
   * @param uniqueBotOutputDialogs
   * @return updatedMap
   */
  private Map<String, String> fetchIntents(Map<String, TreeSet<String>> botMap) {
    Map<String, String> updatedMap = new HashMap<String, String>();
    for (Entry<String, TreeSet<String>> res : botMap.entrySet()) {
      String numberAsString = res.getKey();
      TreeSet<String> intentValue = res.getValue();
      if (intentValue.size() == 1) {
        String str = intentValue.first();
        updatedMap.put(numberAsString, str);
      } else {
        int i = 0;
        for (String str : intentValue) {
          numberAsString = numberAsString + ":" + i;
          updatedMap.put(numberAsString, str);
          i++;
        }
      }
    }
    return updatedMap;
  }

  /***
   * Fetch Entities
   * 
   * @param botEntities
   * @return updatedMap
   */
  private Map<String, String> fetchEntities(Map<String, TreeSet<String>> botEntities) {
    Map<String, String> updatedMap = new HashMap<String, String>();
    for (Entry<String, TreeSet<String>> res : botEntities.entrySet()) {
      String key = res.getKey();
      TreeSet<String> intentValue = res.getValue();
      if (intentValue.size() == 1) {
        String str = intentValue.first();
        updatedMap.put(key, str);
      } else {
        int i = 0;
        for (String str : intentValue) {
          key = key + ":" + i;
          updatedMap.put(key, str);
          i++;
        }
      }
    }
    return updatedMap;
  }

  /***
   * Upload resource strings to respective bundles on GP.
   * 
   * @param resourceString
   * @param uniqueBotOutputTree
   * @param uniqueBotOutputEntity
   * @return
   */
  private void uploadToGP(String resourceString, Map<String, TreeSet<String>> uniqueBotOutputTree) {
    if (resourceString.contains("-intents") || resourceString.contains("-dialogs")) {
      Map<String, String> intentEntries = new HashMap<String, String>(uniqueBotOutputTree.size());
      for (Entry<String, TreeSet<String>> res : uniqueBotOutputTree.entrySet()) {
        String numberAsString = res.getKey();
        TreeSet<String> intentValue = res.getValue();
        if (intentValue.size() == 1) {
          String str = intentValue.first();
          intentEntries.put(numberAsString, str);
        } else {
          int i = 0;
          for (String str : intentValue) {
            numberAsString = numberAsString + ":" + i;
            intentEntries.put(numberAsString, str);
            i++;
          }
        }
      }
      try {
        String src = getGPClient().getBundleInfo(resourceString).getSourceLanguage();
        getGPClient().uploadResourceStrings(resourceString, src, intentEntries);
      } catch (ServiceException e) {
        e.printStackTrace();
      }
    }
  }

  /***
   * Upload Entities to GP
   * 
   * @param resourceString
   * @param uniqueBotOutputTree
   * @return
   */
  private void uploadEntities(String resourceString, Map<String, TreeSet<String>> uniqueBotOutputTree) {
    // Upload Resource Strings
    Map<String, NewResourceEntryData> intentEntries = new HashMap<String, NewResourceEntryData>(
        uniqueBotOutputTree.size());
    NewResourceEntryData newEntry = null;
    for (Entry<String, TreeSet<String>> res : uniqueBotOutputTree.entrySet()) {
      String numberAsString = res.getKey();
      TreeSet<String> intentValue = res.getValue();
      if (intentValue.size() == 1) {
        String str = intentValue.first();
        newEntry = new NewResourceEntryData(str);
        intentEntries.put(numberAsString, newEntry);
      } else {
        int i = 0;
        for (String str : intentValue) {
          newEntry = new NewResourceEntryData(str);
          numberAsString = numberAsString + ":" + i;
          intentEntries.put(numberAsString, newEntry);
          i++;
        }
      }
    }
    try {
      String src = getGPClient().getBundleInfo(resourceString).getSourceLanguage();
      getGPClient().uploadResourceEntries(resourceString, src, intentEntries);
    } catch (ServiceException e) {
      e.printStackTrace();
    }
  }

  /***
   * Get entire Watson conversation workspace
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
   * Parse Intents from Watson Conversation workspace and store translatable
   * content
   * 
   * @param jsonWCSPayload
   * @return uniqueBotOutputIntent
   */
  private Map<String, TreeSet<String>> parseWCSIntents(JsonObject jsonWCSPayload) {
    // Fetch Intents from Watson Conversation File
    System.out.println("\n");
    System.out.println("   ***** Fetching Intents *****");
    JsonArray intents = jsonWCSPayload.getAsJsonArray("intents");

    TreeMap<String, TreeSet<String>> uniqueBotOutputIntent = new TreeMap<String, TreeSet<String>>();
    if (intents.size() > 0) {
      for (JsonElement intentsArray : intents) {
        JsonObject intentObj = intentsArray.getAsJsonObject();
        JsonArray intentEx = (JsonArray) intentObj.get("examples");

        if (intentEx.size() > 0) {
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
              if (!uniqueBotOutputIntent.containsKey(result)) {
                TreeSet<String> intentSet = new TreeSet<String>();
                intentSet.add(textIntent);
                uniqueBotOutputIntent.put(result, intentSet);
              } else {
                // Collision Occurred
                uniqueBotOutputIntent.get(result).add(textIntent);
              }
            }
          }
        }
        // else if size < 0 -> no intent examples defined. Ignore...
      }
    }
    // else if size < 0 -> No Intents Defined in the WCS workspace.
    // Ignore...
    System.out.println("   Fetched Intents -> " + uniqueBotOutputIntent.size());
    return uniqueBotOutputIntent;
  }

  /***
   * Parse Entities from Watson Conversation workspace and store translatable
   * content
   * 
   * @param jsonWCSPayload
   * @return uniqueBotOutputEntity
   * @throws IOException
   */
  private Map<String, TreeSet<String>> parseWCSEntities(JsonObject jsonWCSPayload) throws IOException {
    // Fetch Entities from Watson Conversation File
    System.out.println("\n");
    System.out.println("   ***** Fetching Entities *****");
    JsonArray entities = jsonWCSPayload.getAsJsonArray("entities");
    TreeMap<String, TreeSet<String>> uniqueBotOutputEntity = new TreeMap<String, TreeSet<String>>();

    if (entities.size() > 0) {
      for (JsonElement entitiesArray : entities) {
        JsonObject entityObj = entitiesArray.getAsJsonObject();
        JsonArray valuesEx = (JsonArray) entityObj.get("values");

        if (valuesEx.size() > 0) {
          // get entity
          String entityValue = entityObj.get("entity").getAsString();
          for (JsonElement entitiesText : valuesEx) {
            JsonObject entityValObj = entitiesText.getAsJsonObject();
            // get value
            String entityVal = entityValObj.get("value").getAsString();
            String entityKey = entityValue + ":" + entityVal;

            if (!uniqueBotOutputEntity.containsKey(entityKey)) {
              TreeSet<String> entitySet = new TreeSet<String>();
              entitySet.add(entityVal);
              uniqueBotOutputEntity.put(entityKey, entitySet);
            } else {
              // Collision occurred
              uniqueBotOutputEntity.get(entityKey).add(entityVal);
            }
            entityKey = null;
            // get synonyms
            JsonArray synonymsVal = (JsonArray) entityValObj.get("synonyms");

            for (JsonElement synonymText : synonymsVal) {
              String synonymVal = synonymText.getAsString();
              entityKey = entityValue + ":" + synonymVal;
              if (!uniqueBotOutputEntity.containsKey(entityKey)) {
                TreeSet<String> entitySynonym = new TreeSet<String>();
                entitySynonym.add(synonymVal);
                uniqueBotOutputEntity.put(entityKey, entitySynonym);
              } else {
                // Collision Occurred
                uniqueBotOutputEntity.get(entityKey).add(synonymVal);
              }
              entityKey = null;
            }
          }
        }
        // else if size < 0 -> no entity values defined. Ignore...
      }
    }
    // else if size < 0 -> No Entities Defined in the WCS workspace.
    // Ignore...
    System.out.println("   Fetched Entities -> " + uniqueBotOutputEntity.size());
    return uniqueBotOutputEntity;
  }

  /***
   * Parse Dialogs from Watson Conversation workspace and store translatable
   * content
   * 
   * @param jsonWCSPayload
   * @return uniqueBotOutputDialog
   */
  private Map<String, TreeSet<String>> parseWCSDialogs(JsonObject jsonWCSPayload) {
    // Fetch Output from Dialog Nodes using Watson Conversation File
    System.out.println("\n");
    System.out.println("   ***** Fetching Dialog Nodes *****");
    JsonArray dialogNodes = jsonWCSPayload.getAsJsonArray("dialog_nodes");
    TreeMap<String, TreeSet<String>> uniqueBotOutputDialog = new TreeMap<String, TreeSet<String>>();

    if (dialogNodes.size() > 0) {
      for (JsonElement dialogArray : dialogNodes) {
        JsonObject dialogNodeObj = dialogArray.getAsJsonObject();
        JsonElement element = dialogNodeObj.get("output");
        if (!(element instanceof JsonNull)) {
          JsonObject outputEx = (JsonObject) element;
          JsonElement valuesTextEx = (JsonElement) outputEx.get("text");
          if (!(valuesTextEx instanceof JsonNull)) {
            if (valuesTextEx != null) {
              // text is either a String, JsonArray or JsonObject
              if (valuesTextEx.isJsonPrimitive()) {
                if (valuesTextEx.getAsJsonPrimitive().isString()) {
                  // text is String
                  String textVal = valuesTextEx.getAsString();
                  if (textVal != null && textVal.length() > 0) {
                    Integer textHashCode = textVal.hashCode();
                    String result = Integer.toHexString(textHashCode);
                    String append = null;
                    if (textVal.length() < 5) {
                      append = textVal.substring(0, textVal.length());
                    } else {
                      append = textVal.substring(0, 4);
                    }
                    result = append + "_" + result;
                    if (!uniqueBotOutputDialog.containsKey(result)) {
                      TreeSet<String> dialogTreeSet = new TreeSet<String>();
                      dialogTreeSet.add(textVal);
                      uniqueBotOutputDialog.put(result, dialogTreeSet);
                    } else {
                      // Collision Occurred
                      uniqueBotOutputDialog.get(result).add(textVal);
                    }
                  }
                }
              } else {
                if (valuesTextEx.isJsonArray()) {
                  // text is JsonArray
                  JsonArray jsonNodes = valuesTextEx.getAsJsonArray();
                  if (jsonNodes.size() > 0) {
                    for (JsonElement jsonArray : jsonNodes) {
                      String jsonText = jsonArray.getAsString();
                      if (jsonText != null && jsonText.length() > 0) {
                        Integer jsonHashCode = jsonText.hashCode();
                        String result = Integer.toHexString(jsonHashCode);
                        String append = null;
                        if (jsonText.length() < 5) {
                          append = jsonText.substring(0, jsonText.length());
                        } else {
                          append = jsonText.substring(0, 4);
                        }
                        result = append + "_" + result;
                        if (!uniqueBotOutputDialog.containsKey(result)) {
                          TreeSet<String> dialogJSONSet = new TreeSet<String>();
                          dialogJSONSet.add(jsonText);
                          uniqueBotOutputDialog.put(result, dialogJSONSet);
                        } else {
                          // Collision Occurred
                          uniqueBotOutputDialog.get(result).add(jsonText);
                        }
                      }
                    }
                  }
                  // else ignore empty text
                } else {
                  if (valuesTextEx.isJsonObject()) {
                    // text is JsonArray
                    JsonObject jsonTextObj = (JsonObject) valuesTextEx;
                    // Text contains values
                    JsonArray valuesNodes = jsonTextObj.getAsJsonArray("values");
                    if (valuesNodes.size() > 0) {
                      for (JsonElement valuesArray : valuesNodes) {
                        String valuesText = valuesArray.getAsString();
                        if (valuesText != null && valuesText.length() > 0) {
                          Integer valuesHashCode = valuesText.hashCode();
                          String result = Integer.toHexString(valuesHashCode);
                          String append = null;
                          if (valuesText.length() < 5) {
                            append = valuesText.substring(0, valuesText.length());
                          } else {
                            append = valuesText.substring(0, 4);
                          }
                          result = append + "_" + result;
                          if (!uniqueBotOutputDialog.containsKey(result)) {
                            TreeSet<String> dialogObjectSet = new TreeSet<String>();
                            dialogObjectSet.add(valuesText);
                            uniqueBotOutputDialog.put(result, dialogObjectSet);
                          } else {
                            // Collision Occurred
                            uniqueBotOutputDialog.get(result).add(valuesText);
                          }
                        }
                      }
                    }
                    // else ignore
                  }
                }
              }
            }
          }
        }
      }
    }
    // else if size < 0 -> No Dialog Nodes Defined in the WCS workspace.
    // Ignore...
    System.out.println("   Fetched Dialog Nodes -> " + uniqueBotOutputDialog.size());
    return uniqueBotOutputDialog;
  }
}
