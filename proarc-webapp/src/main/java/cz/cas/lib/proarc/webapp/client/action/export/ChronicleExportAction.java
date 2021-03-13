/*
 * Copyright (C) 2020 Lukas Sykora
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

package cz.cas.lib.proarc.webapp.client.action.export;


import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.util.SC;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import cz.cas.lib.proarc.webapp.client.action.ActionEvent;
import cz.cas.lib.proarc.webapp.client.action.Actions;
import cz.cas.lib.proarc.webapp.client.ds.DigitalObjectDataSource;
import cz.cas.lib.proarc.webapp.client.ds.ExportDataSource;
import cz.cas.lib.proarc.webapp.client.ds.RestConfig;
import cz.cas.lib.proarc.webapp.shared.rest.ExportResourceApi;

public class ChronicleExportAction extends ExportAction {

    public ChronicleExportAction(ClientMessages i18n) {
        super(i18n, i18n.NdkChronicleExportAction_Title(), null, i18n.NdkChronicleExportAction_Hint());
    }

    @Override
    public void performAction(ActionEvent event) {
        Record[] records = Actions.getSelection(event);
        String[] pids = ClientUtils.toFieldValues(records, ExportResourceApi.NDK_PID_PARAM);
        if (pids == null || pids.length == 0) {
            return ;
        }
        Record export = new Record();
        export.setAttribute(ExportResourceApi.NDK_PID_PARAM, pids);
        export.setAttribute(ExportResourceApi.NDK_PACKAGE, ExportResourceApi.Package.CHRONICLE.name());
        exportOrValidate(export);
    }

    private void exportOrValidate(final Record export) {
        DSRequest dsRequest = new DSRequest();
        dsRequest.setShowPrompt(false);
        DataSource ds = ExportDataSource.getNdk();

        dsAddData(ds, export, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (RestConfig.isStatusOk(response)) {
                    Record[] data = response.getData();
                    RecordList erl = errorsFromExportResult(data);
                    if (erl.isEmpty()) {
                        SC.say(i18n.NdkExportAction_ExportDone_Msg());
                    } else {
                        DesaExportAction.ExportResultWidget.showErrors(erl.toArray());
                    }
                }
            }
        }, dsRequest);
    }

    private RecordList errorsFromExportResult(Record[] exportResults) {
        RecordList recordList = new RecordList();
        for (Record result : exportResults) {
            Record[] errors = result.getAttributeAsRecordArray(ExportResourceApi.RESULT_ERRORS);
            if (errors != null && errors.length > 0) {
                recordList.addList(errors);
            }
        }
        return recordList;
    }

    @Override
    public boolean accept(ActionEvent event) {
        Object[] selection = Actions.getSelection(event);
        boolean accept = false;
        if (selection != null && selection instanceof Record[]) {
            Record[] records = (Record[]) selection;
            accept = acceptRecord(records);
        }
        return accept;
    }

    private boolean acceptRecord(Record[] records) {
        boolean accept = false;
        for (Record record : records) {
            DigitalObjectDataSource.DigitalObject dobj = DigitalObjectDataSource.DigitalObject.createOrNull(record);
            if (dobj != null) {
                String modelId = dobj.getModelId();
                if (modelId != null && modelId.startsWith("model:chronicle")) {
                    accept = true;
                    continue;
                }
            }
            accept = false;
            break;
        }
        return accept;
    }
}
