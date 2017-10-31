<!--
/*
 * Copyright IBM Corp. 2017
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
-->

Watson Conversation Globalization CLI Tool
============================================
This is a Command Line Tool/Utility for converting [Watson Conversation](https://www.ibmwatsonconversation.com/) workspaces to other languages. 

## Diagram

![diagram](./images/conv-gp-updated.png)

The tool consists of two utilities. 

* The first utility, WCS_TO_GP (Watson Conversation to Globalization) will extract all translatable content from Watson Conversation JSON file and upload it [Globalization Pipeline](https://console.ng.bluemix.net/catalog/services/globalization-pipeline/) Bluemix service. 

* The second utility, GP_TO_WCS (Globalization to Watson COnversation) will replace all translatable content in target language and create/update watson conversation workspace.

The Globalization Pipeline service makes it easy for you to provide your global customers with Bluemix applications translated into the languages in which they work. 

This utility currently supports JAVA

* [Overview](#TOC-Overview)
* [Prerequisites](#TOC-Prerequisites)
* [Command Reference](#TOC-Command-Reference)
* [Help Command](#TOC-Cmd-Help)
* [Log File](#TOC-Log)
* [Tips](#TOC-Tips)
* [Work Flow Guidance](#TOC-Guidance)
* [Globalization Pipeline](#TOC-Globalization-Pipeline)

---
## <a name="TOC-Overview"></a>Overview

Watson Conversation Globalization CLI (Command Line Interface) Tool is designed for
translating watson conversation json files from a source language to a given target language using Globalization Pipeline Service
on command line. Read about it [here](https://www.ibm.com/blogs/bluemix/2017/10/translate-watson-conversation-chatbots/) or watch the [video](https://youtu.be/vauDA1h1mbY) to get started.

---
## <a name="TOC-Prerequisites"></a>Prerequisites

* Watson Conversation Globalization CLI Tool is distributed in two jar packages
    1. gp-watson-conversation-X.X.X-SNAPSHOT.jar
    2. gp-watson-conversation-X.X.X-SNAPSHOT-with-dependencies.jar
* You need Java SE Runtime Environment 8 or later version to run the tool.
* You need to create Watson Conversation Service and Globalization Pipeline service on IBM Bluemix.
* Create a `WCS-credentials.json` file with the credentials
as given in the Watson Conversation service on Bluemix:

      {
          "username": "…",
          "password": "……",
      }
* Create a `GP-credentials.json` file with the credentials
as given in the bound service on Bluemix:

      {
          "url": "https://…",
          "userId": "…",
          "password": "……",
          "instanceId": "………"
      }
 
You can access these credentials on Bluemix Dashboard.

---
## <a name="TOC-Command-Reference"></a>Command Reference

First specify the utility you want to run followed by following command line params
   * WCS_TO_GP
   * GP_TO_WCS

| Param | Type | Optional/Required | Description |
| --- | --- | --- | --- |
| -s  (--sourceworkspaceId) | String | Required | WCS source workspace ID |
| -v (--versionDate) | String | Required | Watson Conversation API version Date |
| -t (--targetLanguage) | String | Required | Target Language to translate WCS |
| -w (--targetWorkspaceID) | String | Optional | Target Workspace ID for WCS |
| -b (--bundleId) | String | Optional | Bundle Id for Globalization Pipeline |
| -j (--jsonWCSCreds) | String | Required | Watson Conversation Credentials file |
| -g (--jsonGPCreds) | String | Required | Globalization Pipeline Credentials file |


1. If Bundle Id is not provided, workspaceId will be used to create Bundles or to fetch translatable content from Globalization Pipeline
2. If target workspace id is not provided then new Conversation workspace would be created on WCS by GP_TO_WCS utility
3. if target workspace id provided, existing workspace would be updated with translated content on by GP_TO_WCS utility
4. Provide list of target languages separated by comma for WCS_TO_GP Utility
5. Provide only one target language for GP_To_WCS utility

For example,
```
java -jar gp-watson-conversation-0.0.1-SNAPSHOT-with-dependencies.jar wcs_to_gp -s xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxxx -v 2017-05-26 -j WCS-credentials.json -g GP-credentials.json -t fr,es
```
or 
```
java -jar gp-watson-conversation-0.0.1-SNAPSHOT-with-dependencies.jar gp_to_wcs -s xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxxx -v 2017-05-26 -j WCS-credentials.json -g GP-credentials.json -t fr
```
---
### <a name="TOC-Cmd-Help"></a>Help Command

The help command prints out all commands and available options.
```
java -jar gp-watson-conversation-0.0.1-SNAPSHOT-with-dependencies.jar help
```

---
### <a name="TOC-Log"></a>Log File

* When GP_TO_WCS utility runs, it will generate a gp_to_wcs.log file. The file will contain information such as which intents,entities or dialog nodes exceed size limit of 64 in target language or if these were not translated by GP. The data in this log will not be uploaded to WCS. The user should fix the problems as mentioned in the log file before re-running the utility.

---
### <a name="TOC-Tips"></a>Tips
1. Watson Conversation Service currently allows only 20 workspaces. If you have more than 20, you will get response code 400 when using GP_TO_WCS utility. Delete at least one of the workspaces in order to create a new workspace using GP_TO_WCS utility.

2. If you are running the tool for the first time after you have built the conversational workspace then the tool will handle all the translatable content. 

   It will create bundles on GP and limit the size of each bundle to a threshold value. The size is currently limited to a threshold because of Performance reasons.The current threshold value is 300 but can be easily modified.

   Separate Bundles would be created on GP for intents , entities and dialog nodes.
   For example, 
   Following bundles would be created on GP (size of each bundle is limited to a threshold value):
   ```
   <BundleId or workspaceId>-intents-1
   <BundleId or workspaceId>-intents-2 
   ...
   <BundleId or workspaceId>-entities-1
   <BundleId or workspaceId>-entities-2
   ...
   <BundleId or workspaceId>-dialogs-1
   <BundleId or workspaceId>-dialogs-2
   ...
   ```
   
3. After running wcs_to_gp utility, wait for some time before you run gp_to_wcs utility. Monitor the status of translated resource strings (key value pairs) on GP and run gp_to_wcs utility after all the resource strings have been translated.
   
---
### <a name="TOC-Guidance"></a>Work Flow Guidance
1. Translating Literals
   Let's assume you have Entity called @response_no and one of it's value is no in WCS. Now, while creating the dialog flow, a user can enter a condition based on the input or a value, for example
   ```
   "conditions": "@response_no:no"
   ```
   
   The entire dialog node structure looks like:
   ```
   {
		"title": null,
		"output": {
			"text": {
				"values": ["Ok, I won't cancel your policy. Is there anything else I can help with?"],
				"selection_policy": "sequential"
			}
		},
		"parent": "node_5_1485801143609",
		"context": null,
		"created": "2017-01-30T18:34:50.345Z",
		"updated": "2017-02-02T21:42:04.860Z",
		"metadata": null,
		"next_step": null,
		"conditions": "@response_no:no",
		"description": null,
		"dialog_node": "node_7_1485801290107",
		"previous_sibling": null
	}
	```

    The Conversation Globalization tool will handle such cases as follows:
    1. WCS_TO_GP Utility -> Will fetch 'no' from Entity Values and upload to GP. The resource string (no in this case) will be translated by GP (say in French).
    2. GP_TO_WCS utility -> Will traverse Dialog Nodes to find all those conditions where literals are present. The Utility will then update the literals with translated content.
	
    The updated dialog node will look like:
	```
	"conditions": "@response_no:aucun"
	```
	
	The entire dialog node structure looks like:
   ```
   {
		"title": null,
		"output": {
			"text": {
				"values": ["Ok, je n'annulerai pas votre règle. Y a-t-il quelque chose d'autre pour lequel je peux aider?"],
				"selection_policy": "sequential"
			}
		},
		"parent": "node_5_1485801143609",
		"context": null,
		"created": "2017-01-30T18:34:50.345Z",
		"updated": "2017-02-02T21:42:04.860Z",
		"metadata": null,
		"next_step": null,
		"conditions": "@response_no:aucun",
		"description": null,
		"dialog_node": "node_7_1485801290107",
		"previous_sibling": null
	}
	```

2. Handling Duplicates in WCS
   1. Duplicate Intent values, entities and dialog Nodes will be handled by the Utility and only unique intents, entities and dialog output will be uploaded to GP.

3. Generate Small Bundles on GP
   1. For Performance Reasons, multiple small bundles (not exceeding a threshold value) are created on GP. This threshold value can be easily modified but not recommended.

4. Mapping between source and target workspace
    1. Source and Target workspace will also be symmetric (in sink)
    2. If changes are made in source workspace, the target workspace and GP will be updated accordingly (only after running both the utilities).

5. The target workspace would be created in a format sourceWorkspaceName-targetlanguage
   1. For example,
      1. Source Workspace Name -> Car Dashboard
      2. Target Language -> fr (French)
   
      3. Target Workspace Name -> Car Dashboard-fr

6. If the user re-runs GP_TO_WCS utility again and again with same sourceWorkspaceName and same target language then multiple target workspaces would be created with same name 
   
   * for example: sourceWorkspaceName-targetlanguage (assuming no target workspace id is provided)
   
   * WCS allows you to have multiple workspaces with same name (because workspace Id would be unique)     
   * The user can quickly consume the limit of creating new workspaces on WCS (Currently the limit is 20)

7. Any modifications made in the Target workspace will not be reflected in the source workspace (Not supported).

8. Modifications to translatable content can be made in the following ways:
   * Manually edit translatable content/resource strings on GP.
   * Download the JSON/JAVA Properties or AMD file from GP, edit it and upload it back.
   * Use HPE (Human Post Editing) feature of GP which provides review by professional translators. 
   
9. Only Output from Dialog Nodes in WCS will be translated, Context is not translated as of now.

10. Sentence Translation or Translating WCS output from dialog node is just a step but translating entire application for global customers is much more complicated process. GP enables developers to integrate translation processes into their build and deploy infrastructure and translate their apps using machine translation.

11. User can specify any amount target languages. If the target language does not exist on GP, the target language will be added to all the bundles present on GP for source workspace.

---
### <a name="TOC-Globalization-Pipeline"></a>Globalization Pipeline
IBM Globalization Pipeline is a DevOps integrated application translation management service that you can use to rapidly translate and release cloud and mobile applications to your global customers. Access IBM Globalization Pipeline capabilities through its dashboard, RESTful API, or integrate it seamlessly into your application's Delivery Pipeline.

There are a number of [SDKs/Plug-ins](https://developer.ibm.com/open/openprojects/ibm-bluemix-globalization-pipeline/) available for Globalization Pipeline service.

---

# Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

# License

Apache 2.0. See [LICENSE.txt](License.txt).

> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
> http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.


