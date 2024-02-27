/*
 * Copyright (C) 2016 Jan Pokorsky
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
package cz.cas.lib.proarc.common.object;

import cz.cas.lib.proarc.common.storage.DigitalObjectException;
import cz.cas.lib.proarc.common.storage.ProArcObject;
import cz.cas.lib.proarc.common.storage.Storage;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

/**
 * The Read-only dissemination handler.
 *
 * @author Jan Pokorsky
 */
public class ReadonlyDisseminationHandler implements DisseminationHandler {

    private final ProArcObject object;
    private final String dsId;

    public ReadonlyDisseminationHandler(ProArcObject object, String dsId) {
        this.object = object;
        this.dsId = dsId;
    }

    @Override
    public Response getDissemination(Request httpRequest) throws DigitalObjectException {
        return DefaultDisseminationHandler.getResponse(object, dsId);
    }

    @Override
    public void setDissemination(DisseminationInput input, Storage storageType, String message) throws DigitalObjectException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void deleteDissemination(String message) throws DigitalObjectException {
        throw new UnsupportedOperationException("Not supported.");
    }

}
