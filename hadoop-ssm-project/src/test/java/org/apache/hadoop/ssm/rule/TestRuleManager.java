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
package org.apache.hadoop.ssm.rule;

import org.apache.hadoop.ssm.sql.CommandInfo;
import org.apache.hadoop.ssm.sql.DBAdapter;
import org.apache.hadoop.ssm.sql.FileStatusInternal;
import org.apache.hadoop.ssm.sql.TestDBUtil;
import org.apache.hadoop.ssm.sql.Util;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.Random;

/**
 * Testing RuleManager service.
 */
public class TestRuleManager {
  private RuleManager ruleManager;
  private DBAdapter dbAdapter;

  @Before
  public void init() throws Exception {
    String dbFile = TestDBUtil.getUniqueDBFilePath();
    Connection conn = null;
    try {
      conn = Util.createSqliteConnection(dbFile);
      Util.initializeDataBase(conn);
      dbAdapter = new DBAdapter(conn);
      // TODO: to be fixed
      ruleManager = new RuleManager(null, null, dbAdapter);
    } finally {
      File file = new File(dbFile);
      file.deleteOnExit();
    }
  }

  @Test
  public void testSubmitNewActiveRule() throws Exception {
    String rule = "file: every 1s \n | length > 300 | cachefile";
    long id = ruleManager.submitRule(rule, RuleState.ACTIVE);
    RuleInfo ruleInfo = ruleManager.getRuleInfo(id);
    Assert.assertTrue(ruleInfo.getRuleText().equals(rule));
    RuleInfo info = ruleInfo;
    for (int i = 0; i < 5; i++) {
      Thread.sleep(1000);
      info = ruleManager.getRuleInfo(id);
      System.out.println(info);
    }

    Assert.assertTrue(info.getNumChecked()
        - ruleInfo.getNumChecked() > 3);
  }

  @Test
  public void testSubmitDeletedRule() throws Exception {
    String rule = "file: every 1s \n | length > 300 | cachefile";
    try {
      long id = ruleManager.submitRule(rule, RuleState.DELETED);
    } catch (IOException e) {
      Assert.assertTrue(e.getMessage().contains("Invalid initState"));
    }
  }

  @Test
  public void testSubmitNewDisabledRule() throws Exception {
    String rule = "file: every 1s \n | length > 300 | cachefile";
    long id = ruleManager.submitRule(rule, RuleState.DISABLED);
    RuleInfo ruleInfo = ruleManager.getRuleInfo(id);
    Assert.assertTrue(ruleInfo.getRuleText().equals(rule));
    RuleInfo info = ruleInfo;
    for (int i = 0; i < 5; i++) {
      Thread.sleep(1000);
      info = ruleManager.getRuleInfo(id);
      System.out.println(info);
    }

    Assert.assertTrue(info.getNumChecked()
        - ruleInfo.getNumChecked() == 0);
  }

  @Test
  public void testSubmitAutoEndsRule() throws Exception {
    String rule = "file: every 1s from now to now + 2s \n | "
        + "length > 300 | cachefile";

    long id = ruleManager.submitRule(rule, RuleState.ACTIVE);
    RuleInfo ruleInfo = ruleManager.getRuleInfo(id);
    Assert.assertTrue(ruleInfo.getRuleText().equals(rule));
    RuleInfo info = ruleInfo;
    for (int i = 0; i < 5; i++) {
      Thread.sleep(1000);
      info = ruleManager.getRuleInfo(id);
      System.out.println(info);
    }

    Assert.assertTrue(info.getState() == RuleState.FINISHED);
    Assert.assertTrue(info.getNumChecked()
        - ruleInfo.getNumChecked() <= 3);
  }

  @Test
  public void testStopRule() throws Exception {
    String rule = "file: every 1s from now to now + 100s \n | "
        + "length > 300 | cachefile";

    long id = ruleManager.submitRule(rule, RuleState.ACTIVE);
    RuleInfo ruleInfo = ruleManager.getRuleInfo(id);
    Assert.assertTrue(ruleInfo.getRuleText().equals(rule));
    RuleInfo info = ruleInfo;
    for (int i = 0; i < 2; i++) {
      Thread.sleep(1000);
      info = ruleManager.getRuleInfo(id);
      System.out.println(info);
    }

    ruleManager.DeleteRule(ruleInfo.getId(), true);
    Thread.sleep(3000);

    RuleInfo endInfo = ruleManager.getRuleInfo(info.getId());
    System.out.println(endInfo);

    Assert.assertTrue(endInfo.getState() == RuleState.DELETED);
    Assert.assertTrue(endInfo.getNumChecked()
        - info.getNumChecked() <= 1);
  }

  @Test
  public void testResumeRule() throws Exception {
    String rule = "file: every 1s from now to now + 100s \n | "
        + "length > 300 | cachefile";

    long id = ruleManager.submitRule(rule, RuleState.ACTIVE);
    RuleInfo ruleInfo = ruleManager.getRuleInfo(id);
    Assert.assertTrue(ruleInfo.getRuleText().equals(rule));
    RuleInfo info = ruleInfo;
    for (int i = 0; i < 2; i++) {
      Thread.sleep(1000);
      info = ruleManager.getRuleInfo(id);
      System.out.println(info);
    }
    Assert.assertTrue(info.getNumChecked()
        > ruleInfo.getNumChecked());

    ruleManager.DisableRule(ruleInfo.getId(), true);
    Thread.sleep(1000);
    RuleInfo info2 = ruleManager.getRuleInfo(id);
    for (int i = 0; i < 3; i++) {
      Thread.sleep(1000);
      info = ruleManager.getRuleInfo(id);
      System.out.println(info);
    }
    Assert.assertTrue(info.getNumChecked()
        == info2.getNumChecked());

    RuleInfo info3 = info;
    ruleManager.ActivateRule(ruleInfo.getId());
    for (int i = 0; i < 3; i++) {
      Thread.sleep(1000);
      info = ruleManager.getRuleInfo(id);
      System.out.println(info);
    }
    Assert.assertTrue(info.getNumChecked()
        > info3.getNumChecked());
  }

  @Test
  public void testSubmitNewMultiRules() throws Exception {
    String rule = "file: every 1s \n | length > 300 | cachefile";

    // id increasing
    int nRules =  3;
    long[] ids = new long[nRules];
    for (int i = 0; i < nRules; i++) {
      ids[i] = ruleManager.submitRule(rule, RuleState.DISABLED);
      System.out.println(ruleManager.getRuleInfo(ids[i]));
      if (i > 0) {
        Assert.assertTrue(ids[i] - ids[i - 1] == 1);
      }
    }

    for (int i = 0; i < nRules; i++) {
      ruleManager.DeleteRule(ids[i], true);
      RuleInfo info = ruleManager.getRuleInfo(ids[i]);
      Assert.assertTrue(info.getState() == RuleState.DELETED);
    }

    long[] ids2 = new long[nRules];
    for (int i = 0; i < nRules; i++) {
      ids2[i] = ruleManager.submitRule(rule, RuleState.DISABLED);
      System.out.println(ruleManager.getRuleInfo(ids2[i]));
      if (i > 0) {
        Assert.assertTrue(ids2[i] - ids2[i - 1] == 1);
      }
      Assert.assertTrue(ids2[i] > ids[nRules - 1]);
    }

    System.out.println("\nFinal state:");
    List<RuleInfo> allRules = ruleManager.getRuleInfo();
    Assert.assertTrue(allRules.size() == 2 * nRules);
    for (RuleInfo info : allRules) {
      System.out.println(info);
    }
  }

  @Test
  public void testMultiThreadUpdate() throws Exception {
    String rule = "file: every 1s \n | length > 10 | cachefile";

    long now = System.currentTimeMillis();

    long rid = ruleManager.submitRule(rule, RuleState.DISABLED);
    ruleManager.updateRuleInfo(rid, null, now, 1, 1);

    long start = System.currentTimeMillis();

    Thread[] threads = new Thread[] {
        new Thread(new RuleInfoUpdater(rid, 3)),
        new Thread(new RuleInfoUpdater(rid, 7)),
        new Thread(new RuleInfoUpdater(rid, 11)),
        new Thread(new RuleInfoUpdater(rid, 17)),} ;

    for (Thread t : threads) {
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }

    long end = System.currentTimeMillis();
    System.out.println("Time used = " + (end - start) + " ms");

    RuleInfo res = ruleManager.getRuleInfo(rid);
    System.out.println(res);
  }

  private class RuleInfoUpdater implements Runnable {
    private long ruleid;
    private int index;

    public RuleInfoUpdater(long ruleid, int index) {
      this.ruleid = ruleid;
      this.index = index;
    }

    @Override
    public void run() {
      long lastCheckTime;
      long checkedCount;
      int commandsGen;
      try {
        for (int i = 0; i < 1000; i++) {
          RuleInfo info = ruleManager.getRuleInfo(ruleid);
          lastCheckTime = System.currentTimeMillis();
          checkedCount = info.getNumChecked();
          commandsGen = (int)info.getNumCmdsGen();
          System.out.println("" + index + ": " + lastCheckTime + " " + checkedCount + " " + commandsGen);
          Assert.assertTrue(checkedCount == commandsGen);
          ruleManager.updateRuleInfo(ruleid, null, lastCheckTime, index, index);
        }
      } catch (Exception e) {
        Assert.fail("Can not have exception here.");
      }
    }
  }

  @Test
  public void testMultiThreadChangeState() throws Exception {
    String rule = "file: every 1s \n | length > 10 | cachefile";

    long now = System.currentTimeMillis();

    long length = 100;
    long fid = 10000;
    FileStatusInternal[] files = { new FileStatusInternal(length, false, 3,
        1024, now, now, null, null, null, null,
        "testfile".getBytes(), "/tmp", fid, 0, null, (byte)3, null) };

    dbAdapter.insertFiles(files);
    long rid = ruleManager.submitRule(rule, RuleState.ACTIVE);
    //ruleManager.updateRuleInfo(rid, null, now, 1, 1);

    long start = System.currentTimeMillis();

    int nThreads = 10;
    Thread[] threads = new Thread[nThreads];
    for (int i = 0; i< nThreads; i++) {
      threads[i] = new Thread(new StateChangeWorker(rid));
    }

    for (Thread t : threads) {
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }

    long end = System.currentTimeMillis();
    System.out.println("Time used = " + (end - start) + " ms");
    Thread.sleep(1000); // This is needed due to async threads

    RuleInfo res = ruleManager.getRuleInfo(rid);
    System.out.println(res);
    Thread.sleep(5000);
    RuleInfo after = ruleManager.getRuleInfo(rid);
    System.out.println(after);
    if (res.getState() == RuleState.ACTIVE) {
      Assert.assertTrue(after.getNumCmdsGen() - res.getNumCmdsGen() <= 6);
    } else {
      Assert.assertTrue(after.getNumCmdsGen() == res.getNumCmdsGen());
    }
  }

  private class StateChangeWorker implements Runnable {
    private long ruleId;

    public StateChangeWorker(long ruleId) {
      this.ruleId = ruleId;
    }

    @Override
    public void run() {
      Random r = new Random();
      try {
        for (int i = 0; i < 1000; i++) {
          int rand = r.nextInt() % 2;
          //System.out.println(rand == 0 ? "Active" : "Disable");
          switch (rand) {
            case 0:
              ruleManager.ActivateRule(ruleId);
              break;
            case 1:
              ruleManager.DisableRule(ruleId, true);
              break;
          }
        }
      } catch (Exception e) {
        Assert.fail("Should not happen!");
      }
    }
  }
}
