/*
 * Copyright (C) 2021 Lukas Sykora
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
package cz.cas.lib.proarc.webapp.client.action.administration.changeModels;

import cz.cas.lib.proarc.common.object.emods.BornDigitalModsPlugin;
import cz.cas.lib.proarc.common.object.ndk.NdkAudioPlugin;
import cz.cas.lib.proarc.common.object.ndk.NdkPlugin;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.Editor;
import cz.cas.lib.proarc.webapp.client.action.AbstractAction;
import cz.cas.lib.proarc.webapp.client.action.ActionEvent;
import cz.cas.lib.proarc.webapp.client.action.Actions;
import cz.cas.lib.proarc.webapp.client.ds.ChangeModelsDataSource;
import cz.cas.lib.proarc.webapp.client.ds.DigitalObjectDataSource.DigitalObject;
import cz.cas.lib.proarc.webapp.client.ds.RestConfig;
import cz.cas.lib.proarc.webapp.client.widget.StatusView;
import cz.cas.lib.proarc.webapp.client.widget.UserRole;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;

/**
 * Transfer STT page to NdkPage
 *
 * @author Lukas Sykora
 */
public class ChangeSttPageToNdkPageAction extends AbstractAction {

    private final ClientMessages i18n;

    public ChangeSttPageToNdkPageAction(ClientMessages i18n) {
        this(i18n, i18n.ChangeSttPageToNdkPageAction_Title(),
                "[SKIN]/headerIcons/transfer.png",
                i18n.ChangeModelAction_Hint());
    }

    public ChangeSttPageToNdkPageAction(ClientMessages i18n, String title, String icon, String tooltip) {
        super(title, icon, tooltip);
        this.i18n = i18n;
    }

    @Override
    public boolean accept(ActionEvent event) {
        if (!(Editor.getInstance().hasPermission("proarc.permission.admin") || Editor.getInstance().hasPermission(UserRole.ROLE_SUPERADMIN) || Editor.getInstance().hasPermission(UserRole.PERMISSION_RUN_CHANGE_MODEL_FUNCTION))) {
            return false;
        }
        Object[] selection = Actions.getSelection(event);
        boolean accept = false;
        if (selection != null && selection instanceof Record[]) {
            Record[] records = (Record[]) selection;
            accept = acceptModel(records);
        }
        return accept;
    }

    @Override
    public void performAction(ActionEvent event) {
        Record[] records = Actions.getSelection(event);
        Record record = new Record();
        for (Record recordLocal : records){
            DigitalObject dobj = DigitalObject.createOrNull(recordLocal);
            if (dobj != null) {
                record = recordLocal;
                continue;
            }
        }
        register(record);
    }

    private boolean acceptModel(Record[] records) {
        boolean accept = false;
        for (Record record : records) {
            DigitalObject dobj = DigitalObject.createOrNull(record);
            if (dobj != null) {
                String modelId = dobj.getModelId();
                if (NdkPlugin.MODEL_NDK_PAGE.equals(modelId)
                        || NdkPlugin.MODEL_PAGE.equals(modelId)
                        || BornDigitalModsPlugin.MODEL_ARTICLE.equals(modelId)
                        || NdkAudioPlugin.MODEL_PAGE.equals(modelId)) {
                    accept = false;
                    continue;
                } else if (modelId != null) {
                    accept = true;
                    continue;
                }
            }
            accept = false;
            break;
        }
        return accept;
    }

    private void register(Record record) {
        DSRequest dsRequest = new DSRequest();
        dsRequest.setHttpMethod("POST");
        ChangeModelsDataSource ds = ChangeModelsDataSource.changeSttPageToNdkPage();
        ds.addData(record, new DSCallback() {
            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (RestConfig.isStatusOk(response)) {
                    StatusView.getInstance().show(i18n.ChangeSttPageToNdkPageAction_FinishStep_Msg());
                }
            }
        }, dsRequest);
    }
}
