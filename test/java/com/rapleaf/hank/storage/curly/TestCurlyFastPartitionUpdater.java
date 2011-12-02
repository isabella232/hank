/**
 *  Copyright 2011 Rapleaf
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.rapleaf.hank.storage.curly;

import com.rapleaf.hank.compress.CompressionCodec;
import com.rapleaf.hank.compress.NoCompressionCodec;
import com.rapleaf.hank.coordinator.Domain;
import com.rapleaf.hank.coordinator.DomainVersion;
import com.rapleaf.hank.coordinator.mock.MockDomain;
import com.rapleaf.hank.coordinator.mock.MockDomainVersion;
import com.rapleaf.hank.storage.IncrementalPartitionUpdaterTestCase;
import com.rapleaf.hank.storage.IncrementalUpdatePlan;
import com.rapleaf.hank.storage.LocalPartitionRemoteFileOps;
import com.rapleaf.hank.storage.cueball.MockCueballMerger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestCurlyFastPartitionUpdater extends IncrementalPartitionUpdaterTestCase {

  private final DomainVersion v0 = new MockDomainVersion(0, 0l);
  private final DomainVersion v1 = new MockDomainVersion(1, 0l);
  private final DomainVersion v2 = new MockDomainVersion(2, 0l);
  private final Domain domain = new MockDomain("domain") {
    @Override
    public DomainVersion getVersionByNumber(int versionNumber) {
      switch (versionNumber) {
        case 0:
          return v0;
        case 1:
          return v1;
        case 2:
          return v2;
        default:
          throw new RuntimeException("Unknown version: " + versionNumber);
      }
    }
  };
  private CurlyFastPartitionUpdater updater;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    int keyHashSize = 12;
    int offsetSize = 5;
    int hashIndexBits = 1;
    MockCueballMerger cueballMerger = new MockCueballMerger();
    MockCurlyMerger curlyMerger = new MockCurlyMerger();
    CompressionCodec compressionCodec = new NoCompressionCodec();
    this.updater = new CurlyFastPartitionUpdater(domain,
        new LocalPartitionRemoteFileOps(remotePartitionRoot, 0),
        curlyMerger,
        cueballMerger,
        keyHashSize,
        offsetSize,
        hashIndexBits,
        compressionCodec,
        localPartitionRoot);

    if (!new File(updateWorkRoot).mkdir()) {
      throw new IOException("Failed to create update work root");
    }
  }

  public void testUpdate() throws IOException {
    // Updating from v0 to v2
    List<DomainVersion> deltas = new ArrayList<DomainVersion>();
    deltas.add(v1);
    deltas.add(v2);
    // Fail when missing files
    try {
      updater.runUpdateCore(v0, v2, new IncrementalUpdatePlan(v0, deltas), updateWorkRoot);
      fail("Should fail");
    } catch (IOException e) {
      // Good
    }
    // Success merging with deltas
    assertFalse(existsUpdateWorkFile("00002.base.cueball"));
    assertFalse(existsUpdateWorkFile("00002.base.curly"));
    makeLocalFile("00000.base.cueball");
    makeLocalFile("00000.base.curly");
    makeLocalCacheFile("00001.delta.cueball");
    makeLocalCacheFile("00001.delta.curly");
    makeLocalCacheFile("00002.delta.cueball");
    makeLocalCacheFile("00002.delta.curly");

    // Make sure file exists on remote partition so that the versions are not considered empty
    makeRemoteFile("0/00001.delta.cueball");
    makeRemoteFile("0/00001.delta.curly");
    makeRemoteFile("0/00002.delta.cueball");
    makeRemoteFile("0/00002.delta.curly");

    updater.runUpdateCore(v0, v2, new IncrementalUpdatePlan(v0, deltas), updateWorkRoot);
    // Deltas still exist
    assertTrue(existsCacheFile("00001.delta.cueball"));
    assertTrue(existsCacheFile("00002.delta.cueball"));
    // New base created
    assertTrue(existsUpdateWorkFile("00002.base.cueball"));
    assertTrue(existsUpdateWorkFile("00002.base.curly"));
    // Old Cueball base still exists
    assertTrue(existsLocalFile("00000.base.cueball"));
    // Old Curly base has disappeared
    assertFalse(existsLocalFile("00000.base.curly"));
  }
}
