/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ssm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.protocol.CacheDirectiveEntry;
import org.apache.hadoop.hdfs.protocol.CacheDirectiveInfo;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorageReport;

/**
 * Created by cc on 17-3-1.
 */
public class CacheStatusReport {
  private Configuration conf;
  private CacheStatus.nodeCacheInfo nodeCacheInfo;
  private CacheStatus cacheStatus;
  private Map<String, List<CacheStatus.cacheFileInfo>> reportMap;
  private Map<String, CacheStatus.nodeCacheInfo> dnCacheReportMap;
  private DFSClient dfsClient;

  public CacheStatusReport(DFSClient client, Configuration conf) {
    this.conf = conf;
    this.dfsClient = client;
    reportMap = new HashMap<>();
    cacheStatus = new CacheStatus();
    nodeCacheInfo = cacheStatus.new nodeCacheInfo();
    dnCacheReportMap = new HashMap<String, CacheStatus.nodeCacheInfo>();
  }

  /**
   * getCacheStatusReport : Get a CacheStatus report
   */
  public CacheStatus getCacheStatusReport() {
    long cacheCapacity;
    long cacheUsed;
    long cacheRemaining;
    float cacheUsedPercentage;
    long cacheCapacityTotal = 0;
    long cacheUsedTotal = 0;
    long cacheRemaTotal = 0;
    float cacheUsedPerTotal = 0;
    try {
      int len = dfsClient.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.LIVE).length;
      DatanodeStorageReport dnStorageReport;
      //get info from each dataNode
      for (int i = 0; i < len; i++) {
        dnStorageReport = dfsClient.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.LIVE)[i];
        cacheCapacity = dnStorageReport.getDatanodeInfo().getCacheCapacity();
        cacheUsed = dnStorageReport.getDatanodeInfo().getCacheUsed();
        cacheRemaining = dnStorageReport.getDatanodeInfo().getCacheRemaining();
        cacheUsedPercentage = dnStorageReport.getDatanodeInfo().getCacheUsedPercent();
        nodeCacheInfo.setCacheCapacity(cacheCapacity);
        nodeCacheInfo.setCacheUsed(cacheUsed);
        nodeCacheInfo.setCacheRemaining(cacheRemaining);
        nodeCacheInfo.setCacheUsedPercentage(cacheUsedPercentage);
        //Each host name cannot be equal
        dnCacheReportMap.put(dnStorageReport.getDatanodeInfo().getHostName(), nodeCacheInfo);
        //summary
        cacheCapacityTotal += cacheCapacity;
        cacheUsedTotal += cacheUsed;
        cacheRemaTotal += cacheRemaining;
        cacheUsedPerTotal += cacheUsedPercentage;
      }
      cacheStatus.setCacheCapacityTotal(cacheCapacityTotal);
      cacheStatus.setCacheUsedTotal(cacheUsedTotal);
      cacheStatus.setCacheRemainingTotal(cacheRemaTotal);
      cacheStatus.setCacheUsedPercentageTotal(cacheUsedPerTotal);
      //get the cacheStatusMap
      String poolName;
      String path;
      Short replication;
      CacheStatus.cacheFileInfo cacheFileInfo = cacheStatus.new cacheFileInfo();
      CacheDirectiveInfo filter = new CacheDirectiveInfo.Builder().build();
      RemoteIterator<CacheDirectiveEntry> remoteIterator = dfsClient.listCacheDirectives(filter);
      while (remoteIterator.hasNext()) {
        poolName = dfsClient.listCacheDirectives(filter).next().getInfo().getPool();
        path = dfsClient.listCacheDirectives(filter).next().getInfo().getPath().toString();
        replication = dfsClient.listCacheDirectives(filter).next().getInfo().getReplication();
        if (reportMap.containsKey(poolName)) {
          List<CacheStatus.cacheFileInfo> list = reportMap.get(poolName);
          cacheFileInfo.setFilePath(path);
          cacheFileInfo.setRepliNum(replication);
          list.add(cacheFileInfo);
          reportMap.put(poolName, list);
        } else {
          cacheFileInfo.setFilePath(path);
          cacheFileInfo.setRepliNum(replication);
          List<CacheStatus.cacheFileInfo> list = new ArrayList<>();
          list.add(cacheFileInfo);
          reportMap.put(poolName, list);
        }
        remoteIterator.next();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    cacheStatus.setCacheStatusMap(reportMap);
    cacheStatus.setdnCacheStatusMap(dnCacheReportMap);
    return cacheStatus;
  }
}