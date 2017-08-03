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
package org.smartdata.hdfs.metric.fetcher;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.server.balancer.ExitStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartdata.SmartContext;
import org.smartdata.actions.hdfs.HdfsAction;
import org.smartdata.actions.hdfs.MoveFileAction;
import org.smartdata.actions.hdfs.move.MoverStatus;
import org.smartdata.hdfs.HadoopUtil;
import org.smartdata.metastore.ActionPreProcessService;
import org.smartdata.metastore.MetaStore;
import org.smartdata.model.LaunchAction;
import org.smartdata.model.actions.hdfs.SchedulePlan;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class MoverPreProcessService extends ActionPreProcessService {
  private DFSClient client;
  private MoverStatus moverStatus;
  private MoverProcessor processor;
  private URI nnUri;

  public static final Logger LOG =
      LoggerFactory.getLogger(MoverPreProcessService.class);

  public MoverPreProcessService(SmartContext context, MetaStore metaStore)
      throws IOException {
    super(context, metaStore);
    nnUri = HadoopUtil.getNameNodeUri(getContext().getConf());
  }

  public void init() throws IOException {
    this.client = new DFSClient(nnUri, getContext().getConf());
    moverStatus = new MoverStatus();
  }

  /**
   * After start call, all services and public calls should work.
   * @return
   * @throws IOException
   */
  public void start() throws IOException {
    // TODO: Will be removed when MetaStore part finished
    DatanodeStorageReportProcTask task = new DatanodeStorageReportProcTask(client);
    task.run();
    processor = new MoverProcessor(client, task.getStorages(), moverStatus);
  }

  /**
   * After stop call, all states in database will not be changed anymore.
   * @throws IOException
   */
  public void stop() throws IOException {
  }

  private static final List<String> actions = Arrays.asList("allssd", "onessd", "archive");
  public List<String> getSupportedActions() {
    return actions;
  }

  public void beforeExecution(LaunchAction action) {
    if (!actions.contains(action.getActionType())) {
      return;
    }

    String file = action.getArgs().get(HdfsAction.FILE_PATH);
    if (file == null) {
      return;
    }

    try {
      ExitStatus exitStatus = processor.processNamespace(new Path(file));
      if (exitStatus == ExitStatus.SUCCESS) {
        SchedulePlan plan = processor.getSchedulePlan();
        plan.setNamenode(nnUri);
        action.getArgs().put(MoveFileAction.MOVE_PLAN, plan.toString());
      }
    } catch (IOException e) {
      LOG.error("Exception while processing " + action, e);
    }
  }

  public void afterExecution(LaunchAction action) {

  }
}
