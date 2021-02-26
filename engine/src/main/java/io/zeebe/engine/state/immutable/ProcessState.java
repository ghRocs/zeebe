/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.state.immutable;

import io.zeebe.engine.processing.deployment.model.element.ExecutableFlowElement;
import io.zeebe.engine.state.deployment.DeployedProcess;
import java.util.Collection;
import org.agrona.DirectBuffer;

public interface ProcessState {

  DeployedProcess getLatestProcessVersionByProcessId(DirectBuffer processId);

  DeployedProcess getProcessByProcessIdAndVersion(DirectBuffer processId, int version);

  DeployedProcess getProcessByKey(long key);

  Collection<DeployedProcess> getProcesss();

  Collection<DeployedProcess> getProcesssByBpmnProcessId(DirectBuffer bpmnProcessId);

  DirectBuffer getLatestVersionDigest(DirectBuffer processId);

  <T extends ExecutableFlowElement> T getFlowElement(
      long processKey, DirectBuffer elementId, Class<T> elementType);
}
