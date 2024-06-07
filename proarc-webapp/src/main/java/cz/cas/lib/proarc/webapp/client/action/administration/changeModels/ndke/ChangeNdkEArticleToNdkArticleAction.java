/*
 * Copyright (C) 2024 Lukas Sykora
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
package cz.cas.lib.proarc.webapp.client.action.administration.changeModels.ndke;

import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import cz.cas.lib.proarc.common.object.ndk.NdkEbornPlugin;
import cz.cas.lib.proarc.common.object.ndk.NdkPlugin;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import cz.cas.lib.proarc.webapp.client.Editor;
import cz.cas.lib.proarc.webapp.client.action.AbstractAction;
import cz.cas.lib.proarc.webapp.client.action.ActionEvent;
import cz.cas.lib.proarc.webapp.client.action.Actions;
import cz.cas.lib.proarc.webapp.client.ds.ChangeModelsDataSource;
import cz.cas.lib.proarc.webapp.client.ds.DigitalObjectDataSource;
import cz.cas.lib.proarc.webapp.client.ds.RestConfig;
import cz.cas.lib.proarc.webapp.client.widget.StatusView;
import cz.cas.lib.proarc.webapp.client.widget.UserRole;
import cz.cas.lib.proarc.webapp.shared.rest.DigitalObjectResourceApi;

/**
 * Change Ndk EArticle to Ndk Article
 *
 * @author Lukas Sykora
 */
public class ChangeNdkEArticleToNdkArticleAction extends AbstractAction {

    private final ClientMessages i18n;

    public ChangeNdkEArticleToNdkArticleAction(ClientMessages i18n) {
        this(i18n, i18n.ChangeNdkEArticleToNdkArticleAction_Title(),
                "[SKIN]/headerIcons/transfer.png",
                i18n.ChangeModelAction_Hint());
    }

    public ChangeNdkEArticleToNdkArticleAction(ClientMessages i18n, String title, String icon, String tooltip) {
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
        String[] pids = ClientUtils.toFieldValues(records, DigitalObjectResourceApi.DIGITALOBJECT_PID);
        Record record = new Record();
        record.setAttribute(DigitalObjectResourceApi.DIGITALOBJECT_PID, pids);
        if (records != null && records.length > 0) {
            record.setAttribute(DigitalObjectResourceApi.DIGITALOBJECT_MODEL, records[0].getAttribute(DigitalObjectResourceApi.DIGITALOBJECT_MODEL));
        }
        changeModel(record);
    }

    private boolean acceptModel(Record[] records) {
        boolean accept = false;
        for (Record record : records) {
            DigitalObjectDataSource.DigitalObject dobj = DigitalObjectDataSource.DigitalObject.createOrNull(record);
            if (dobj != null) {
                String modelId = dobj.getModelId();
                if (modelId != null && (NdkEbornPlugin.MODEL_EARTICLE.equals(modelId) || NdkEbornPlugin.MODEL_EPERIODICALISSUE.equals(modelId)
                        || NdkEbornPlugin.MODEL_EPERIODICALVOLUME.equals(modelId) || NdkEbornPlugin.MODEL_EPERIODICAL.equals(modelId)
                        || NdkPlugin.MODEL_PERIODICAL.equals(modelId) || NdkPlugin.MODEL_PERIODICALVOLUME.equals(modelId)
                        || NdkPlugin.MODEL_PERIODICALISSUE.equals(modelId))) {
                    accept = true;
                    continue;
                }
            }
            accept = false;
            break;
        }
        return accept;
    }

    private void changeModel(Record record) {
        DSRequest dsRequest = new DSRequest();
        dsRequest.setHttpMethod("POST");
        ChangeModelsDataSource ds = ChangeModelsDataSource.changeNdkEArticleToNdkArticle();
        ds.addData(record, new DSCallback() {
            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (hasValidationError(response)) {
                    handleValidations(response);
                } else if (RestConfig.isStatusOk(response)) {
                    StatusView.getInstance().show(i18n.ChangeNdkEArticleToNdkArticleAction_FinishStep_Msg());
                }
            }
        }, dsRequest);
    }
}
