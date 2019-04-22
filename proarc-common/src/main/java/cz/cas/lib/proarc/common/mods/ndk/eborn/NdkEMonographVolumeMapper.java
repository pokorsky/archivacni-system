/*
 * Copyright (C) 2018 Martin Rumanek
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

package cz.cas.lib.proarc.common.mods.ndk.eborn;

import cz.cas.lib.proarc.common.export.mets.Const;
import cz.cas.lib.proarc.common.mods.ndk.MapperUtils;
import cz.cas.lib.proarc.common.mods.ndk.NdkMonographVolumeMapper;
import cz.cas.lib.proarc.mods.DigitalOriginDefinition;
import cz.cas.lib.proarc.mods.ModsDefinition;
import cz.cas.lib.proarc.mods.PhysicalDescriptionDefinition;

public class NdkEMonographVolumeMapper extends NdkMonographVolumeMapper {
    private boolean addTextResource = true;

    public boolean isAddTextResource() {
        return addTextResource;
    }

    @Override
    public void createMods(ModsDefinition mods, Context ctx) {
        super.createMods(mods, ctx);
    }

    @Override
    protected void addGenre(ModsDefinition mods) {
        //  mods/genre="electronic_volume"
        MapperUtils.addGenre(mods, Const.GENRE_EVOLUME);
        mods.getPhysicalDescription().stream().map(PhysicalDescriptionDefinition::getDigitalOrigin).filter(origin -> origin.isEmpty()).forEach(origin -> origin.add(DigitalOriginDefinition.BORN_DIGITAL));
    }
}
