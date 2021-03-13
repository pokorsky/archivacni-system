/*
 * Copyright (C) 2012 Jan Pokorsky
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.webapp.client.ds;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.shared.GWT;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.PromptStyle;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.BooleanItem;
import cz.cas.lib.proarc.common.workflow.model.WorkflowModelConsts;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import cz.cas.lib.proarc.webapp.client.Editor;
import cz.cas.lib.proarc.webapp.client.action.DeleteAction.Deletable;
import cz.cas.lib.proarc.webapp.client.action.administration.RestoreAction.Restorable;
import cz.cas.lib.proarc.webapp.client.ds.ImportBatchDataSource.BatchRecord;
import cz.cas.lib.proarc.webapp.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.cas.lib.proarc.webapp.client.widget.StatusView;
import cz.cas.lib.proarc.webapp.client.widget.UserRole;
import cz.cas.lib.proarc.webapp.shared.rest.DigitalObjectResourceApi;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectDataSource extends ProarcDataSource {

    public static final String ID = "DigitalObjectDataSource";
    public static final String FIELD_PID = DigitalObjectResourceApi.DIGITALOBJECT_PID;
    public static final String FIELD_MODEL = DigitalObjectResourceApi.DIGITALOBJECT_MODEL;
    public static final String FIELD_MODS = DigitalObjectResourceApi.NEWOBJECT_XML_PARAM;
    public static final String FIELD_WF_JOB_ID = DigitalObjectResourceApi.WORKFLOW_JOB_ID;
    /** Synthetic attribute holding {@link DigitalObject}. */
    private static final String FIELD_INSTANCE = "DIGITALOBJECT_INSTANCE";

    public DigitalObjectDataSource() {
        setID(ID);
        setDataURL(RestConfig.URL_DIGOBJECT);

        DataSourceTextField pid = new DataSourceTextField(FIELD_PID);
        pid.setPrimaryKey(true);

        DataSourceTextField model = new DataSourceTextField(FIELD_MODEL);
        setFields(pid, model);

        setOperationBindings(RestConfig.createAddOperation(), RestConfig.createDeleteOperation());
        setRequestProperties(RestConfig.createRestRequest(getDataFormat()));
    }

    public static DigitalObjectDataSource getInstance() {
        DigitalObjectDataSource ds = (DigitalObjectDataSource) DataSource.get(ID);
        return  ds != null ? ds : new DigitalObjectDataSource();
    }

    /**
     * @see cz.cas.lib.proarc.webapp.client.action.DeleteAction
     */
    public static Deletable<Record> createDeletable() {
        return new Deletable<Record>() {

            @Override
            public void delete(Object[] items) {
                delete(items, null);
            }

            @Override
            public void delete(Object[] items, Record options) {
                if (items != null && items.length > 0) {
                    String[] pids = ClientUtils.toFieldValues((Record[]) items, DigitalObjectDataSource.FIELD_PID);
                    DigitalObjectDataSource.getInstance().delete(pids,
                            options != null ? options.toMap() : Collections.emptyMap());
                }
            }
        };
    }

    public static Restorable<Record> createRestorable() {
        return new Restorable<Record>() {

            @Override
            public void restore(Object[] items) {
                restore(items, null);
            }

            @Override
            public void restore(Object[] items, Record options) {
                if (items != null && items.length > 0) {
                    String[] pids = ClientUtils.toFieldValues((Record[]) items, DigitalObjectDataSource.FIELD_PID);
                    DigitalObjectDataSource.getInstance().restore(pids,
                            options != null ? options.toMap() : Collections.emptyMap());
                }
            }
        };
    }

    public static DynamicForm createDeleteOptionsForm() {
        if (!(Editor.getInstance().hasPermission("proarc.permission.admin") || Editor.getInstance().hasPermission(UserRole.ROLE_SUPERADMIN))) {
            return null;
        }
        ClientMessages i18n = GWT.create(ClientMessages.class);
        DynamicForm f = new DynamicForm();
        BooleanItem opPermanently = new BooleanItem(DigitalObjectResourceApi.DELETE_PURGE_PARAM,
                i18n.DigitalObjectDeleteAction_OptionPermanently_Title());
        opPermanently.setTooltip(i18n.DigitalObjectDeleteAction_OptionPermanently_Hint());
        f.setFields(opPermanently);
        f.setAutoHeight();
        return f;
    }

    /**
     * Deletes list of digital objects.
     * <p>For now it marks whole object hierarchy with state Deleted.
     * @param pids digital object IDs
     */
    public void delete(String[] pids) {
        delete(pids, Collections.emptyMap());
    }

    public void saveNewDigitalObject(String modelId, String pid, String mods, Long workflowJobId, Callback<String, ErrorSavingDigitalObject> callback) {
        Record r = new Record();
        DigitalObjectDataSource ds = DigitalObjectDataSource.getInstance();
        r.setAttribute(DigitalObjectDataSource.FIELD_MODEL, modelId);
        if (mods != null) {
            r.setAttribute(DigitalObjectDataSource.FIELD_MODS, mods);
        }
        if (pid != null && !pid.isEmpty()) {
            r.setAttribute(DigitalObjectDataSource.FIELD_PID, pid);
        }

        if (workflowJobId != null) {
            r.setAttribute(DigitalObjectDataSource.FIELD_WF_JOB_ID, workflowJobId);
        }

        DSRequest dsRequest = new DSRequest();
        dsRequest.setWillHandleError(true);
        ds.addData(r, new DSCallback() {
            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (response.getStatus() == RPCResponse.STATUS_VALIDATION_ERROR) {
                    ErrorSavingDigitalObject validationError = ErrorSavingDigitalObject.VALIDATION_ERROR;
                    validationError.setValidationErrors(response.getErrors());
                    callback.onFailure(validationError);
                    request.setWillHandleError(true);
                }
                if (response.getHttpResponseCode() >= 400) {
                    callback.onFailure(null);
                } else if (RestConfig.isConcurrentModification(response)) {
                    callback.onFailure(ErrorSavingDigitalObject.CONCURRENT_MODIFICATION);
                } else {
                    Record[] data = response.getData();
                    if (data != null && data.length > 0) {
                        String pid = data[0].getAttribute(DigitalObjectDataSource.FIELD_PID);
                        callback.onSuccess(pid);
                    } else {
                        callback.onFailure(ErrorSavingDigitalObject.ERROR_SAVING_DIGITAL_OBJECT);
                    }
                }
            }
        }, dsRequest);
    }

    private static String option(Object val, String defval) {
        return val != null ? val.toString() : defval;
    }

    public void delete(String[] pids, Map<?,?> options) {
        final ClientMessages i18n = GWT.create(ClientMessages.class);
        HashMap<String, String> deleteParams = new HashMap<String, String>();
        deleteParams.put(DigitalObjectResourceApi.DELETE_PURGE_PARAM,
                option(options.get(DigitalObjectResourceApi.DELETE_PURGE_PARAM), Boolean.FALSE.toString()));
        deleteParams.put(DigitalObjectResourceApi.DELETE_HIERARCHY_PARAM,
                option(options.get(DigitalObjectResourceApi.DELETE_HIERARCHY_PARAM), Boolean.TRUE.toString()));
        DSRequest dsRequest = new DSRequest();
        dsRequest.setPromptStyle(PromptStyle.DIALOG);
        dsRequest.setPrompt(i18n.DeleteAction_Deleting_Msg());
        dsRequest.setParams(deleteParams);
        Record query = new Record();
        query.setAttribute(FIELD_PID, pids);
        DigitalObjectDataSource.getInstance().removeData(query, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (RestConfig.isStatusOk(response)) {
                    StatusView.getInstance().show(i18n.DeleteAction_Done_Msg());
                    DigitalObjectDataSource.this.updateCaches(response, request);
                    SearchDataSource.getInstance().updateCaches(response, request);
                    RelationDataSource.getInstance().updateCaches(response, request);
                }
            }
        }, dsRequest);
    }

    public void restore(String[] pids, Map<?,?> options) {
        final ClientMessages i18n = GWT.create(ClientMessages.class);
        HashMap<String, String> deleteParams = new HashMap<String, String>();
        deleteParams.put(DigitalObjectResourceApi.DELETE_RESTORE_PARAM,
                option(options.get(DigitalObjectResourceApi.DELETE_RESTORE_PARAM), Boolean.TRUE.toString()));
        deleteParams.put(DigitalObjectResourceApi.DELETE_HIERARCHY_PARAM,
                option(options.get(DigitalObjectResourceApi.DELETE_HIERARCHY_PARAM), Boolean.FALSE.toString()));
        deleteParams.put(DigitalObjectResourceApi.DELETE_PURGE_PARAM,
                option(options.get(DigitalObjectResourceApi.DELETE_PURGE_PARAM), Boolean.FALSE.toString()));
        DSRequest dsRequest = new DSRequest();
        dsRequest.setPromptStyle(PromptStyle.DIALOG);
        dsRequest.setPrompt(i18n.RestoreDeletedObjectsAction_Msg());
        dsRequest.setParams(deleteParams);
        Record query = new Record();
        query.setAttribute(FIELD_PID, pids);
        DigitalObjectDataSource.getInstance().removeData(query, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (RestConfig.isStatusOk(response)) {
                    StatusView.getInstance().show(i18n.RestoreDeletedObjectsAction_Msg());
                    DigitalObjectDataSource.this.updateCaches(response, request);
                    SearchDataSource.getInstance().updateCaches(response, request);
                    RelationDataSource.getInstance().updateCaches(response, request);
                }
            }
        }, dsRequest);
    }

    public static enum ErrorSavingDigitalObject {
        VALIDATION_ERROR, CONCURRENT_MODIFICATION, ERROR_SAVING_DIGITAL_OBJECT;

        private Map validationErrors;

        public Map getValidationErrors() {
            return validationErrors;
        }

        public void setValidationErrors(Map validationErrors) {
            this.validationErrors = validationErrors;
        }
    }

    public static final class DigitalObject {

        private final String pid;
        private final String batchId;
        private final String modelId;
        private final Long workflowJobId;
        private MetaModelRecord model;
        private Record record;

        public static DigitalObject create(String pid, String batchId, MetaModelRecord model) {
            return new DigitalObject(pid, batchId, null, model, null, null);
        }

        public static DigitalObject create(String pid, String batchId, String modelId) {
            return new DigitalObject(pid, batchId, modelId, null, null, null);
        }

        public static DigitalObject create(Record r) {
            return create(r, true);
        }

        public static DigitalObject createOrNull(Record r) {
            return create(r, false);
        }

        /**
         * Creates synthetic root object of a batch import.
         * @param r batch record
         * @return digital object
         */
        public static DigitalObject create(BatchRecord r) {
            return create("proarc:root_item", String.valueOf(r.getId()), "none");
        }

        public static DigitalObject[] toArray(Record[] records) {
            DigitalObject[] dobjs = new DigitalObject[records.length];
            for (int i = 0; i < records.length; i++) {
                dobjs[i] = create(records[i]);
            }
            return dobjs;
        }

        public static String[] toPidArray(DigitalObject[] objects) {
            String[] pids = new String[objects.length];
            for (int i = 0; i < objects.length; i++) {
                pids[i] = objects[i].getPid();
            }
            return pids;
        }

        /**
         * Creates digital object instance from record.
         * @param r record
         * @param checked {@code false} means ignore missing attributes and return {@code null}
         * @return instance of digital object
         */
        private static DigitalObject create(Record r, boolean checked) {
            if (r == null) {
                throw new NullPointerException();
            }
            DigitalObject dobj = (DigitalObject) r.getAttributeAsObject(FIELD_INSTANCE);
            if (dobj != null) {
                return dobj;
            }


            Long workflowJobId = r.getAttributeAsLong(WorkflowModelConsts.JOB_ID);
            String pid = getAttribute(r, FIELD_PID, checked && workflowJobId == null);

            String modelId = getAttribute(r, FIELD_MODEL, checked);
            if ((pid == null && workflowJobId == null) || modelId == null) {
                return null;
            }
            String batchId = r.getAttribute(ModsCustomDataSource.FIELD_BATCHID);
            MetaModelRecord model = MetaModelDataSource.getModel(r);
            return new DigitalObject(pid, batchId, modelId, model, workflowJobId, r);
        }

        public static boolean hasPid(Record r) {
            return r != null && r.getAttribute(FIELD_PID) != null;
        }

        private static String getAttribute(Record r, String fieldName, boolean checked) {
            String attr = r.getAttribute(fieldName);
            if (checked && (attr == null || attr.isEmpty())) {
                throw new IllegalArgumentException(fieldName);
            }
            return attr;
        }

        private DigitalObject(String pid, String batchId, String modelId, MetaModelRecord model, Long worfklowjobId, Record record) {
            if ((pid == null || pid.isEmpty()) && worfklowjobId == null) {
                throw new IllegalArgumentException("No PID or WorkflowJobId was set");
            }
            this.pid = pid;
            this.batchId = batchId;
            this.modelId = model == null ? modelId : model.getId();
            this.model = model;
            this.workflowJobId = worfklowjobId;

            if (this.modelId == null || this.modelId.isEmpty()) {
                throw new IllegalArgumentException("No model for: " + pid);
            }
            this.record = record;
            if (record != null) {
                record.setAttribute(FIELD_INSTANCE, this);
            }
        }

        public String getPid() {
            return pid;
        }

        public Long getWorkflowJobId() {
            return workflowJobId;
        }

        public String getBatchId() {
            return batchId;
        }

        public Integer getBatchIdAsInt() {
            return batchId == null ? null : Integer.valueOf(batchId);
        }

        public String getModelId() {
            return modelId;
        }

        public MetaModelRecord getModel() {
            return model;
        }

        public Record getRecord() {
            if (record == null) {
                record = toRecord();
            }
            return record;
        }

        /**
         * Gets always a new instance of Record.
         */
        public Record toRecord() {
            Record r = new Record();
            r.setAttribute(FIELD_PID, pid);
            r.setAttribute(FIELD_MODEL, modelId);
            if (batchId != null) {
                r.setAttribute(ModsCustomDataSource.FIELD_BATCHID, batchId);
            }
            if (model != null) {
                r.setAttribute(MetaModelDataSource.FIELD_MODELOBJECT, model);
            }
            r.setAttribute(FIELD_INSTANCE, this);
            return r;
        }

        public Criteria toCriteria() {
            Criteria criteria = new Criteria(FIELD_PID, pid);
            if (batchId != null) {
                criteria.addCriteria(ModsCustomDataSource.FIELD_BATCHID, batchId);
            }
            return criteria;
        }

        @Override
        public String toString() {
            return "DigitalObject{" + "pid=" + pid + ", batchId=" + batchId
                    + ", modelId=" + modelId + ", model=" + model + '}';
        }

    }

}
