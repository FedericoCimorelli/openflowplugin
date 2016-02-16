/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.openflowplugin.openflow.md.core.sal;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.openflowplugin.api.openflow.md.core.SwitchConnectionDistinguisher;
import org.opendaylight.openflowplugin.openflow.md.core.sal.convertor.PacketOutConvertor;
import org.opendaylight.openflowplugin.api.openflow.md.core.session.IMessageDispatchService;
import org.opendaylight.openflowplugin.openflow.md.core.session.OFSessionUtil;
import org.opendaylight.openflowplugin.api.openflow.md.core.session.SessionContext;
import org.opendaylight.openflowplugin.openflow.md.core.session.SwitchConnectionCookieOFImpl;
import org.opendaylight.openflowplugin.statistics.ONNodesStatisticsManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForGivenMatchInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsFromFlowTableInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsFromFlowTableOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupFeaturesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterFeaturesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.module.config.rev141015.SetConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.module.config.rev141015.SetConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.nodes.statistics.rev160114.OfStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.nodes.statistics.rev160114.OfStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.nodes.statistics.rev160114.ofstatistics.OfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.nodes.statistics.rev160114.ofstatistics.OfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.nodes.statistics.rev160114.ofstatistics.ofnode.Counter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.nodes.statistics.rev160114.ofstatistics.ofnode.CounterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.MultipartRequestFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.MultipartType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MultipartRequestInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PacketOutInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.multipart.request.multipart.request.body.MultipartRequestDescCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.multipart.request.multipart.request.body.MultipartRequestPortDescCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.multipart.request.multipart.request.body.MultipartRequestTableFeaturesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.multipart.request.multipart.request.body.multipart.request.table.features._case.MultipartRequestTableFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.ConnectionCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.UpdatePortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.UpdatePortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromGivenPortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromGivenPortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.UpdateTableInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.UpdateTableOutput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;

/**
 * RPC implementation of MD-switch
 */
public class ModelDrivenSwitchImpl extends AbstractModelDrivenSwitch {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ModelDrivenSwitchImpl.class);
    private final NodeId nodeId;
    private final IMessageDispatchService messageService;
    private short version = 0;
    private OFRpcTaskContext rpcTaskContext;

    // TODO:read timeout from configSubsystem
    protected long maxTimeout = 1000;
    protected TimeUnit maxTimeoutUnit = TimeUnit.MILLISECONDS;
    
    protected long onNodeStatsSlotDuration = 2*60*1000L;
    protected long onNodeStatsWriteInDatastoreTimeout = 3*60*1000L;
    protected long lastWriteInDatastoreTimestamp = 0L;
    private OfNode ofNodeStats;

    protected ModelDrivenSwitchImpl(final NodeId nodeId, final InstanceIdentifier<Node> identifier,
                                    final SessionContext sessionContext) {
        super(identifier, sessionContext);
        this.nodeId = nodeId;
        messageService = sessionContext.getMessageDispatchService();
        version = sessionContext.getPrimaryConductor().getVersion();
        final NotificationProviderService rpcNotificationProviderService = OFSessionUtil.getSessionManager().getNotificationProviderService();

        rpcTaskContext = new OFRpcTaskContext();
        rpcTaskContext.setSession(sessionContext);
        rpcTaskContext.setMessageService(messageService);
        rpcTaskContext.setRpcNotificationProviderService(rpcNotificationProviderService);
        rpcTaskContext.setMaxTimeout(maxTimeout);
        rpcTaskContext.setMaxTimeoutUnit(maxTimeoutUnit);
        rpcTaskContext.setRpcPool(OFSessionUtil.getSessionManager().getRpcPool());
        rpcTaskContext.setMessageSpy(OFSessionUtil.getSessionManager().getMessageSpy());
        
        ofNodeStats = new OfNodeBuilder().setNodeId(nodeId.getValue()).setCounter(new ArrayList<Counter>()).build();
        lastWriteInDatastoreTimestamp = 0L;
        initializeOFStatsPerSwitch();
    }

    @Override
    public Future<RpcResult<AddFlowOutput>> addFlow(final AddFlowInput input) {
        LOG.debug("Calling the FlowMod RPC method on MessageDispatchService");
        // use primary connection
        SwitchConnectionDistinguisher cookie = null;

        OFRpcTask<AddFlowInput, RpcResult<UpdateFlowOutput>> task =
                OFRpcTaskFactory.createAddFlowTask(rpcTaskContext, input, cookie);
        ListenableFuture<RpcResult<UpdateFlowOutput>> result = task.submit();
        //update OF node stitistics
        incrementSwitchOFMessageCounters("addFlow");
        return Futures.transform(result, OFRpcFutureResultTransformFactory.createForAddFlowOutput());
    }


    @Override
    public Future<RpcResult<AddGroupOutput>> addGroup(final AddGroupInput input) {
        LOG.debug("Calling the GroupMod RPC method on MessageDispatchService");

        // use primary connection
        SwitchConnectionDistinguisher cookie = null;

        OFRpcTask<AddGroupInput, RpcResult<UpdateGroupOutput>> task =
                OFRpcTaskFactory.createAddGroupTask(rpcTaskContext, input, cookie);
        ListenableFuture<RpcResult<UpdateGroupOutput>> result = task.submit();
        incrementSwitchOFMessageCounters("addGroup");
        return Futures.transform(result, OFRpcFutureResultTransformFactory.createForAddGroupOutput());
    }

    @Override
    public Future<RpcResult<AddMeterOutput>> addMeter(final AddMeterInput input) {
        LOG.debug("Calling the MeterMod RPC method on MessageDispatchService");

        // use primary connection
        SwitchConnectionDistinguisher cookie = null;

        OFRpcTask<AddMeterInput, RpcResult<UpdateMeterOutput>> task =
                OFRpcTaskFactory.createAddMeterTask(rpcTaskContext, input, cookie);
        ListenableFuture<RpcResult<UpdateMeterOutput>> result = task.submit();
        incrementSwitchOFMessageCounters("addMeter");
        return Futures.transform(result, OFRpcFutureResultTransformFactory.createForAddMeterOutput());
    }

    @Override
    public Future<RpcResult<RemoveFlowOutput>> removeFlow(final RemoveFlowInput input) {
        LOG.debug("Calling the removeFlow RPC method on MessageDispatchService");

        // use primary connection
        SwitchConnectionDistinguisher cookie = null;
        OFRpcTask<RemoveFlowInput, RpcResult<UpdateFlowOutput>> task =
                OFRpcTaskFactory.createRemoveFlowTask(rpcTaskContext, input, cookie);
        ListenableFuture<RpcResult<UpdateFlowOutput>> result = task.submit();
        //update OF node stitistics
        incrementSwitchOFMessageCounters("removeFlow");
        return Futures.transform(result, OFRpcFutureResultTransformFactory.createForRemoveFlowOutput());
    }

    @Override
    public Future<RpcResult<RemoveGroupOutput>> removeGroup(final RemoveGroupInput input) {
        LOG.debug("Calling the Remove Group RPC method on MessageDispatchService");

        SwitchConnectionDistinguisher cookie = null;
        OFRpcTask<RemoveGroupInput, RpcResult<UpdateGroupOutput>> task =
                OFRpcTaskFactory.createRemoveGroupTask(rpcTaskContext, input, cookie);
        ListenableFuture<RpcResult<UpdateGroupOutput>> result = task.submit();
        incrementSwitchOFMessageCounters("removeGroup");
        return Futures.transform(result, OFRpcFutureResultTransformFactory.createForRemoveGroupOutput());
    }

    @Override
    public Future<RpcResult<RemoveMeterOutput>> removeMeter(final RemoveMeterInput input) {
        LOG.debug("Calling the Remove MeterMod RPC method on MessageDispatchService");

        SwitchConnectionDistinguisher cookie = null;
        OFRpcTask<RemoveMeterInput, RpcResult<UpdateMeterOutput>> task =
                OFRpcTaskFactory.createRemoveMeterTask(rpcTaskContext, input, cookie);
        ListenableFuture<RpcResult<UpdateMeterOutput>> result = task.submit();
        incrementSwitchOFMessageCounters("removeMeter");
        return Futures.transform(result, OFRpcFutureResultTransformFactory.createForRemoveMeterOutput());
    }

    @Override
    public Future<RpcResult<Void>> transmitPacket(final TransmitPacketInput input) {
        LOG.debug("TransmitPacket - {}", input);
        // Convert TransmitPacket to PacketOutInput
        PacketOutInput message = PacketOutConvertor.toPacketOutInput(input, version, sessionContext.getNextXid(),
                sessionContext.getFeatures().getDatapathId());

        SwitchConnectionDistinguisher cookie = null;
        ConnectionCookie connectionCookie = input.getConnectionCookie();
        if (connectionCookie != null && connectionCookie.getValue() != null) {
            cookie = new SwitchConnectionCookieOFImpl(connectionCookie.getValue());
        }
        incrementSwitchOFMessageCounters("transmitPacket");
        return messageService.packetOut(message, cookie);
    }

    @Override
    public Future<RpcResult<UpdateFlowOutput>> updateFlow(final UpdateFlowInput input) {
        LOG.debug("Calling the updateFlow RPC method on MessageDispatchService");

        // use primary connection
        SwitchConnectionDistinguisher cookie = null;
        final ReadWriteTransaction rwTx = OFSessionUtil.getSessionManager().getDataBroker().newReadWriteTransaction();
        OFRpcTask<UpdateFlowInput, RpcResult<UpdateFlowOutput>> task =
                OFRpcTaskFactory.createUpdateFlowTask(rpcTaskContext, input, cookie, rwTx);
        ListenableFuture<RpcResult<UpdateFlowOutput>> result = task.submit();
        incrementSwitchOFMessageCounters("updateFlow");
        return result;
    }

    @Override
    public Future<RpcResult<UpdateGroupOutput>> updateGroup(final UpdateGroupInput input) {
        LOG.debug("Calling the update Group Mod RPC method on MessageDispatchService");

        // use primary connection
        SwitchConnectionDistinguisher cookie = null;

        OFRpcTask<UpdateGroupInput, RpcResult<UpdateGroupOutput>> task =
                OFRpcTaskFactory.createUpdateGroupTask(rpcTaskContext, input, cookie);
        ListenableFuture<RpcResult<UpdateGroupOutput>> result = task.submit();
        incrementSwitchOFMessageCounters("updateGroup");
        return result;
    }

    @Override
    public Future<RpcResult<UpdateMeterOutput>> updateMeter(final UpdateMeterInput input) {
        LOG.debug("Calling the MeterMod RPC method on MessageDispatchService");

        // use primary connection
        SwitchConnectionDistinguisher cookie = null;

        OFRpcTask<UpdateMeterInput, RpcResult<UpdateMeterOutput>> task =
                OFRpcTaskFactory.createUpdateMeterTask(rpcTaskContext, input, cookie);
        ListenableFuture<RpcResult<UpdateMeterOutput>> result = task.submit();
        incrementSwitchOFMessageCounters("updateMeter");
        return result;
    }

    @Override
    public NodeId getNodeId() {
        return nodeId;
    }


    @Override
    public Future<RpcResult<GetAllGroupStatisticsOutput>> getAllGroupStatistics(final GetAllGroupStatisticsInput input) {
        // use primary connection
        LOG.debug("Calling the getAllGroupStatistics RPC method on MessageDispatchService");
        SwitchConnectionDistinguisher cookie = null;

        OFRpcTask<GetAllGroupStatisticsInput, RpcResult<GetAllGroupStatisticsOutput>> task =
                OFRpcTaskFactory.createGetAllGroupStatisticsTask(rpcTaskContext, input, cookie);
        ListenableFuture<RpcResult<GetAllGroupStatisticsOutput>> result = task.submit();
        incrementSwitchOFMessageCounters("getAllGroupStatistics");
        return result;

    }

    @Override
    public Future<RpcResult<GetGroupDescriptionOutput>> getGroupDescription(final GetGroupDescriptionInput input) {
        LOG.debug("Calling the getGroupDescription RPC method on MessageDispatchService");

        OFRpcTask<GetGroupDescriptionInput, RpcResult<GetGroupDescriptionOutput>> task =
                OFRpcTaskFactory.createGetGroupDescriptionTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getGroupDescription");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetGroupFeaturesOutput>> getGroupFeatures(final GetGroupFeaturesInput input) {
        LOG.debug("Calling the getGroupFeatures RPC method on MessageDispatchService");

        OFRpcTask<GetGroupFeaturesInput, RpcResult<GetGroupFeaturesOutput>> task =
                OFRpcTaskFactory.createGetGroupFeaturesTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getGroupFeatures");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetGroupStatisticsOutput>> getGroupStatistics(final GetGroupStatisticsInput input) {
        LOG.debug("Calling the getGroupStatistics RPC method on MessageDispatchService");

        OFRpcTask<GetGroupStatisticsInput, RpcResult<GetGroupStatisticsOutput>> task =
                OFRpcTaskFactory.createGetGroupStatisticsTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getGroupStatistics");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetAllMeterConfigStatisticsOutput>> getAllMeterConfigStatistics(
            final GetAllMeterConfigStatisticsInput input) {
        LOG.debug("Calling the getAllMeterConfigStatistics RPC method on MessageDispatchService");

        OFRpcTask<GetAllMeterConfigStatisticsInput, RpcResult<GetAllMeterConfigStatisticsOutput>> task =
                OFRpcTaskFactory.createGetAllMeterConfigStatisticsTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getAllMeterConfigStatistics");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetAllMeterStatisticsOutput>> getAllMeterStatistics(
            final GetAllMeterStatisticsInput input) {
        LOG.debug("Calling the getAllMeterStatistics RPC method on MessageDispatchService");

        OFRpcTask<GetAllMeterStatisticsInput, RpcResult<GetAllMeterStatisticsOutput>> task =
                OFRpcTaskFactory.createGetAllMeterStatisticsTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getAllMeterStatistics");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetMeterFeaturesOutput>> getMeterFeatures(
            final GetMeterFeaturesInput input) {
        LOG.debug("Calling the getMeterFeatures RPC method on MessageDispatchService");

        OFRpcTask<GetMeterFeaturesInput, RpcResult<GetMeterFeaturesOutput>> task =
                OFRpcTaskFactory.createGetMeterFeaturesTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getMeterFeatures");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetMeterStatisticsOutput>> getMeterStatistics(
            final GetMeterStatisticsInput input) {
        LOG.debug("Calling the getMeterStatistics RPC method on MessageDispatchService");

        OFRpcTask<GetMeterStatisticsInput, RpcResult<GetMeterStatisticsOutput>> task =
                OFRpcTaskFactory.createGetMeterStatisticsTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getMeterStatistics");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetAllNodeConnectorsStatisticsOutput>> getAllNodeConnectorsStatistics(
            final GetAllNodeConnectorsStatisticsInput input) {
        LOG.debug("Calling the getAllNodeConnectorsStatistics RPC method on MessageDispatchService");

        OFRpcTask<GetAllNodeConnectorsStatisticsInput, RpcResult<GetAllNodeConnectorsStatisticsOutput>> task =
                OFRpcTaskFactory.createGetAllNodeConnectorsStatisticsTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getAllNodeConnectorsStatistics");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetNodeConnectorStatisticsOutput>> getNodeConnectorStatistics(
            final GetNodeConnectorStatisticsInput input) {
        LOG.debug("Calling the getNodeConnectorStatistics RPC method on MessageDispatchService");

        OFRpcTask<GetNodeConnectorStatisticsInput, RpcResult<GetNodeConnectorStatisticsOutput>> task =
                OFRpcTaskFactory.createGetNodeConnectorStatisticsTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getNodeConnectorStatistics");
        return task.submit();
    }

    @Override
    public Future<RpcResult<UpdatePortOutput>> updatePort(final UpdatePortInput input) {
        LOG.debug("Calling the updatePort RPC method on MessageDispatchService");

        OFRpcTask<UpdatePortInput, RpcResult<UpdatePortOutput>> task =
                OFRpcTaskFactory.createUpdatePortTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("updatePort");
        return task.submit();
    }

    @Override
    public Future<RpcResult<UpdateTableOutput>> updateTable(final UpdateTableInput input) {
        LOG.debug("Calling the updateTable RPC method on MessageDispatchService");

        OFRpcTask<UpdateTableInput, RpcResult<UpdateTableOutput>> task =
                OFRpcTaskFactory.createUpdateTableTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("updateTable");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetAllFlowStatisticsFromFlowTableOutput>> getAllFlowStatisticsFromFlowTable(
            final GetAllFlowStatisticsFromFlowTableInput input) {
        LOG.debug("Calling the getAllFlowStatisticsFromFlowTable RPC method on MessageDispatchService");

        OFRpcTask<GetAllFlowStatisticsFromFlowTableInput, RpcResult<GetAllFlowStatisticsFromFlowTableOutput>> task =
                OFRpcTaskFactory.createGetAllFlowStatisticsFromFlowTableTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getAllFlowStatisticsFromFlowTable");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetAllFlowsStatisticsFromAllFlowTablesOutput>> getAllFlowsStatisticsFromAllFlowTables(
            final GetAllFlowsStatisticsFromAllFlowTablesInput input) {
        LOG.debug("Calling the getAllFlowsStatisticsFromAllFlowTables RPC method on MessageDispatchService");

        OFRpcTask<GetAllFlowsStatisticsFromAllFlowTablesInput, RpcResult<GetAllFlowsStatisticsFromAllFlowTablesOutput>> task =
                OFRpcTaskFactory.createGetAllFlowsStatisticsFromAllFlowTablesTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getAllFlowsStatisticsFromAllFlowTables");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetFlowStatisticsFromFlowTableOutput>> getFlowStatisticsFromFlowTable(
            final GetFlowStatisticsFromFlowTableInput input) {
        LOG.debug("Calling the getFlowStatisticsFromFlowTable RPC method on MessageDispatchService");

        OFRpcTask<GetFlowStatisticsFromFlowTableInput, RpcResult<GetFlowStatisticsFromFlowTableOutput>> task =
                OFRpcTaskFactory.createGetFlowStatisticsFromFlowTableTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getFlowStatisticsFromFlowTable");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput>> getAggregateFlowStatisticsFromFlowTableForAllFlows(
            final GetAggregateFlowStatisticsFromFlowTableForAllFlowsInput input) {
        LOG.debug("Calling the getAggregateFlowStatisticsFromFlowTableForAllFlows RPC method on MessageDispatchService");

        OFRpcTask<GetAggregateFlowStatisticsFromFlowTableForAllFlowsInput, RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput>> task =
                OFRpcTaskFactory.createGetAggregateFlowStatisticsFromFlowTableForAllFlowsTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getAggregateFlowStatisticsFromFlowTableForAllFlows");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput>> getAggregateFlowStatisticsFromFlowTableForGivenMatch(
            final GetAggregateFlowStatisticsFromFlowTableForGivenMatchInput input) {
        LOG.debug("Calling the getAggregateFlowStatisticsFromFlowTableForGivenMatch RPC method on MessageDispatchService");

        OFRpcTask<GetAggregateFlowStatisticsFromFlowTableForGivenMatchInput, RpcResult<GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput>> task =
                OFRpcTaskFactory.createGetAggregateFlowStatisticsFromFlowTableForGivenMatchTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getAggregateFlowStatisticsFromFlowTableForGivenMatch");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetFlowTablesStatisticsOutput>> getFlowTablesStatistics(
            final GetFlowTablesStatisticsInput input) {
        LOG.debug("Calling the getFlowTablesStatistics RPC method on MessageDispatchService");

        OFRpcTask<GetFlowTablesStatisticsInput, RpcResult<GetFlowTablesStatisticsOutput>> task =
                OFRpcTaskFactory.createGetFlowTablesStatisticsTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getFlowTablesStatistics");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetAllQueuesStatisticsFromAllPortsOutput>> getAllQueuesStatisticsFromAllPorts(
            final GetAllQueuesStatisticsFromAllPortsInput input) {
        LOG.debug("Calling the getAllQueuesStatisticsFromAllPorts RPC method on MessageDispatchService");

        OFRpcTask<GetAllQueuesStatisticsFromAllPortsInput, RpcResult<GetAllQueuesStatisticsFromAllPortsOutput>> task =
                OFRpcTaskFactory.createGetAllQueuesStatisticsFromAllPortsTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getAllQueuesStatisticsFromAllPorts");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetAllQueuesStatisticsFromGivenPortOutput>> getAllQueuesStatisticsFromGivenPort(
            final GetAllQueuesStatisticsFromGivenPortInput input) {
        LOG.debug("Calling the getAllQueuesStatisticsFromGivenPort RPC method on MessageDispatchService");

        OFRpcTask<GetAllQueuesStatisticsFromGivenPortInput, RpcResult<GetAllQueuesStatisticsFromGivenPortOutput>> task =
                OFRpcTaskFactory.createGetAllQueuesStatisticsFromGivenPortTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getAllQueuesStatisticsFromGivenPort");
        return task.submit();
    }

    @Override
    public Future<RpcResult<GetQueueStatisticsFromGivenPortOutput>> getQueueStatisticsFromGivenPort(
            final GetQueueStatisticsFromGivenPortInput input) {
        LOG.debug("Calling the getQueueStatisticsFromGivenPort RPC method on MessageDispatchService");

        OFRpcTask<GetQueueStatisticsFromGivenPortInput, RpcResult<GetQueueStatisticsFromGivenPortOutput>> task =
                OFRpcTaskFactory.createGetQueueStatisticsFromGivenPortTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("getQueueStatisticsFromGivenPort");
        return task.submit();
    }

    @Override
    public Future<RpcResult<SetConfigOutput>> setConfig(SetConfigInput input) {
        OFRpcTask<SetConfigInput, RpcResult<SetConfigOutput>> task = OFRpcTaskFactory.createSetNodeConfigTask(rpcTaskContext, input, null);
        incrementSwitchOFMessageCounters("setConfig");
        return task.submit();
    }
    @Override
    public Optional<BigInteger> sendEmptyTableFeatureRequest() {
        LOG.debug("Send table feature request to {}",nodeId);

        final Long xid = rpcTaskContext.getSession().getNextXid();

        MultipartRequestTableFeaturesCaseBuilder caseBuilder = new MultipartRequestTableFeaturesCaseBuilder();
        MultipartRequestTableFeaturesBuilder requestBuilder = new MultipartRequestTableFeaturesBuilder();
        caseBuilder.setMultipartRequestTableFeatures(requestBuilder.build());

        MultipartRequestInputBuilder mprInput = new MultipartRequestInputBuilder();
        mprInput.setType(MultipartType.OFPMPTABLEFEATURES);
        mprInput.setVersion(rpcTaskContext.getSession().getPrimaryConductor().getVersion());
        mprInput.setXid(xid);
        mprInput.setFlags(new MultipartRequestFlags(false));

        mprInput.setMultipartRequestBody(caseBuilder.build());

        Future<RpcResult<Void>> resultFromOFLib = rpcTaskContext.getMessageService()
                .multipartRequest(mprInput.build(), null);

        return Optional.of(BigInteger.valueOf(xid));

    }

    @Override
    public void requestSwitchDetails(){
        // post-handshake actions
        if (version == OFConstants.OFP_VERSION_1_3) {
            requestPorts();
        }

        requestDesc();
    }

    /*
     * Send an OFPMP_DESC request message to the switch
     */
    private void requestDesc() {
        MultipartRequestInputBuilder builder = new MultipartRequestInputBuilder();
        builder.setType(MultipartType.OFPMPDESC);
        builder.setVersion(version);
        builder.setFlags(new MultipartRequestFlags(false));
        builder.setMultipartRequestBody(new MultipartRequestDescCaseBuilder()
                .build());
        builder.setXid(getSessionContext().getNextXid());
        rpcTaskContext.getSession().getPrimaryConductor().getConnectionAdapter().multipartRequest(builder.build());
    }

    private void requestPorts() {
        MultipartRequestInputBuilder builder = new MultipartRequestInputBuilder();
        builder.setType(MultipartType.OFPMPPORTDESC);
        builder.setVersion(version);
        builder.setFlags(new MultipartRequestFlags(false));
        builder.setMultipartRequestBody(new MultipartRequestPortDescCaseBuilder()
                .build());
        builder.setXid(getSessionContext().getNextXid());
        rpcTaskContext.getSession().getPrimaryConductor().getConnectionAdapter().multipartRequest(builder.build());
    }
    
    
    private void initializeOFStatsPerSwitch() {
        DataBroker dataBroker = OFSessionUtil.getSessionManager().getDataBroker();
        try{
            LOG.info("OF Node stats initialization...");
            InstanceIdentifier<OfStatistics> NODEOFSTATS_IID = InstanceIdentifier.builder(OfStatistics.class).build();
            
            List<String> l = new ArrayList<String>();
            l.add("0");
            OfStatistics nodeOFStatistics = new OfStatisticsBuilder()
            		.setOfNode(new ArrayList<OfNode>())
            		.build(); 
            WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
            wtx.merge(LogicalDatastoreType.OPERATIONAL, NODEOFSTATS_IID, nodeOFStatistics, true);
            wtx.submit();
            LOG.info("OF Node stats initialization success!!");           
        }
        catch(Exception e){
            LOG.error(e.getMessage());
        }
    }   
    
    
    
    private void incrementSwitchOFMessageCounters(final String msgType) {
    	  	
    	boolean found = false;
    	Counter odlCounter = null, newCounter = null;
    	
		for(Counter c : ofNodeStats.getCounter()){
			if(c.getMsgType().equals(msgType)){
				found=true;
				odlCounter = c;
				if(System.currentTimeMillis()-c.getCounterFirstPacketTs().longValue()>onNodeStatsSlotDuration){
					//timeout, update last counter, initialize new counter
					newCounter = new CounterBuilder()
							.setMsgType(c.getMsgType())
							.setCounterCount(BigInteger.ONE)
							.setCounterFirstPacketTs(BigInteger.valueOf(System.currentTimeMillis()))
							.setLastCounterCount(c.getCounterCount())
							.setLastCounterFirstPacketTs(c.getCounterFirstPacketTs())
							.build();
				}
				else{
					//update counter
					newCounter = new CounterBuilder()
							.setMsgType(c.getMsgType())
							.setCounterCount(c.getCounterCount().add(BigInteger.ONE))
							.setCounterFirstPacketTs(c.getCounterFirstPacketTs())
							.build();
				}
			}
		}
		if(!found){
			newCounter = new CounterBuilder()
					.setMsgType(msgType)
					.setCounterCount(BigInteger.ONE)
					.setCounterFirstPacketTs(BigInteger.valueOf(System.currentTimeMillis()))
					.build();
		}
    	
		ofNodeStats.getCounter().remove(odlCounter);
		ofNodeStats.getCounter().add(newCounter);
		
		//ONNodesStatisticsManager.ofNodesStatistics.add(ofNodeStats);
		//LOG.info("QWERTY5: "+ONNodesStatisticsManager.ofNodesStatistics.toString());
		
		LOG.info(System.currentTimeMillis()+"");
		LOG.info((System.currentTimeMillis()-lastWriteInDatastoreTimestamp)+"");
		long offset = System.currentTimeMillis()-lastWriteInDatastoreTimestamp;
		
		if(offset > onNodeStatsWriteInDatastoreTimeout){
			lastWriteInDatastoreTimestamp = System.currentTimeMillis();
			writeOFStatsUpdateInDataStore();
		}
    }

    
    
	private void writeOFStatsUpdateInDataStore() {
		final DataBroker dataBroker = OFSessionUtil.getSessionManager().getDataBroker();
		try{
		    LOG.info("Writing OF nodes stats to Datastore...");
		    final InstanceIdentifier<OfStatistics> NODEOFSTATS_IID = InstanceIdentifier.builder(OfStatistics.class).build();
		    ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
		    ListenableFuture<Optional<OfStatistics>> dataFuture = readTx.read(LogicalDatastoreType.OPERATIONAL, NODEOFSTATS_IID);
		    Futures.addCallback(dataFuture, new FutureCallback<Optional<OfStatistics>>() {          
		    	@Override
		        public void onSuccess(final Optional<OfStatistics> result) {
		            if(result.isPresent()) {
		            	OfStatistics ofs = result.get();
		            	OfNode ofn = cointains(ofs.getOfNode(), getNodeId());
		            	if(ofn==null){		
		            		LOG.info("DataStore doesn't cointains an entry for node "+getNodeId().getValue()+", creating");
		            		ofs.getOfNode().add(ofNodeStats);
		            	}
		            	else{
		            		LOG.info("DataStore already cointains an entry for node "+getNodeId().getValue()+", updating");
		            		ofs.getOfNode().remove(ofn);
		            		ofs.getOfNode().add(ofNodeStats);
		            	}         	
		            	WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
		                wtx.merge(LogicalDatastoreType.OPERATIONAL, NODEOFSTATS_IID, ofs, true);
		                wtx.submit();
		                LOG.info("Writing changes to DataStore for node "+getNodeId().getValue());
		            } 
		            else {
		            	LOG.info("OF Node stats not properly initialized");
		            }
		        }                
				@Override
		        public void onFailure(final Throwable t) {
		        	LOG.info("OF Node stats, read from datastore failed");
		        }
		    });
		}
		catch(Exception e){
		    LOG.error(e.getMessage());
		}
	} 

    
    
    private OfNode cointains(List<OfNode>  ofNodes, NodeId nodeId) {
		for(OfNode n : ofNodes)
			if(n.getNodeId().equals(nodeId.getValue()))
				return n;
		
		return null;
	}

}
