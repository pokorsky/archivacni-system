/*
 * Copyright (C) 2013 Jan Pokorsky
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
package cz.cas.lib.proarc.webapp.client.ds;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.FieldType;
import cz.cas.lib.proarc.webapp.shared.rest.DigitalObjectResourceApi;
import java.util.logging.Logger;

/**
 * Data source to manage ATM of digital objects.
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectAdministrationDataSource extends ProarcDataSource {

    private static final Logger LOG = Logger.getLogger(DigitalObjectAdministrationDataSource.class.getName());
    public static final String ID = "DigitalObjectAdministrationDataSource";

    public static final String FIELD_PID = DigitalObjectResourceApi.ATM_ITEM_PID;
    public static final String FIELD_MODEL = DigitalObjectResourceApi.ATM_ITEM_MODEL;
    public static final String FIELD_OWNER = DigitalObjectResourceApi.ATM_ITEM_OWNER;
    public static final String FIELD_STATE = DigitalObjectResourceApi.ATM_ITEM_STATE;
    public static final String FIELD_CREATED = DigitalObjectResourceApi.ATM_ITEM_CREATED;
    public static final String FIELD_MODIFIED = DigitalObjectResourceApi.ATM_ITEM_MODIFIED;
    public static final String FIELD_DEVICE = DigitalObjectResourceApi.ATM_ITEM_DEVICE;
    public static final String FIELD_FILENAME = DigitalObjectResourceApi.ATM_ITEM_FILENAME;
    public static final String FIELD_EXPORT = DigitalObjectResourceApi.ATM_ITEM_EXPORTRESULT;
    public static final String FIELD_NDK_EXPORT = DigitalObjectResourceApi.ATM_ITEM_NDK_EXPORT;
    public static final String FIELD_ARCHIVE_EXPORT = DigitalObjectResourceApi.ATM_ITEM_ARCHIVE_EXPORT;
    public static final String FIELD_KRAMERIUS_EXPORT = DigitalObjectResourceApi.ATM_ITEM_KRAMERIUS_EXPORT;
    public static final String FIELD_CROSSREF_EXPORT = DigitalObjectResourceApi.ATM_ITEM_CROSSREF_EXPORT;
    public static final String FIELD_ORGANIZATION = DigitalObjectResourceApi.ATM_ITEM_ORGANIZATION;
    public static final String FIELD_STATUS = DigitalObjectResourceApi.ATM_ITEM_STATUS;
    public static final String FIELD_USER = DigitalObjectResourceApi.ATM_ITEM_USER;


    public static DigitalObjectAdministrationDataSource getInstance() {
        DigitalObjectAdministrationDataSource ds = (DigitalObjectAdministrationDataSource) DataSource.get(ID);
        return  ds != null ? ds : new DigitalObjectAdministrationDataSource();
    }

    public DigitalObjectAdministrationDataSource() {
        setID(ID);

        setDataURL(RestConfig.URL_DIGOBJECT_ATM);

        DataSourceField pid = new DataSourceField(FIELD_PID, FieldType.TEXT);
        pid.setPrimaryKey(true);
        pid.setRequired(true);

        DataSourceTextField created = new DataSourceTextField(FIELD_CREATED);
        DataSourceTextField device = new DataSourceTextField(FIELD_DEVICE);
        DataSourceTextField filename = new DataSourceTextField(FIELD_FILENAME);
        DataSourceTextField model = new DataSourceTextField(FIELD_MODEL);
        DataSourceTextField modified = new DataSourceTextField(FIELD_MODIFIED);
        DataSourceTextField owner = new DataSourceTextField(FIELD_OWNER);
        DataSourceTextField state = new DataSourceTextField(FIELD_STATE);
        DataSourceTextField export = new DataSourceTextField(FIELD_EXPORT);
        DataSourceTextField ndkExport = new DataSourceTextField(FIELD_NDK_EXPORT);
        DataSourceTextField archiveExport = new DataSourceTextField(FIELD_ARCHIVE_EXPORT);
        DataSourceTextField krameriusExport = new DataSourceTextField(FIELD_KRAMERIUS_EXPORT);
        DataSourceTextField crossrefExport = new DataSourceTextField(FIELD_CROSSREF_EXPORT);
        DataSourceTextField organization = new DataSourceTextField(FIELD_ORGANIZATION);
        DataSourceTextField status = new DataSourceTextField(FIELD_STATUS);
        DataSourceTextField user = new DataSourceTextField(FIELD_USER);

        setFields(pid, model, state, owner, created, modified, device, filename, export, organization, user, ndkExport, archiveExport, krameriusExport, crossrefExport, status);

        setRequestProperties(RestConfig.createRestRequest(getDataFormat()));
        setOperationBindings(
                RestConfig.createUpdateOperation()
                );
    }

    public DigitalObjectAdministrationDataSource(JavaScriptObject jsObj) {
        super(jsObj);
    }

}
