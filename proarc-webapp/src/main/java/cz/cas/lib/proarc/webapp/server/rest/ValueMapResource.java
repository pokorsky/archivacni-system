/*
 * Copyright (C) 2014 Jan Pokorsky
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
package cz.cas.lib.proarc.webapp.server.rest;

import cz.cas.lib.proarc.common.object.DigitalObjectPlugin;
import cz.cas.lib.proarc.common.object.ValueMap;
import cz.cas.lib.proarc.common.object.model.MetaModel;
import cz.cas.lib.proarc.common.object.model.MetaModelRepository;
import cz.cas.lib.proarc.webapp.shared.rest.ValueMapResourceApi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Provides value maps used by form pick lists.
 *
 * @author Jan Pokorsky
 */
@Path(ValueMapResourceApi.PATH)
public class ValueMapResource {

    private final SessionContext session;

    public ValueMapResource(
            @Context HttpServletRequest httpRequest
            ) {
        session = SessionContext.from(httpRequest);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<ValueMap> getValueMaps() {
        ArrayList<ValueMap> result = new ArrayList<ValueMap>();
        result.addAll(searchPlugins());
        return new SmartGwtResponse<ValueMap>(result);
    }

    private List<ValueMap> searchPlugins() {
        ArrayList<ValueMap> result = new ArrayList<ValueMap>();
        HashMap<String, DigitalObjectPlugin> pluginCache = new HashMap<String, DigitalObjectPlugin>();
        for (MetaModel model : MetaModelRepository.getInstance().find()) {
            DigitalObjectPlugin plugin = model.getPlugin();
            pluginCache.put(plugin.getId(), plugin);
        }
        for (DigitalObjectPlugin plugin : pluginCache.values()) {
            List<ValueMap> vms = plugin.getValueMaps(session.getUser());
            result.addAll(vms);
        }
        return result;
    }

}
