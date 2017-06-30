/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartdata.integration.rest;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.Matchers;

import java.util.List;
import java.util.Map;

public class ActionRestApi extends RestApiBase {
  // submit an action using action type and arguments
  public static long submitAction(String actionType, String args) {
    Response action = RestAssured.post(CMDLETROOT +
        "/submit/" + actionType + "?" + "args=" + args);
    action.then().body("status", Matchers.equalTo("CREATED"));
    return new JsonPath(action.asString()).getLong("body");
  }

  // get aids of a cmdlet
  public static List<Long> getActionIds(long cid) {
    Response cmdletInfo = RestAssured.get(CMDLETROOT + "/" + cid + "/info");
    JsonPath cmdletInfoPath = new JsonPath(cmdletInfo.asString());
    return (List)cmdletInfoPath.getMap("body").get("aids");
  }

  // get action info
  public static Map getActionInfo(long aid) {
    Response actionInfo = RestAssured.get(ACTIONROOT + "/" + aid + "/info");
    JsonPath actionInfoPath = new JsonPath(actionInfo.asString());
    Map actionInfoMap = actionInfoPath.getMap("body");
    return actionInfoMap;
  }

  public static List<String> getActionsSupported() {
    Response response = RestAssured.get(ACTIONROOT + "/registry/list");
    return response.jsonPath().getList("body");
  }
}
