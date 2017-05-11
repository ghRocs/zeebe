package org.camunda.tngp.broker.workflow.data;

public enum WorkflowInstanceEventType
{
    CREATE_WORKFLOW_INSTANCE,
    WORKFLOW_INSTANCE_CREATED,
    WORKFLOW_INSTANCE_REJECTED,

    START_EVENT_OCCURRED,
    END_EVENT_OCCURRED,

    SEQUENCE_FLOW_TAKEN,

    ACTIVITY_READY,
    ACTIVITY_ACTIVATED,
    ACTIVITY_COMPLETING,
    ACTIVITY_COMPLETED,
    ACTIVITY_TERMINATED,

    WORKFLOW_INSTANCE_COMPLETED,

    CANCEL_WORKFLOW_INSTANCE,
    WORKFLOW_INSTANCE_CANCELED,
    CANCEL_WORKFLOW_INSTANCE_REJECTED;

}
