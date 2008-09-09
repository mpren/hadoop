/**
 * Copyright 2008 The Apache Software Foundation
 *
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
package org.apache.hadoop.hbase.master;

import java.io.IOException;

import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.RemoteExceptionHandler;

/** Scanner for the <code>ROOT</code> HRegion. */
class RootScanner extends BaseScanner {
  /**
   * Constructor
   * 
   * @param master
   * @param regionManager
   */
  public RootScanner(HMaster master, RegionManager regionManager) {
    super(master, regionManager, true, master.metaRescanInterval, master.closed);
  }

  // Don't retry if we get an error while scanning. Errors are most often
  // caused by the server going away. Wait until next rescan interval when
  // things should be back to normal
  private boolean scanRoot() {
    boolean scanSuccessful = false;
    master.waitForRootRegionLocation();
    if (master.closed.get()) {
      return scanSuccessful;
    }

    try {
      // Don't interrupt us while we're working
      synchronized(scannerLock) {
        if (master.getRootRegionLocation() != null) {
          scanRegion(new MetaRegion(master.getRootRegionLocation(),
            HRegionInfo.ROOT_REGIONINFO.getRegionName()));
        }
      }
      scanSuccessful = true;
    } catch (IOException e) {
      e = RemoteExceptionHandler.checkIOException(e);
      LOG.warn("Scan ROOT region", e);
      // Make sure the file system is still available
      master.checkFileSystem();
    } catch (Exception e) {
      // If for some reason we get some other kind of exception, 
      // at least log it rather than go out silently.
      LOG.error("Unexpected exception", e);
    }
    return scanSuccessful;
  }

  @Override
  protected boolean initialScan() {
    initialScanComplete = scanRoot();
    return initialScanComplete;
  }

  @Override
  protected void maintenanceScan() {
    scanRoot();
  }
}