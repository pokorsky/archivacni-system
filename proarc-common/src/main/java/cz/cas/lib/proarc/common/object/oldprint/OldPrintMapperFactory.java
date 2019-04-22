/*
 * Copyright (C) 2015 Jan Pokorsky
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
package cz.cas.lib.proarc.common.object.oldprint;

import cz.cas.lib.proarc.common.mods.ndk.NdkMapper;
import cz.cas.lib.proarc.common.mods.ndk.NdkMapperFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * It is expected to handle MODS of old prints the same way like NDK.
 *
 * @author Jan Pokorsky
 */
public class OldPrintMapperFactory extends NdkMapperFactory {

    private static final Map<String, Supplier<NdkMapper>> mappers = new HashMap<>();

    static {
        mappers.put(OldPrintPlugin.MODEL_PAGE, OldPrintPageMapper::new);
        mappers.put(OldPrintPlugin.MODEL_VOLUME, OldPrintVolumeMapper::new);
        mappers.put(OldPrintPlugin.MODEL_SUPPLEMENT, OldPrintSupplementMapper::new);
        mappers.put(OldPrintPlugin.MODEL_MONOGRAPHTITLE, OldPrintMonographTitleMapper::new);
        mappers.put(OldPrintPlugin.MODEL_CHAPTER, OldPrintChapterMapper::new);
    }

    @Override
    public NdkMapper get(String modelId) {
        Optional<Supplier<NdkMapper>> ndkMapper = Optional.ofNullable(mappers.get(modelId));
        return ndkMapper.map(s -> s.get()).orElseThrow(() -> new IllegalStateException("Unsupported model: " + modelId));
    }

}
