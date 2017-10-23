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

/***
 * The base class of Conversation Globalization's Exception
 * 
 * @author harpreet
 */

public class WCSWorkspaceException extends Exception {

  private static final long serialVersionUID = 1L;

  public WCSWorkspaceException() {
    super();
  }

  public WCSWorkspaceException(String message) {
    super(message);
  }

  public WCSWorkspaceException(String message, Throwable cause) {
    super(message, cause);
  }

  public WCSWorkspaceException(Throwable cause) {
    super(cause);
  }

}
