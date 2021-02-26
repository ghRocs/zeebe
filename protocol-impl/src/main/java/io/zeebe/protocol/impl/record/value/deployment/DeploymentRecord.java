/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.protocol.impl.record.value.deployment;

import io.zeebe.msgpack.property.ArrayProperty;
import io.zeebe.msgpack.value.ValueArray;
import io.zeebe.protocol.impl.record.UnifiedRecordValue;
import io.zeebe.protocol.record.value.DeploymentRecordValue;
import io.zeebe.protocol.record.value.deployment.DeployedProcess;
import java.util.ArrayList;
import java.util.List;
import org.agrona.concurrent.UnsafeBuffer;

public final class DeploymentRecord extends UnifiedRecordValue implements DeploymentRecordValue {

  public static final String RESOURCES = "resources";
  public static final String PROCESSS = "deployedProcesss";

  private final ArrayProperty<DeploymentResource> resourcesProp =
      new ArrayProperty<>(RESOURCES, new DeploymentResource());

  private final ArrayProperty<ProcessRecord> processsProp =
      new ArrayProperty<>(PROCESSS, new ProcessRecord());

  public DeploymentRecord() {
    declareProperty(resourcesProp).declareProperty(processsProp);
  }

  public ValueArray<ProcessRecord> processs() {
    return processsProp;
  }

  public ValueArray<DeploymentResource> resources() {
    return resourcesProp;
  }

  @Override
  public List<io.zeebe.protocol.record.value.deployment.DeploymentResource> getResources() {
    final List<io.zeebe.protocol.record.value.deployment.DeploymentResource> resources =
        new ArrayList<>();

    for (final DeploymentResource resource : resourcesProp) {
      final byte[] bytes = new byte[resource.getLength()];
      final UnsafeBuffer copyBuffer = new UnsafeBuffer(bytes);
      resource.write(copyBuffer, 0);

      final DeploymentResource copiedResource = new DeploymentResource();
      copiedResource.wrap(copyBuffer);
      resources.add(copiedResource);
    }

    return resources;
  }

  @Override
  public List<DeployedProcess> getDeployedProcesss() {
    final List<DeployedProcess> processs = new ArrayList<>();

    for (final ProcessRecord processRecord : processsProp) {
      final byte[] bytes = new byte[processRecord.getLength()];
      final UnsafeBuffer copyBuffer = new UnsafeBuffer(bytes);
      processRecord.write(copyBuffer, 0);

      final ProcessRecord copiedProcessRecord = new ProcessRecord();
      copiedProcessRecord.wrap(copyBuffer);
      processs.add(copiedProcessRecord);
    }

    return processs;
  }
}
