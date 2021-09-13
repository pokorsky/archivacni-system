/*
 * Copyright (C) 2015 Jan Pokorsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.webapp.client.widget.workflow;

import cz.cas.lib.proarc.common.workflow.model.Job;
import cz.cas.lib.proarc.common.workflow.model.MaterialType;
import cz.cas.lib.proarc.common.workflow.profile.WorkflowProfileConsts;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import cz.cas.lib.proarc.webapp.client.Editor;
import cz.cas.lib.proarc.webapp.client.action.AbstractAction;
import cz.cas.lib.proarc.webapp.client.action.Action;
import cz.cas.lib.proarc.webapp.client.action.ActionEvent;
import cz.cas.lib.proarc.webapp.client.action.Actions;
import cz.cas.lib.proarc.webapp.client.action.Actions.ActionSource;
import cz.cas.lib.proarc.webapp.client.action.RefreshAction;
import cz.cas.lib.proarc.webapp.client.action.RefreshAction.Refreshable;
import cz.cas.lib.proarc.webapp.client.ds.DigitalObjectDataSource;
import cz.cas.lib.proarc.webapp.client.ds.MetaModelDataSource;
import cz.cas.lib.proarc.webapp.client.ds.RelationDataSource;
import cz.cas.lib.proarc.webapp.client.ds.RestConfig;
import cz.cas.lib.proarc.webapp.client.ds.UserDataSource;
import cz.cas.lib.proarc.webapp.client.ds.ValueMapDataSource;
import cz.cas.lib.proarc.webapp.client.ds.WorkflowJobDataSource;
import cz.cas.lib.proarc.webapp.client.ds.WorkflowMaterialDataSource;
import cz.cas.lib.proarc.webapp.client.ds.WorkflowProfileDataSource;
import cz.cas.lib.proarc.webapp.client.presenter.WorkflowJobsEditor;
import cz.cas.lib.proarc.webapp.client.widget.CanvasSizePersistence;
import cz.cas.lib.proarc.webapp.client.widget.Dialog;
import cz.cas.lib.proarc.webapp.client.widget.ListGridPersistance;
import cz.cas.lib.proarc.webapp.client.widget.form.CustomUUIDValidator;
import cz.cas.lib.proarc.webapp.client.widget.mods.NewIssueEditor;
import cz.cas.lib.proarc.webapp.shared.rest.WorkflowResourceApi;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.CriteriaPolicy;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.FetchMode;
import com.smartgwt.client.types.OperatorId;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.IconMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ItemClickEvent;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

/**
 *
 * @author Jan Pokorsky
 */
public class WorkflowJobView implements Refreshable {

    private final ClientMessages i18n;
    private final Canvas widget;
    private ListGrid jobGrid;
    private ListGrid subjobGrid;
    private boolean ignoreSubjobSelection;
    private ListGridPersistance jobsPersistance;
    private ListGridPersistance subjobsPersistance;
    private WorkflowJobFormView jobFormView;
    private WorkflowJobsEditor handler;
    private final ActionSource actionSource = new ActionSource(this);
    private boolean isUpdateOperation;
    private boolean isDataInitialized;
    private ListGridRecord lastSelection;
    private IconMenuButton addSubjobButton;
    private IconMenuButton createNewObjectButton;

    public WorkflowJobView(ClientMessages i18n) {
        this.i18n = i18n;
        this.widget = createMainLayout();
    }

    public Canvas getWidget() {
        return widget;
    }

    private void init() {
        if (!isDataInitialized) {
            isDataInitialized = true;
            jobGrid.setViewState(jobsPersistance.getViewState());
            jobGrid.fetchData(jobsPersistance.getFilterCriteria());
            subjobGrid.setViewState(subjobsPersistance.getViewState());
        }
    }

    public void edit(String jobId) {
        if (jobId == null) {
            init();
            return ;
        }
        int jobRec = jobGrid.findIndex(
                new AdvancedCriteria(WorkflowJobDataSource.FIELD_ID, OperatorId.EQUALS, jobId));
        if (jobRec >= 0) {
            jobGrid.selectSingleRecord(jobRec);
            jobGrid.scrollToRow(jobRec);
        } else {
            lastSelection = null;
            jobGrid.deselectAllRecords();
            Record r = new Record();
            r.setAttribute(WorkflowJobDataSource.FIELD_ID, jobId);
            jobFormView.setJob(r);
            loadSubjobs(r);
        }
    }

    public void setHandler(WorkflowJobsEditor handler) {
        this.handler = handler;
        jobFormView.setHandler(handler);
    }

    @Override
    public void refresh() {
        if (isDataInitialized) {
            WorkflowProfileDataSource.getInstance().getProfileResultSet(true);
            jobGrid.invalidateCache();
        } else {
            init();
        }
    }

    public void editSelection() {
        ListGridRecord selection = jobGrid.getSelectedRecord();
        jobFormView.setJob(selection);
        loadSubjobs(selection);
        refreshState();
    }

    private void loadSubjobs(Record job) {
        try {
            ignoreSubjobSelection = true;
            subjobGrid.deselectAllRecords();
            if (job != null) {
                String id = job.getAttribute(WorkflowJobDataSource.FIELD_ID);
                subjobGrid.fetchData(new Criteria(WorkflowJobDataSource.FIELD_PARENTID, id));
            } else {
                subjobGrid.setData(new Record[0]);
            }
        } finally {
            ignoreSubjobSelection = false;
        }
    }

    public void editSubjobSelection() {
        jobFormView.setJob(subjobGrid.getSelectedRecord());
        actionSource.fireEvent();
        jobFormView.refreshState();
    }

    public void refreshState() {
        fetchAddSubjobMenu(jobGrid.getSelectedRecord());
        fetchModelMenu(jobGrid.getSelectedRecord());
        actionSource.fireEvent();
        jobFormView.refreshState();
    }

    /**
     * Set to {@code true} before saving a job. It is a hack not to select
     * the job again inside {@code onDataArrived}.
     */
    public void setExpectUpdateOperation(boolean isUpdateOperation) {
        this.isUpdateOperation = isUpdateOperation;
    }

    private Canvas createMainLayout() {
        VLayout main = new VLayout();
        main.addMember(createPanelLabel());
        // filter
        main.addMember(createFilter());
        // list + item
        main.addMember(createJobLayout());
        return main;
    }

    private Canvas createJobLayout() {
        VLayout left = new VLayout();
        left.addMember(createJobsToolbar());
        left.addMember(createJobList());
        left.addMember(createSubjobList());
        left.setShowResizeBar(true);

        CanvasSizePersistence sizePersistence = new CanvasSizePersistence("WorkflowJobFormView.jobLayout", left);
        left.setWidth(sizePersistence.getWidth());

        HLayout l = new HLayout();
        l.addMember(left);
        l.addMember(createJobFormLayout());
        return l;
    }

    private Label createPanelLabel() {
        Label lblHeader = new Label();
        String title = ClientUtils.format("<b>%s</b>", i18n.WorkflowJob_View_Title());
        lblHeader.setContents(title);
        lblHeader.setAutoHeight();
        lblHeader.setPadding(4);
        lblHeader.setStyleName(Editor.CSS_PANEL_DESCRIPTION_TITLE);
        return lblHeader;
    }

    private DynamicForm createFilter() {
        DynamicForm form = new DynamicForm();
        form.setBrowserSpellCheck(false);
        form.setValidateOnExit(true);
        form.setSaveOnEnter(true);
        form.setAutoHeight();
        form.setWidth100();
        form.setNumCols(3);
        // ????
        return form;
    }

    private ToolStrip createJobsToolbar() {
        ToolStrip toolbar = Actions.createToolStrip();
        RefreshAction refreshAction = new RefreshAction(i18n);

        AbstractAction addAction = new AbstractAction(i18n.WorkflowJob_View_NewAction_Title(),
                "[SKIN]/actions/add.png", i18n.WorkflowJob_View_NewAction_Hint()) {

            @Override
            public void performAction(ActionEvent event) {
                if (handler != null) {
                    handler.onCreateNew();
                }
            }
        };
        Action addSubjobAction = Actions.emptyAction(i18n.WorkflowJob_View_NewSubjobAction_Title(),
                "[SKIN]/actions/add.png", i18n.WorkflowJob_View_NewSubjobAction_Hint());

        Action createNewObject = Actions.emptyAction(i18n.DigitalObjectCreator_FinishedStep_CreateNewObjectButton_Title(),
                "[SKIN]/actions/save.png",
                "");

        toolbar.addMember(Actions.asIconButton(refreshAction, this));
        toolbar.addMember(Actions.asIconButton(addAction, this));
        addSubjobButton = Actions.asIconMenuButton(addSubjobAction, this);
        addSubjobButton.setVisible(false);

        createNewObjectButton = Actions.asIconMenuButton(createNewObject, this);
        createNewObjectButton.setShowDisabledIcon(false);
        createNewObjectButton.setDisabled(true);

        toolbar.addMember(addSubjobButton);
        toolbar.addMember(createNewObjectButton);
        return toolbar;
    }

    private void fetchAddSubjobMenu(Record job) {
        if (job == null
                || job.getAttribute(WorkflowJobDataSource.FIELD_PARENTID) != null
                || !Job.State.OPEN.name().equals(job.getAttribute(WorkflowJobDataSource.FIELD_STATE))
                ) {
            addSubjobButton.setVisible(false);
            return ;
        }
        String jobName = job.getAttribute(WorkflowJobDataSource.FIELD_PROFILE_ID);
        if (jobName == null) {
            return ;
        }
        WorkflowProfileDataSource.getInstance().getSubjobs(false, jobName, (subjobs) -> {
            Menu menu = createSubjobMenu(subjobs);
            addSubjobButton.setVisible(menu != null);
            addSubjobButton.setMenu(menu);
        });
    }

    private void fetchModelMenu(Record job) {
        if (job == null
                // not subjob
                //|| job.getAttribute(WorkflowJobDataSource.FIELD_PARENTID) != null
                || !Job.State.OPEN.name().equals(job.getAttribute(WorkflowJobDataSource.FIELD_STATE))
                ) {
            createNewObjectButton.setVisible(false);
            return;
        }
        String jobName = job.getAttribute(WorkflowJobDataSource.FIELD_PROFILE_ID);
        Long jobId = job.getAttributeAsLong(WorkflowJobDataSource.FIELD_ID);

        if (jobName == null) {
            return;
        }

        Criteria criteria = new Criteria();
        criteria.setAttribute(WorkflowMaterialDataSource.FIELD_JOB_ID, jobId);
        criteria.setAttribute(WorkflowMaterialDataSource.FIELD_TYPE, MaterialType.DIGITAL_OBJECT.name());
        WorkflowMaterialDataSource.getInstance().fetchData(criteria, (dsResponse, o, dsRequest) -> {
            RecordList records = dsResponse.getDataAsRecordList();
            if (records.getLength() == 1 && records.get(0)
                    .getAttribute(WorkflowMaterialDataSource.FIELD_DIGITAL_PID) == null) {
                createNewObjectButton.enable();
            } else {
                createNewObjectButton.disable();
            }
        });


        WorkflowProfileDataSource.getInstance().getModels(false, jobName, (models) -> {
            Menu menu = new Menu();

            for (Record model : models) {
                MenuItem menuItem = new MenuItem(model.getAttribute(WorkflowProfileConsts.MODEL_TITLE));
                menuItem.setAttribute(WorkflowProfileConsts.MODEL_PID, model.getAttribute(WorkflowProfileConsts.MODEL_NAME));
                menu.addItem(menuItem);
                menuItem.addClickHandler(event -> {
                    saveNewDigitalObject(model.getAttributeAsString(WorkflowProfileConsts.MODEL_NAME), jobId);
                    createNewObjectButton.disable();
                    actionSource.fireEvent();
                    jobFormView.refreshState();
                    jobFormView.refresh();
                });
            }
            Arrays.stream(menu.getItems()).forEach(menuItem -> attachAddSubmenu(menuItem, jobId));
            createNewObjectButton.setVisible(models.length > 0);
            createNewObjectButton.setMenu(menu);
        });
    }

    private void attachAddSubmenu(MenuItem menuItem, Long jobId) {
        Menu sm = new Menu();
        MenuItem miAddSingle = new MenuItem(i18n.DigitalObjectEditor_ChildrenEditor_CreateAction_Title());
        MenuItem miAddSingleWithID = new MenuItem(i18n.DigitalObjectEditor_ChildrenEditor_CreateWithParamsAction_Title());
        MenuItem miAddMultiple = new MenuItem(i18n.DigitalObjectEditor_ChildrenEditor_CreateMoreAction_Title());

        sm.addItemClickHandler((event) -> {
            String model = menuItem.getAttribute(WorkflowProfileConsts.MODEL_PID);
            if (event.getItem() == miAddSingle) {
                addChild(model, Collections.emptyMap(), jobId);
            } else if (event.getItem() == miAddSingleWithID) {
                final Dialog d = new Dialog(i18n.DigitalObjectEditor_ChildrenEditor_CreateWithParamsAction_Dialog_Title());

                DynamicForm paramsForm = createParamsForm();
                paramsForm.clearValues();

                d.getDialogContentContainer().setMembers(paramsForm);
                d.addOkButton((ClickEvent eventX) -> {
                    if(!paramsForm.validate()) {
                        return;
                    }

                    addChild(model, Collections.emptyMap(), paramsForm, d, jobId);
                });

                d.addCancelButton(() -> d.destroy());
                d.setWidth(400);
                d.show();
            } else if (event.getItem() == miAddMultiple && "model:ndkperiodicalissue".equals(menuItem.getAttribute(MetaModelDataSource.FIELD_PID))) {
                new NewIssueEditor(i18n).showWindow(params -> {
                    addChild(model, params.toMap(), jobId);
                });
            }
        });

        sm.addItem(miAddSingle);
        sm.addItem(miAddSingleWithID);

        if ("model:ndkperiodicalissue".equals(menuItem.getAttribute(MetaModelDataSource.FIELD_PID))) {
            sm.addItem(miAddMultiple);
        }

        menuItem.setSubmenu(sm);
    }

    private void addChild(String model, Map<String, Object> params, Long jobId) {
        addChild(model, params, null, null, jobId);
    }

    private void addChild(String model, Map<String, Object> params, DynamicForm paramsForm, Dialog dialog, Long jobId) {
        Record record = new Record(params);
        record.setAttribute(DigitalObjectDataSource.FIELD_MODEL, model);
        record.setAttribute(DigitalObjectDataSource.FIELD_WF_JOB_ID, jobId);

        DSRequest dsRequest = new DSRequest();

        if (paramsForm != null) {
            String pid = paramsForm.getValueAsString(DigitalObjectDataSource.FIELD_PID);

            if (pid != null) {
                record.setAttribute(DigitalObjectDataSource.FIELD_PID, pid);

                dsRequest.setWillHandleError(true);
            }
        }

        DigitalObjectDataSource.getInstance().addData(record, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (RestConfig.isStatusOk(response)) {
                    Record[] data = response.getData();
                    final Record r = data[0];
                    DSRequest dsRequest = new DSRequest();
                    dsRequest.setOperationType(DSOperationType.ADD);
                    RelationDataSource.getInstance().updateCaches(response, dsRequest);

                    if (dialog != null) {
                        dialog.destroy();
                    }
                }

                if (response.getStatus() == RPCResponse.STATUS_VALIDATION_ERROR) {
                    if (paramsForm != null) {
                        Map errors = response.getErrors();

                        paramsForm.setErrors(errors, true);

                        return;
                    }
                } else if (response.getStatus() == RPCResponse.STATUS_SUCCESS) {
                    SC.say(i18n.DigitalObjectCreator_FinishedStep_CreateNewObjectButton_Title(), i18n.DigitalObjectCreator_FinishedStep_Done_Msg());
                }
            }
        }, dsRequest);
    }

    private DynamicForm createParamsForm() {
        ClientMessages i18n = GWT.create(ClientMessages.class);
        DynamicForm f = new DynamicForm();
        f.setAutoHeight();

        TextItem newPid = new TextItem(DigitalObjectDataSource.FIELD_PID);

        newPid.setTitle(i18n.NewDigObject_OptionPid_Title());
        newPid.setTooltip(i18n.NewDigObject_OptionPid_Hint());
        newPid.setRequired(true);
        newPid.setLength(36 + 5);
        newPid.setWidth((36 + 5) * 8);
        newPid.setValidators(new CustomUUIDValidator(i18n));
        f.setFields(newPid);
        f.setAutoFocus(true);
        return f;
    }

    private void saveNewDigitalObject(String modelId, Long jobId) {
        if (modelId == null || jobId == null) {
            return;
        }

        DigitalObjectDataSource ds = DigitalObjectDataSource.getInstance();
        ds.saveNewDigitalObject(modelId, null, null, jobId, null, new Callback<String, DigitalObjectDataSource.ErrorSavingDigitalObject>() {

            @Override
            public void onFailure(DigitalObjectDataSource.ErrorSavingDigitalObject reason) {
                switch (reason) {
                    case CONCURRENT_MODIFICATION:
                        SC.ask(i18n.SaveAction_ConcurrentErrorAskReload_Msg(), aBoolean -> {
                                if (aBoolean!= null && aBoolean) {
                                    refresh();
                            }});
                    default:
                        SC.warn("Failed to create digital object!");
                }
            }

            @Override
            public void onSuccess(String result) {
                SC.say(i18n.DigitalObjectCreator_FinishedStep_CreateNewObjectButton_Title(), i18n.DigitalObjectCreator_FinishedStep_Done_Msg());
            }
        });
    }



    private Menu createSubjobMenu(Record[] subjobs) {
        if (subjobs == null || subjobs.length == 0) {
            return null;
        }
        Menu menu = new Menu();
        menu.setData(subjobs);
        menu.addItemClickHandler((ItemClickEvent event) -> {
            Record subjob = event.getRecord();
            Record job = jobGrid.getSelectedRecord();
            createSubjob(job, subjob);
        });
        return menu;
    }

    /*private void createSubjob(Record job, Record subjob) {
        Record query = new Record();
        query.setAttribute(WorkflowResourceApi.NEWJOB_PARENTID, job.getAttribute(WorkflowJobDataSource.FIELD_ID));
        query.setAttribute(WorkflowResourceApi.NEWJOB_PROFILE, subjob.getAttribute(WorkflowProfileDataSource.FIELD_ID));
        WorkflowJobDataSource ds = WorkflowJobDataSource.getInstance();
        ds.addData(query, (dsResponse, data, dsRequest) -> {
            if (RestConfig.isStatusOk(dsResponse)) {
                StatusView.getInstance().show(i18n.DigitalObjectCreator_FinishedStep_Done_Msg());
                Record[] records = dsResponse.getData();
                if (records.length > 0) {
                    int idx = subjobGrid.findIndex(new AdvancedCriteria(WorkflowJobDataSource.FIELD_ID,
                            OperatorId.EQUALS, records[0].getAttribute(WorkflowJobDataSource.FIELD_ID)));
                    subjobGrid.selectSingleRecord(idx);
                    subjobGrid.scrollToRow(idx);
                }
            }
        });
    }*/

    private void createSubjob(Record job, Record subjob) {
        if (handler != null) {
            Record query = new Record();
            query.setAttribute(WorkflowResourceApi.NEWJOB_PARENTID, job.getAttribute(WorkflowJobDataSource.FIELD_ID));
            query.setAttribute(WorkflowResourceApi.NEWJOB_PROFILE, subjob.getAttribute(WorkflowProfileDataSource.FIELD_ID));
            handler.onCreateNewSubjob(query, subjobGrid);
        }
    }

    private ListGrid createSubjobList() {
        ListGrid g = new ListGrid();
        subjobGrid = g;
        subjobsPersistance = new ListGridPersistance("WorkflowJobView.subjobList", g);

        CanvasSizePersistence sizePersistence = new CanvasSizePersistence("WorkflowJobView.subjobList", g);
        g.setHeight(sizePersistence.getHeight());

        g.setSelectionType(SelectionStyle.SINGLE);
        g.setCanGroupBy(false);
        g.setDataFetchMode(FetchMode.BASIC);
        g.setDataSource(WorkflowJobDataSource.getInstance(),
//                new ListGridField(WorkflowJobDataSource.FIELD_MASTER_PATH, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_RAW_PATH, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_MBARCODE, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MISSUE, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MDETAIL, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_TASK_NAME, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_TASK_CHANGE_DATE, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_TASK_CHANGE_USER, 50),
                new ListGridField(WorkflowJobDataSource.FIELD_MEDITION, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_FINANCED, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_ID, 30),
                new ListGridField(WorkflowJobDataSource.FIELD_LABEL),
                new ListGridField(WorkflowJobDataSource.FIELD_MPID, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_MFIELD001, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_NOTE),
                new ListGridField(WorkflowJobDataSource.FIELD_PRIORITY, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_PROFILE_ID, 80),
                new ListGridField(WorkflowJobDataSource.FIELD_MVOLUME, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MYEAR, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MSIGLA, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MSIGNATURE, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_STATE, 50),
                //new ListGridField(WorkflowJobDataSource.FIELD_OWNER, 50),
                new ListGridField(WorkflowJobDataSource.FIELD_CREATED, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_MODIFIED, 100)

                );
        g.setSortField(WorkflowJobDataSource.FIELD_CREATED);
        g.setSortDirection(SortDirection.ASCENDING);

        SelectItem profileFilter = new SelectItem();
        profileFilter.setOptionDataSource(WorkflowProfileDataSource.getInstance());
        profileFilter.setValueField(WorkflowProfileDataSource.FIELD_ID);
        profileFilter.setDisplayField(WorkflowProfileDataSource.FIELD_LABEL);
        g.getField(WorkflowJobDataSource.FIELD_PROFILE_ID).setFilterEditorProperties(profileFilter);

        /*
        SelectItem owner = new SelectItem();
        owner.setOptionDataSource(UserDataSource.getInstance());
        owner.setValueField(UserDataSource.FIELD_ID);
        owner.setDisplayField(UserDataSource.FIELD_USERNAME);
        g.getField(WorkflowJobDataSource.FIELD_OWNER).setFilterEditorProperties(owner);
         */

        SelectItem taskOwner = new SelectItem();
        taskOwner.setOptionDataSource(UserDataSource.getInstance());
        taskOwner.setValueField(UserDataSource.FIELD_ID);
        taskOwner.setDisplayField(UserDataSource.FIELD_USERNAME);
        g.getField(WorkflowJobDataSource.FIELD_TASK_CHANGE_USER).setFilterEditorProperties(taskOwner);

        g.addSelectionUpdatedHandler((SelectionUpdatedEvent event) -> {
            if (!ignoreSubjobSelection) {
                editSubjobSelection();
            }
            ignoreSubjobSelection = false;
        });
        return g;
    }

    private ListGrid createJobList() {
        jobGrid = new ListGrid();
        jobsPersistance = new ListGridPersistance("WorkflowJobView.jobList", jobGrid);
        jobGrid.setSelectionType(SelectionStyle.SINGLE);
        jobGrid.setShowFilterEditor(true);
        jobGrid.setAllowFilterOperators(false);
        jobGrid.setFilterOnKeypress(true);
        jobGrid.setFilterLocalData(false);
        jobGrid.setCanSort(true);
        jobGrid.setCanGroupBy(false);
        jobGrid.setShowResizeBar(true);
        jobGrid.setResizeBarTarget("next");
        jobGrid.setDataFetchMode(FetchMode.PAGED);
        ResultSet rs = new ResultSet();
        rs.setCriteriaPolicy(CriteriaPolicy.DROPONCHANGE);
        rs.setUseClientFiltering(false);
        rs.setUseClientSorting(false);
        jobGrid.setDataProperties(rs);
        jobGrid.setDataSource(WorkflowJobDataSource.getInstance(),
//                new ListGridField(WorkflowJobDataSource.FIELD_MASTER_PATH, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_RAW_PATH, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_MBARCODE, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MISSUE, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MDETAIL, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_TASK_NAME, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_TASK_CHANGE_DATE, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_TASK_CHANGE_USER, 50),
                new ListGridField(WorkflowJobDataSource.FIELD_MEDITION, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_FINANCED, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_ID, 30),
                new ListGridField(WorkflowJobDataSource.FIELD_LABEL),
                new ListGridField(WorkflowJobDataSource.FIELD_MPID, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_MFIELD001, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_NOTE),
                new ListGridField(WorkflowJobDataSource.FIELD_PRIORITY, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_PROFILE_ID, 80),
                new ListGridField(WorkflowJobDataSource.FIELD_MVOLUME, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MYEAR, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MSIGLA, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_MSIGNATURE, 60),
                new ListGridField(WorkflowJobDataSource.FIELD_STATE, 50),
                //new ListGridField(WorkflowJobDataSource.FIELD_OWNER, 50),
                new ListGridField(WorkflowJobDataSource.FIELD_CREATED, 100),
                new ListGridField(WorkflowJobDataSource.FIELD_MODIFIED, 100)
                );

        jobGrid.getField(WorkflowJobDataSource.FIELD_LABEL).setWidth("80%");
        jobGrid.getField(WorkflowJobDataSource.FIELD_LABEL).setFilterOnKeypress(false);

        jobGrid.getField(WorkflowJobDataSource.FIELD_ID).setFilterOperator(OperatorId.EQUALS);

        jobGrid.getField(WorkflowJobDataSource.FIELD_STATE).setCanSort(false);

        jobGrid.getField(WorkflowJobDataSource.FIELD_PROFILE_ID).setCanSort(false);
        SelectItem profileFilter = new SelectItem();
        profileFilter.setOptionDataSource(WorkflowProfileDataSource.getInstance());
        profileFilter.setValueField(WorkflowProfileDataSource.FIELD_ID);
        profileFilter.setDisplayField(WorkflowProfileDataSource.FIELD_LABEL);
        jobGrid.getField(WorkflowJobDataSource.FIELD_PROFILE_ID).setFilterEditorProperties(profileFilter);

        /*
        jobGrid.getField(WorkflowJobDataSource.FIELD_OWNER).setCanSort(false);
        SelectItem owner = new SelectItem();
        owner.setOptionDataSource(UserDataSource.getInstance());
        owner.setValueField(UserDataSource.FIELD_ID);
        owner.setDisplayField(UserDataSource.FIELD_USERNAME);
        jobGrid.getField(WorkflowJobDataSource.FIELD_OWNER).setFilterEditorProperties(owner);
        */

        jobGrid.getField(WorkflowJobDataSource.FIELD_TASK_NAME).setCanFilter(true);
        jobGrid.getField(WorkflowJobDataSource.FIELD_TASK_NAME).setCanSort(false);

        SelectItem taskOptions = new SelectItem();
        taskOptions.setOptionDataSource(ValueMapDataSource.getInstance()
                .getOptionDataSource(WorkflowProfileConsts.WORKFLOWITEMVIEW_TASKS_VALUEMAP));
        taskOptions.setValueField(WorkflowProfileConsts.NAME);
        taskOptions.setDisplayField(WorkflowProfileConsts.TITLE_EL);
        jobGrid.getField(WorkflowJobDataSource.FIELD_TASK_NAME).setFilterEditorProperties(taskOptions);

        SelectItem taskOwner = new SelectItem();
        taskOwner.setOptionDataSource(UserDataSource.getInstance());
        taskOwner.setValueField(UserDataSource.FIELD_ID);
        taskOwner.setDisplayField(UserDataSource.FIELD_USERNAME);
        jobGrid.getField(WorkflowJobDataSource.FIELD_TASK_CHANGE_USER).setFilterEditorProperties(taskOwner);
        jobGrid.getField(WorkflowJobDataSource.FIELD_TASK_CHANGE_USER).setCanSort(false);

        jobGrid.getField(WorkflowJobDataSource.FIELD_NOTE).setCanSort(false);

        jobGrid.addDataArrivedHandler((DataArrivedEvent event) -> {
            if (isUpdateOperation) {
                isUpdateOperation = false;
                return ;
            }
            int startRow = event.getStartRow();
            int endRow = event.getEndRow();
            if (startRow == 0 && endRow >= 0) {
                jobGrid.focus();
                updateSelection();
            } else if (endRow < 0) {
                jobGrid.deselectAllRecords();
            }
        });
        jobGrid.addSelectionUpdatedHandler((SelectionUpdatedEvent event) -> {
            lastSelection = jobGrid.getSelectedRecord();
            editSelection();
        });
        return jobGrid;
    }

    private void updateSelection() {
        RecordList rl = jobGrid.getRecordList();
        if (rl.isEmpty()) {
            return ;
        }
        if (lastSelection == null) {
            jobGrid.selectSingleRecord(0);
            return ;
        }
        Record newRec = rl.find(WorkflowJobDataSource.FIELD_ID,
                lastSelection.getAttribute(WorkflowJobDataSource.FIELD_ID));
        if (newRec != null) {
            jobGrid.selectSingleRecord(newRec);
            int rowNum = jobGrid.getRecordIndex(newRec);
            if (rowNum >= 0) {
                jobGrid.scrollToRow(rowNum);
            }
        }
    }

    private Canvas createJobFormLayout() {
        jobFormView = new WorkflowJobFormView(i18n);
        return jobFormView.getWidget();
    }

}
