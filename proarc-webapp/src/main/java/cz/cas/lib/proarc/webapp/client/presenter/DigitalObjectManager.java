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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.webapp.client.presenter;

import com.google.gwt.core.client.Callback;
import com.google.gwt.place.shared.PlaceController;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.util.Offline;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedEvent;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.IconMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import cz.cas.lib.proarc.common.object.model.DatastreamEditorType;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import cz.cas.lib.proarc.webapp.client.Editor;
import cz.cas.lib.proarc.webapp.client.action.AbstractAction;
import cz.cas.lib.proarc.webapp.client.action.ActionEvent;
import cz.cas.lib.proarc.webapp.client.action.Actions;
import cz.cas.lib.proarc.webapp.client.action.Actions.ActionSource;
import cz.cas.lib.proarc.webapp.client.action.DeleteAction;
import cz.cas.lib.proarc.webapp.client.action.DigitalObjectEditAction;
import cz.cas.lib.proarc.webapp.client.action.FoxmlViewAction;
import cz.cas.lib.proarc.webapp.client.action.RefreshAction;
import cz.cas.lib.proarc.webapp.client.action.RefreshAction.Refreshable;
import cz.cas.lib.proarc.webapp.client.action.TreeExpandAction;
import cz.cas.lib.proarc.webapp.client.action.UrnNbnAction;
import cz.cas.lib.proarc.webapp.client.action.export.ArchiveExportAction;
import cz.cas.lib.proarc.webapp.client.action.export.CejshExportAction;
import cz.cas.lib.proarc.webapp.client.action.export.CrossrefExportAction;
import cz.cas.lib.proarc.webapp.client.action.export.DataStreamExportAction;
import cz.cas.lib.proarc.webapp.client.action.export.DesaExportAction;
import cz.cas.lib.proarc.webapp.client.action.export.KrameriusExportAction;
import cz.cas.lib.proarc.webapp.client.action.export.NdkExportAction;
import cz.cas.lib.proarc.webapp.client.action.export.NdkOldPrintExportAction;
import cz.cas.lib.proarc.webapp.client.action.export.NdkSipExportAction;
import cz.cas.lib.proarc.webapp.client.ds.DigitalObjectDataSource;
import cz.cas.lib.proarc.webapp.client.ds.MetaModelDataSource;
import cz.cas.lib.proarc.webapp.client.ds.RelationDataSource;
import cz.cas.lib.proarc.webapp.client.widget.DigitalObjectSearchView;
import cz.cas.lib.proarc.webapp.client.widget.DigitalObjectTreeView;
import java.util.LinkedHashMap;

/**
 * The component allows to search digital objects and perform actions on
 * search results.
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectManager {

    public static final String LAST_SELECTED_MODEL_TAG = "DigitalObjectManager_LastSelectedModel";

    private final ClientMessages i18n;
    private final PlaceController places;
    private final VLayout widget;
    private final DigitalObjectSearchView foundView;
    private final DigitalObjectTreeView treeView;
    private FoxmlViewAction foxmlAction;
    private ArchiveExportAction archiveExportAction;
    private KrameriusExportAction krameriusExportAction;
    private NdkExportAction ndkExportAction;
    private NdkSipExportAction ndkSipExportAction;
    private NdkOldPrintExportAction ndkOldPrintExportAction;
    private CejshExportAction cejshExportAction;
    private CrossrefExportAction crossrefExportAction;
    private DesaExportAction desaDownloadAction;
    private DesaExportAction desaExportAction;
    private DataStreamExportAction fullDataStreamExportAction;
    private DataStreamExportAction rawDataStreamExportAction;
    private DeleteAction deleteAction;
    private DigitalObjectEditAction ocrEditAction;
    private DigitalObjectEditAction noteEditAction;
    private DigitalObjectEditAction modsEditAction;
    private DigitalObjectEditAction parentEditAction;
    private DigitalObjectEditAction mediaEditAction;
    private DigitalObjectEditAction childrenEditAction;
    private DigitalObjectEditAction atmEditAction;
    private UrnNbnAction registerUrnNbnAction;
    private TreeExpandAction expandTreeAction;
    private boolean initialized;

    public DigitalObjectManager(ClientMessages i18n, PlaceController places) {
        this.i18n = i18n;
        this.places = places;

        Label lblHeader = new Label();
        String title = ClientUtils.format("<b>%s</b>", i18n.DigitalObjectManager_Title());
        lblHeader.setContents(title);
        lblHeader.setAutoHeight();
        lblHeader.setPadding(4);
        lblHeader.setStyleName(Editor.CSS_PANEL_DESCRIPTION_TITLE);

        widget = new VLayout(4);
        widget.setWidth100();
        widget.setHeight100();

        foundView = new DigitalObjectSearchView(i18n, LAST_SELECTED_MODEL_TAG);
        foundView.getGrid().setSelectionType(SelectionStyle.MULTIPLE);
        final ActionSource listSource = new ActionSource(foundView);
        foundView.getGrid().addSelectionUpdatedHandler(new SelectionUpdatedHandler() {

            @Override
            public void onSelectionUpdated(SelectionUpdatedEvent event) {
                final Record[] selectedRecords = foundView.getSelection();
                int selectedRecordNumber = selectedRecords != null ? selectedRecords.length : 0;
                listSource.fireEvent();
                if (selectedRecordNumber == 1) {
                    String pid = selectedRecords[0].getAttribute(RelationDataSource.FIELD_PID);
                    treeView.setRoot(pid);
                } else if (selectedRecordNumber == 0) {
                    treeView.setRoot(null);
                }
            }
        });

        treeView = new DigitalObjectTreeView(i18n);
        treeView.getTree().setSelectionType(SelectionStyle.MULTIPLE);
        final ActionSource treeSource = new ActionSource(treeView);
        treeView.getTree().addSelectionUpdatedHandler(new SelectionUpdatedHandler() {

            @Override
            public void onSelectionUpdated(SelectionUpdatedEvent event) {
                treeSource.fireEvent();
            }
        });

        widget.addMember(lblHeader);
        Canvas foundViewWidget = foundView.asWidget();
        foundViewWidget.setShowResizeBar(true);
        widget.addMember(foundViewWidget);
        widget.addMember(treeView.asWidget());
        createActions();
        initToolbar(foundView.getToolbar(), listSource, MenuType.FOUND_VIEW);
        initToolbar(treeView.getToolbar(), treeSource, MenuType.TREE_VIEW);
        initContextMenu(foundView.getGrid().getContextMenu(), listSource);
        initContextMenu(treeView.getTree().getContextMenu(), treeSource);
        Actions.fixListGridContextMenu(foundView.getGrid());
        Actions.fixListGridContextMenu(treeView.getTree());
    }

    public void init() {
        if (initialized) {
            return ;
        }
        initialized = true;
        fetchModels(false);
        foundView.onShow();
        treeView.setRoot(null);
    }

    private void fetchModels(final boolean reload) {
        MetaModelDataSource.getModels(reload, new Callback<ResultSet, Void>() {

            @Override
            public void onFailure(Void reason) {
            }

            @Override
            public void onSuccess(ResultSet modelResultSet) {
                LinkedHashMap<?, ?> valueMap = ClientUtils.getValueMap(modelResultSet,
                        MetaModelDataSource.FIELD_PID, MetaModelDataSource.FIELD_DISPLAY_NAME);
                treeView.setModels(valueMap);
                foundView.setModels(valueMap);
                if (!reload) {
                    //issue #499
                    Object previousId = Offline.get(LAST_SELECTED_MODEL_TAG);
                    if (previousId != null) {
                        foundView.setFilterModel(previousId);
                    } else if (!valueMap.isEmpty()) {
                        // init the view filter with the first modelId on first show
                        Object firstModel = valueMap.keySet().iterator().next();
                        foundView.setFilterModel(firstModel);
                    }
                    foundView.refresh();
                }
            }
        });
    }

    public VLayout getUI() {
        return widget;
    }

    private void createActions() {
        archiveExportAction = new ArchiveExportAction(i18n);
        foxmlAction = new FoxmlViewAction(i18n);
        krameriusExportAction = new KrameriusExportAction(i18n);
        ndkExportAction = new NdkExportAction(i18n);
        ndkOldPrintExportAction = new NdkOldPrintExportAction(i18n);
        ndkSipExportAction = new NdkSipExportAction(i18n);
        cejshExportAction = new CejshExportAction(i18n);
        crossrefExportAction = new CrossrefExportAction(i18n);
        desaExportAction = DesaExportAction.export(i18n);
        desaDownloadAction = DesaExportAction.download(i18n);
        fullDataStreamExportAction = DataStreamExportAction.full(i18n);
        rawDataStreamExportAction = DataStreamExportAction.raw(i18n);
        deleteAction = new DeleteAction(DigitalObjectDataSource.createDeletable(),
                DigitalObjectDataSource.createDeleteOptionsForm(), i18n);
        ocrEditAction = new DigitalObjectEditAction(
                i18n.ImportBatchItemEditor_TabOcr_Title(), DatastreamEditorType.OCR, i18n);
        noteEditAction = new DigitalObjectEditAction(
                i18n.ImportBatchItemEditor_TabNote_Title(),
                i18n.ImportBatchItemEditor_TabNote_Hint(),
                null,
                DatastreamEditorType.NOTE, places);
        modsEditAction = new DigitalObjectEditAction(
                i18n.ImportBatchItemEditor_TabMods_Title(), DatastreamEditorType.MODS, i18n);
        parentEditAction = new DigitalObjectEditAction(
                i18n.DigitalObjectEditor_ParentAction_Title(), DatastreamEditorType.PARENT, i18n);
        mediaEditAction = new DigitalObjectEditAction(
                i18n.DigitalObjectEditor_MediaAction_Title(),
                i18n.DigitalObjectEditor_MediaAction_Hint(),
                null,
                DatastreamEditorType.MEDIA, places);
        childrenEditAction = new DigitalObjectEditAction(
                i18n.DigitalObjectEditor_ChildrenAction_Title(),
                i18n.DigitalObjectEditor_ChildrenAction_Hint(),
                null,
                DatastreamEditorType.CHILDREN, places);
        atmEditAction = new DigitalObjectEditAction(
                i18n.DigitalObjectEditor_AdministrationAction_Title(),
                i18n.DigitalObjectEditor_AdministrationAction_Hint(),
                null,
                DatastreamEditorType.ATM, places);
        registerUrnNbnAction = new UrnNbnAction(i18n);
        expandTreeAction = new TreeExpandAction(
                i18n,
                treeView);
    }

    /**
     * export (Kramerius, Datastream), edit(MODS, Hierarchy), delete, view (Datastream)
     */
    private void initToolbar(ToolStrip toolbar, ActionSource actionSource, MenuType menuType) {
        final AbstractAction exportMenuAction = new AbstractAction(
                i18n.ExportsAction_Title(), "[SKIN]/actions/save.png", null) {

            @Override
            public boolean accept(ActionEvent event) {
                Object[] selection = Actions.getSelection(event);
                return selection != null && selection.length > 0;
            }

            @Override
            public void performAction(ActionEvent event) {
                // choose default action iff supported
            }
        };
        IconMenuButton btnExport = Actions.asIconMenuButton(exportMenuAction, actionSource);
        Menu menuExport = Actions.createMenu();
        menuExport.addItem(Actions.asMenuItem(archiveExportAction, actionSource, false));
        menuExport.addItem(Actions.asMenuItem(krameriusExportAction, actionSource, false));
        menuExport.addItem(Actions.asMenuItem(ndkExportAction, actionSource, false));
        menuExport.addItem(Actions.asMenuItem(ndkSipExportAction, actionSource, false));
        menuExport.addItem(Actions.asMenuItem(ndkOldPrintExportAction, actionSource, false));
        menuExport.addItem(Actions.asMenuItem(cejshExportAction, actionSource, false));
        menuExport.addItem(Actions.asMenuItem(crossrefExportAction, actionSource, false));
        menuExport.addItem(Actions.asMenuItem(desaExportAction, actionSource, true));
        menuExport.addItem(Actions.asMenuItem(desaDownloadAction, actionSource, false));
        menuExport.addItem(Actions.asMenuItem(fullDataStreamExportAction, actionSource, false));
        menuExport.addItem(Actions.asMenuItem(rawDataStreamExportAction, actionSource, false));
        btnExport.setMenu(menuExport);

        toolbar.addMember(Actions.asIconButton(new RefreshAction(i18n),
                new RefreshableView((Refreshable) actionSource.getSource())));
        toolbar.addSeparator();
        toolbar.addMember(Actions.asIconButton(modsEditAction, actionSource));
        toolbar.addMember(Actions.asIconButton(noteEditAction, actionSource));
        toolbar.addMember(Actions.asIconButton(parentEditAction, actionSource));
        toolbar.addMember(Actions.asIconButton(mediaEditAction, actionSource));
        toolbar.addMember(Actions.asIconButton(ocrEditAction, actionSource));
        toolbar.addMember(Actions.asIconButton(childrenEditAction, actionSource));
        toolbar.addMember(Actions.asIconButton(atmEditAction, actionSource));
        toolbar.addSeparator();
        toolbar.addMember(Actions.asIconButton(foxmlAction, actionSource));
        toolbar.addMember(btnExport);
        toolbar.addMember(Actions.asIconButton(deleteAction, actionSource));
        toolbar.addMember(Actions.asIconButton(registerUrnNbnAction, actionSource));
        if (menuType == MenuType.TREE_VIEW) {
            toolbar.addMember(Actions.asIconButton(expandTreeAction, actionSource));
        }
    }

    private void initContextMenu(Menu menu, ActionSource actionSource) {
        menu.addItem(Actions.asMenuItem(modsEditAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(noteEditAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(parentEditAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(mediaEditAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(ocrEditAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(childrenEditAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(atmEditAction, actionSource, false));
        menu.addItem(new MenuItemSeparator());
        menu.addItem(Actions.asMenuItem(foxmlAction, actionSource, true));
        menu.addItem(new MenuItemSeparator());
        menu.addItem(Actions.asMenuItem(archiveExportAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(krameriusExportAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(ndkExportAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(ndkOldPrintExportAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(cejshExportAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(crossrefExportAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(desaExportAction, actionSource, true));
        menu.addItem(Actions.asMenuItem(desaDownloadAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(fullDataStreamExportAction, actionSource, false));
        menu.addItem(Actions.asMenuItem(rawDataStreamExportAction, actionSource, false));
        menu.addItem(new MenuItemSeparator());
        menu.addItem(Actions.asMenuItem(deleteAction, actionSource, true));
        menu.addItem(Actions.asMenuItem(registerUrnNbnAction, actionSource, true));
        menu.addItem(Actions.asMenuItem(expandTreeAction, actionSource, true));
    }

    private final class RefreshableView implements Refreshable {

        private final Refreshable delegate;

        RefreshableView(Refreshable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void refresh() {
            fetchModels(true);
            delegate.refresh();
        }
    }

    public enum MenuType {
        TREE_VIEW, FOUND_VIEW
    }
}


