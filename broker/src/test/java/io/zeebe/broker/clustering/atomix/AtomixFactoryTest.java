/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.broker.clustering.atomix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.atomix.core.Atomix;
import io.atomix.raft.partition.RaftPartitionGroup;
import io.atomix.raft.partition.RaftPartitionGroupConfig;
import io.zeebe.broker.system.configuration.BrokerCfg;
import io.zeebe.snapshots.broker.impl.FileBasedSnapshotStoreFactory;
import io.zeebe.util.Environment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class AtomixFactoryTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private Environment environment;

  @Before
  public void setUp() {
    environment = new Environment();
  }

  @Test
  public void shouldDisableExplicitFlush() {
    // given
    final var brokerConfig = newConfig();
    brokerConfig.getExperimental().setDisableExplicitRaftFlush(true);

    // when
    final var atomix =
        AtomixFactory.fromConfiguration(brokerConfig, mock(FileBasedSnapshotStoreFactory.class));

    // then
    final var config = getPartitionGroupConfig(atomix);
    assertThat(config.getStorageConfig().shouldFlushExplicitly()).isFalse();
  }

  @Test
  public void shouldEnableExplicitFlush() {
    // given
    final var brokerConfig = newConfig();
    brokerConfig.getExperimental().setDisableExplicitRaftFlush(false);

    // when
    final var atomix =
        AtomixFactory.fromConfiguration(brokerConfig, mock(FileBasedSnapshotStoreFactory.class));

    // then
    final var config = getPartitionGroupConfig(atomix);
    assertThat(config.getStorageConfig().shouldFlushExplicitly()).isTrue();
  }

  private RaftPartitionGroup getPartitionGroup(final Atomix atomix) {
    return (RaftPartitionGroup)
        atomix.getPartitionService().getPartitionGroup(AtomixFactory.GROUP_NAME);
  }

  private RaftPartitionGroupConfig getPartitionGroupConfig(final Atomix atomix) {
    return (RaftPartitionGroupConfig) getPartitionGroup(atomix).config();
  }

  private BrokerCfg newConfig() {
    final var config = new BrokerCfg();
    config.init(temporaryFolder.getRoot().getAbsolutePath(), environment);

    return config;
  }
}
