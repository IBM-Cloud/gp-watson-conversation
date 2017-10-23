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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

/***
 * Watson Conversation Globalization command interface main entry
 * 
 * @author Harpreet Chawla (hchawla@us.ibm.com)
 *
 */
public class ConversationUtility {

  @Parameters(commandDescription = "Display command help")
  private static class HelpCmd {
  }

  public static void main(String[] args) throws Exception {

    ConversationUtility conversationCmd = new ConversationUtility();
    JCommander jCommander = new JCommander(conversationCmd);

    jCommander.setProgramName("Watson Conversation Globalization");
    jCommander.addCommand("help", new HelpCmd());

    jCommander.addCommand("WCS_To_GP", new WCS_To_GP(), "wcs_to_gp");
    jCommander.addCommand("GP_To_WCS", new GP_To_WCS(), "gp_to_wcs");

    try {
      jCommander.parse(args);
      String parsedCommand = jCommander.getParsedCommand();
      System.out.println(parsedCommand);
      if (parsedCommand == null || parsedCommand.equalsIgnoreCase("help")) {
        jCommander.usage();
      } else {
        BaseUtility parsedCmd = (BaseUtility) jCommander.getCommands().get(parsedCommand).getObjects().get(0);
        parsedCmd.execute();
      }
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      jCommander.usage();
      System.exit(1);
    }
  }
}
