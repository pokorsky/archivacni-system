/*
 * Copyright (C) 2019 Lukas Sykora
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

import com.smartgwt.client.data.fields.DataSourceTextField;
import cz.cas.lib.proarc.webapp.shared.rest.DigitalObjectResourceApi;

/**
 *
 * @author Lukas Sykora
 */
public class UpdateObjectDataSource extends ProarcDataSource {

    public static final String FIELD_PID = DigitalObjectResourceApi.DIGITALOBJECT_PID;
    public static final String FIELD_MODEL = DigitalObjectResourceApi.DIGITALOBJECT_MODEL;

    private UpdateObjectDataSource(String type) {
        setDataURL(type);
        DataSourceTextField pid = new DataSourceTextField(FIELD_PID);
        DataSourceTextField model = new DataSourceTextField(FIELD_MODEL);
        setFields(pid, model);
        setOperationBindings(RestConfig.createAddOperation());
    }

    public static UpdateObjectDataSource updateNdkArticle() {
        return new UpdateObjectDataSource(RestConfig.URL_UPDATE_NDK_ARTICLE);
    }

    public static UpdateObjectDataSource updateNdkPage() {
        return new UpdateObjectDataSource(RestConfig.URL_UPDATE_NDK_PAGE);
    }

    public static UpdateObjectDataSource updateOldprintPage() {
        return new UpdateObjectDataSource(RestConfig.URL_UPDATE_OLDPRINT_PAGE);
    }
}
