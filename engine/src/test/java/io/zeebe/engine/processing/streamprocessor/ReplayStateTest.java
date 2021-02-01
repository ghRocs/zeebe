/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.processing.streamprocessor;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.db.DbKey;
import io.zeebe.db.DbValue;
import io.zeebe.engine.processing.streamprocessor.StreamProcessor.Phase;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.util.EngineRule;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.intent.JobIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.test.util.record.RecordingExporter;
import io.zeebe.test.util.record.RecordingExporterTestWatcher;
import io.zeebe.util.buffer.BufferUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public final class ReplayStateTest {

  @Rule public final EngineRule engine = EngineRule.singlePartition();

  @Rule
  public final RecordingExporterTestWatcher recordingExporterTestWatcher =
      new RecordingExporterTestWatcher();

  private long workflowInstanceKey;
  private Map<ZbColumnFamilies, Map<Object, Object>> processingState;

  @Before
  public void setup() {
    engine
        .deployment()
        .withXmlResource(
            Bpmn.createExecutableProcess("process")
                .startEvent()
                .serviceTask("task", t -> t.zeebeJobType("test"))
                .done())
        .deploy();

    workflowInstanceKey = engine.workflowInstance().ofBpmnProcessId("process").create();

    RecordingExporter.workflowInstanceRecords(WorkflowInstanceIntent.ELEMENT_ACTIVATED)
        .withElementType(BpmnElementType.SERVICE_TASK)
        .await();

    RecordingExporter.jobRecords(JobIntent.CREATED).await();

    processingState = collectState(engine.getZeebeState());
    engine.stop();
  }

  private Map<ZbColumnFamilies, Map<Object, Object>> collectState(
      final io.zeebe.engine.state.ZeebeState zeebeState) {

    final var keyInstance = new VersatileBlob();
    final var valueInstance = new VersatileBlob();

    return Arrays.stream(ZbColumnFamilies.values())
        .collect(
            Collectors.toMap(
                Function.identity(),
                column -> {
                  final var entries = new HashMap<>();
                  zeebeState.visit(
                      column,
                      keyInstance,
                      valueInstance,
                      (key, value) ->
                          entries.put(
                              Arrays.toString(
                                  BufferUtil.cloneBuffer(key.getDirectBuffer())
                                      .byteArray()), // the key is written as plain bytes
                              MsgPackConverter.convertToJson(value.getDirectBuffer())));
                  return entries;
                }));
  }

  @Test
  public void shouldContinueAfterRestart() {
    // given
    engine.start();

    // when
    engine
        .jobs()
        .withType("test")
        .activate()
        .getValue()
        .getJobKeys()
        .forEach(jobKey -> engine.job().withKey(jobKey).complete());

    // then
    assertThat(
            RecordingExporter.workflowInstanceRecords()
                .withWorkflowInstanceKey(workflowInstanceKey)
                .filterRootScope()
                .limitToWorkflowInstanceCompleted())
        .extracting(Record::getIntent)
        .contains(WorkflowInstanceIntent.ELEMENT_COMPLETED);
  }

  @Test
  public void shouldRestoreState() {
    // when
    engine.start();

    Awaitility.await()
        .untilAsserted(
            () -> {
              assertThat(engine.getStreamProcessor(1).getCurrentPhase().join())
                  .isEqualTo(Phase.PROCESSING);
            });

    // then
    final var replayState = collectState(engine.getZeebeState());

    final var softly = new SoftAssertions();

    processingState.forEach(
        (column, processingEntries) -> {
          final var replayEntries = replayState.get(column);

          softly
              .assertThat(replayEntries)
              .describedAs("The state column '%s' has different entries after replay", column)
              .containsExactlyInAnyOrderEntriesOf(processingEntries);
        });

    softly.assertAll();
  }

  private static final class VersatileBlob implements DbKey, DbValue {

    private final DirectBuffer genericBuffer = new UnsafeBuffer(0, 0);

    @Override
    public void wrap(final DirectBuffer buffer, final int offset, final int length) {
      genericBuffer.wrap(buffer, offset, length);
    }

    @Override
    public int getLength() {
      return genericBuffer.capacity();
    }

    @Override
    public void write(final MutableDirectBuffer buffer, final int offset) {
      buffer.putBytes(offset, genericBuffer, 0, genericBuffer.capacity());
    }

    public DirectBuffer getDirectBuffer() {
      return genericBuffer;
    }
  }
}