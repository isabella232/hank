/**
 *  Copyright 2012 Rapleaf
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

package com.rapleaf.hank.coordinator.zk;

import com.rapleaf.hank.coordinator.*;
import com.rapleaf.hank.generated.DomainGroupMetadata;
import com.rapleaf.hank.zookeeper.WatchedMap;
import com.rapleaf.hank.zookeeper.WatchedThriftNode;
import com.rapleaf.hank.zookeeper.ZkPath;
import com.rapleaf.hank.zookeeper.ZooKeeperPlus;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class NewZkDomainGroup extends AbstractDomainGroup implements DomainGroup {

  private static final String VERSIONS_PATH = "v";

  private final ZooKeeperPlus zk;
  private final String name;
  private final String path;
  private final WatchedThriftNode<DomainGroupMetadata> metadata;
  private final WatchedMap<NewZkDomainGroupVersion> versions;

  public static NewZkDomainGroup create(final ZooKeeperPlus zk,
                                        final String rootPath,
                                        final String name,
                                        final Coordinator coordinator) throws InterruptedException, KeeperException, IOException {
    String path = ZkPath.append(rootPath, name);
    DomainGroupMetadata initialValue = new DomainGroupMetadata();
    initialValue.set_next_version_number(0);
    return new NewZkDomainGroup(zk, path, coordinator, true, initialValue);
  }

  public NewZkDomainGroup(final ZooKeeperPlus zk,
                          final String path,
                          final Coordinator coordinator) throws IOException, InterruptedException, KeeperException {
    this(zk, path, coordinator, false, null);
  }

  public NewZkDomainGroup(final ZooKeeperPlus zk,
                          final String path,
                          final Coordinator coordinator,
                          final boolean create,
                          final DomainGroupMetadata initialValue)
      throws InterruptedException, KeeperException, IOException {
    super(coordinator);
    this.zk = zk;
    this.path = path;
    this.name = ZkPath.getFilename(path);
    this.metadata = new WatchedThriftNode<DomainGroupMetadata>(zk, path, true, create, initialValue, new DomainGroupMetadata());
    if (create) {
      zk.create(ZkPath.append(path, VERSIONS_PATH), null);
    }
    this.versions = new WatchedMap<NewZkDomainGroupVersion>(zk, ZkPath.append(path, VERSIONS_PATH),
        new WatchedMap.ElementLoader<NewZkDomainGroupVersion>() {
          @Override
          public NewZkDomainGroupVersion load(ZooKeeperPlus zk, String basePath, String relPath) throws KeeperException, InterruptedException, IOException {
            return new NewZkDomainGroupVersion(zk, coordinator, ZkPath.append(basePath, relPath), NewZkDomainGroup.this);
          }
        });
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SortedSet<DomainGroupVersion> getVersions() throws IOException {
    return new TreeSet<DomainGroupVersion>(versions.values());
  }

  @Override
  public NewZkDomainGroupVersion createNewVersion(Map<Domain, Integer> domainToVersion) throws IOException {
    // First, copy next version number
    int versionNumber = metadata.get().get_next_version_number();
    // Then, increment next version counter
    try {
      metadata.update(metadata.new Updater() {
        @Override
        public void updateCopy(DomainGroupMetadata currentCopy) {
          currentCopy.set_next_version_number(currentCopy.get_next_version_number() + 1);
        }
      });
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (KeeperException e) {
      throw new RuntimeException(e);
    }
    try {
      return NewZkDomainGroupVersion.create(zk,
          getCoordinator(),
          ZkPath.append(path, VERSIONS_PATH),
          versionNumber,
          domainToVersion,
          this);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public boolean delete() throws IOException {
    try {
      zk.deleteNodeRecursively(path);
      return true;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public String toString() {
    return "ZkDomainGroup [name=" + getName() + "]";
  }

  public String getPath() {
    return path;
  }
}