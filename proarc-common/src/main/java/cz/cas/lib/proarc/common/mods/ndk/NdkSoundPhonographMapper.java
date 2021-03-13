/*
 * Copyright (C) 2020 Lukas Sykora
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
package cz.cas.lib.proarc.common.mods.ndk;

import cz.cas.lib.proarc.mods.ModsDefinition;
import cz.cas.lib.proarc.oaidublincore.OaiDcType;

/**
 *
 * @author Lukas Sykora
 */
public class NdkSoundPhonographMapper extends NdkSoundCollectionMapper {

    /**
     * Updates missing required attribute and elements.
     */
    @Override
    public void createMods(ModsDefinition mods, Context ctx) {
        super.createMods(mods, ctx);
    }

    @Override
    protected OaiDcType createDc(ModsDefinition mods, Context ctx) {
        return super.createDc(mods, ctx);
    }
}
