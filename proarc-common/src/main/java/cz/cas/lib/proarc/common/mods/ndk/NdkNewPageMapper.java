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

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.proarc.common.mods.custom.ModsConstants;
import cz.cas.lib.proarc.mods.DetailDefinition;
import cz.cas.lib.proarc.mods.ExtentDefinition;
import cz.cas.lib.proarc.mods.ModsDefinition;
import cz.cas.lib.proarc.mods.PartDefinition;
import cz.cas.lib.proarc.mods.PhysicalDescriptionDefinition;
import cz.cas.lib.proarc.mods.PhysicalDescriptionNote;
import cz.cas.lib.proarc.mods.RecordInfoDefinition;
import cz.cas.lib.proarc.mods.StringPlusLanguage;
import cz.cas.lib.proarc.mods.StringPlusLanguagePlusAuthority;
import cz.cas.lib.proarc.mods.TitleInfoDefinition;
import cz.cas.lib.proarc.oaidublincore.OaiDcType;
import java.io.IOException;
import java.util.List;
import static cz.cas.lib.proarc.common.mods.ndk.MapperUtils.addElementType;
import static cz.cas.lib.proarc.common.mods.ndk.MapperUtils.addSubTitle;
import static cz.cas.lib.proarc.common.mods.ndk.MapperUtils.addTitle;

public class NdkNewPageMapper extends NdkMapper {

    /** {@code /mods/part/detail@type} */
    private static final String NUMBER_TYPE_PAGE_INDEX = "pageIndex";
    /** {@code /mods/part/detail@type} */
    private static final String NUMBER_TYPE_PAGE_NUMBER = "pageNumber";
    /** {@code /mods/part@type} */
    public static final String PAGE_TYPE_NORMAL = "normalPage";


    @Override
    public void createMods(ModsDefinition mods, Context ctx) {
        super.createMods(mods, ctx);
    }

    @Override
    protected OaiDcType createDc(ModsDefinition mods, Context ctx) {
        OaiDcType dc = super.createDc(mods, ctx);
        for (TitleInfoDefinition titleInfo : mods.getTitleInfo()) {
            StringBuilder title = new StringBuilder();
            addTitle(title, titleInfo);
            addSubTitle(title, titleInfo);
            addElementType(dc.getTitles(), title.toString());
        }
        for (PartDefinition part : mods.getPart()) {
            for (ExtentDefinition extent : part.getExtent()) {
                addElementType(dc.getCoverages(), extent.getStart().getValue());
            }
        }
        addElementType(dc.getTypes(), "model:page");
        for (PhysicalDescriptionDefinition physicalDescription : mods.getPhysicalDescription()) {
            for (PhysicalDescriptionNote note : physicalDescription.getNote()) {
                addElementType(dc.getDescriptions(), note.getValue());
            }
        }
        return dc;
    }

    @Override
    protected String createObjectLabel(ModsDefinition mods) {
        StringBuilder sb = new StringBuilder();
        if (!mods.getPart().isEmpty()) {
            PartDefinition part = mods.getPart().get(0);
            if (getNumber(getDetail(part.getDetail(), NUMBER_TYPE_PAGE_NUMBER)) != null) {
                sb.append(getNumber(getDetail(part.getDetail(), NUMBER_TYPE_PAGE_NUMBER)));
            } else {
                sb.append('?');
            }
            if (part.getType() != null && !PAGE_TYPE_NORMAL.equalsIgnoreCase(part.getType())) {
                sb.append(", ").append(part.getType());
            }
        }
        return sb.toString();
    }

    private String getNumber(DetailDefinition detail) {
        if (detail != null) {
            List<StringPlusLanguage> numbers = detail.getNumber();
            if (!numbers.isEmpty()) {
                return numbers.get(0).getValue();
            }
        }
        return null;
    }

    private DetailDefinition getDetail(List<DetailDefinition> details, String type) {
        for (DetailDefinition detail : details) {
            if (type.equals(detail.getType())) {
                return detail;
            }
        }
        return null;
    }

    @Override
    public final RdaModsWrapper toJsonObject(ModsDefinition mods, NdkMapper.Context ctx) {
        NdkMapper.PageModsWrapper wrapper = new NdkMapper.PageModsWrapper();
        wrapper.setMods(mods);
        String pageType = mods.getPart().get(0).getType();
        mods.getPart().get(0).setType(null);
        wrapper.setType(pageType);

        if (mods.getPart().get(0).getDetail().isEmpty()) {
            return wrapper;
        }
        String pageIndex = getNumber(getDetail(mods.getPart().get(0).getDetail(), NUMBER_TYPE_PAGE_INDEX));
        setNumber(getDetail(mods.getPart().get(0).getDetail(), NUMBER_TYPE_PAGE_INDEX), null);

        String pageNumber = getNumber(getDetail(mods.getPart().get(0).getDetail(), NUMBER_TYPE_PAGE_NUMBER));
        setNumber(getDetail(mods.getPart().get(0).getDetail(), NUMBER_TYPE_PAGE_NUMBER), null);
        wrapper.setPageNumber(pageNumber);
        wrapper.setPageIndex(pageIndex);
        if (mods.getRecordInfo().isEmpty() || mods.getRecordInfo().get(0).getDescriptionStandard().isEmpty()) {
            return wrapper;
        }
        String descriptionStandard = mods.getRecordInfo().get(0).getDescriptionStandard().get(0).getValue();
        mods.getRecordInfo().get(0).getDescriptionStandard().clear();
        if (descriptionStandard.equalsIgnoreCase(ModsConstants.VALUE_DESCRIPTIONSTANDARD_RDA)) {
            wrapper.setRdaRules(true);
        } else {
            wrapper.setRdaRules(false);
        }
        if (mods.getPart().isEmpty()) {
            return wrapper;
        }

        return wrapper;

    }

    @Override
    public ModsDefinition fromJsonObject(ObjectMapper jsMapper, String json, Context ctx) throws IOException {

        PageModsWrapper wrapper = jsMapper.readValue(json, PageModsWrapper.class);
        ModsDefinition mods = wrapper.getMods();

        NdkPageMapper.Page page = jsMapper.readValue(json, NdkPageMapper.Page.class);

        StringPlusLanguagePlusAuthority descriptionStandard = new StringPlusLanguagePlusAuthority();
        if (wrapper.getRdaRules() != null && wrapper.getRdaRules()) {
            descriptionStandard.setValue(ModsConstants.VALUE_DESCRIPTIONSTANDARD_RDA);
        } else {
            descriptionStandard.setValue(ModsConstants.VALUE_DESCRIPTIONSTANDARD_AACR);
        }
        if (mods.getRecordInfo().isEmpty()) {
            RecordInfoDefinition recordInfo = new RecordInfoDefinition();
            recordInfo.getDescriptionStandard().add(0, descriptionStandard);
            mods.getRecordInfo().add(0, recordInfo);
        } else {
            mods.getRecordInfo().get(0).getDescriptionStandard().add(0, descriptionStandard);
        }

        String number = getValue(wrapper.getPageNumber(), page.getNumber());
        String index = getValue(wrapper.getPageIndex(), page.getIndex());
        String type = getValueRevers(wrapper.getType(), page.getType());

        if (type != null) {
            if (mods.getPart().isEmpty()) {
                PartDefinition part = new PartDefinition();
                mods.getPart().add(part);
            }
            mods.getPart().get(0).setType(type);
        }

        if (number != null) {
            if (mods.getPart().isEmpty()) {
                PartDefinition part = new PartDefinition();
                mods.getPart().add(part);
            }
            DetailDefinition detail = getDetail(mods.getPart().get(0).getDetail(), NUMBER_TYPE_PAGE_NUMBER);
            if (detail== null) {
                detail = new DetailDefinition();
                detail.setType(NUMBER_TYPE_PAGE_NUMBER);
                mods.getPart().get(0).getDetail().add(detail);
            }
            setNumber(detail, number);
        }
        if (index != null) {
            if (mods.getPart().isEmpty()) {
                PartDefinition part = new PartDefinition();
                mods.getPart().add(part);
            }
            DetailDefinition detail = getDetail(mods.getPart().get(0).getDetail(), NUMBER_TYPE_PAGE_INDEX);
            if (detail == null) {
                detail = new DetailDefinition();
                detail.setType(NUMBER_TYPE_PAGE_INDEX);
                mods.getPart().get(0).getDetail().add(detail);
            }
            setNumber(detail, index);
        }
        return mods;
    }

    private String getValueRevers(String wrapperValue, String pageValue) {
        return getValue(pageValue, wrapperValue);
    }

    private String getValue(String wrapperValue, String pageValue) {
        if (wrapperValue != null &&!wrapperValue.isEmpty()) {
            return wrapperValue;
        } else return pageValue;
    }

    private void setNumber(DetailDefinition detail, String number) {
        if (detail != null) {
            if (detail.getNumber().isEmpty()) {
                StringPlusLanguage stringPlusLanguage = new StringPlusLanguage();
                detail.getNumber().add(stringPlusLanguage);
            }
            detail.getNumber().get(0).setValue(number);
        }
    }

}