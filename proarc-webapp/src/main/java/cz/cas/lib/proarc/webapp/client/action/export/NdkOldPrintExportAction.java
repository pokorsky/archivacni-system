/*
 * Copyright (C) 2018 Lukas Sykora
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

package cz.cas.lib.proarc.webapp.client.action.export;

import com.smartgwt.client.data.Record;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.shared.rest.ExportResourceApi;

/**
 * The NDK OldPrint export action.
 *
 * @author Lukas Sykora
 */
public class NdkOldPrintExportAction extends NdkExportAction {

    public NdkOldPrintExportAction (ClientMessages i18n){
        super(i18n, i18n.NdkOldPrintExportAction_Title(), null, i18n.NdkOldPrintExportAction_Hint());
    }

    @Override
    protected boolean isAcceptableModel(String modelId) {
        return modelId != null && isOldPrintModel(modelId);
    }

    protected boolean isOldPrintModel(String modelId) {
        return modelId.startsWith("model:old");
    }

    @Override
    protected void setAttributes(Record export, String[] pids) {
        export.setAttribute(ExportResourceApi.NDK_PID_PARAM, pids);
        export.setAttribute(ExportResourceApi.NDK_PACKAGE, ExportResourceApi.Package.STT.name());
    }
}
