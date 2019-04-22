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
package cz.cas.lib.proarc.common.object.ndk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourmediashelf.fedora.client.FedoraClientException;
import cz.cas.lib.proarc.common.config.AppConfiguration;
import cz.cas.lib.proarc.common.config.AppConfigurationException;
import cz.cas.lib.proarc.common.config.AppConfigurationFactory;
import cz.cas.lib.proarc.common.dublincore.DcStreamEditor;
import cz.cas.lib.proarc.common.dublincore.DcStreamEditor.DublinCoreRecord;
import cz.cas.lib.proarc.common.export.mets.ValidationErrorHandler;
import cz.cas.lib.proarc.common.fedora.DigitalObjectException;
import cz.cas.lib.proarc.common.fedora.DigitalObjectValidationException;
import cz.cas.lib.proarc.common.fedora.FedoraObject;
import cz.cas.lib.proarc.common.fedora.FoxmlUtils;
import cz.cas.lib.proarc.common.fedora.PageView.PageViewHandler;
import cz.cas.lib.proarc.common.fedora.PageView.PageViewItem;
import cz.cas.lib.proarc.common.fedora.RemoteStorage;
import cz.cas.lib.proarc.common.fedora.SearchView;
import cz.cas.lib.proarc.common.fedora.SearchView.Item;
import cz.cas.lib.proarc.common.fedora.SearchView.Query;
import cz.cas.lib.proarc.common.fedora.XmlStreamEditor;
import cz.cas.lib.proarc.common.fedora.relation.RelationEditor;
import cz.cas.lib.proarc.common.json.JsonUtils;
import cz.cas.lib.proarc.common.mods.ModsStreamEditor;
import cz.cas.lib.proarc.common.mods.ModsUtils;
import cz.cas.lib.proarc.common.mods.custom.ModsConstants;
import cz.cas.lib.proarc.common.mods.custom.ModsCutomEditorType;
import cz.cas.lib.proarc.common.mods.ndk.NdkMapper;
import cz.cas.lib.proarc.common.mods.ndk.NdkMapper.Context;
import cz.cas.lib.proarc.common.mods.ndk.NdkMapperFactory;
import cz.cas.lib.proarc.common.mods.ndk.NdkPageMapper;
import cz.cas.lib.proarc.common.mods.ndk.NdkPageMapper.Page;
import cz.cas.lib.proarc.common.object.DescriptionMetadata;
import cz.cas.lib.proarc.common.object.DigitalObjectCrawler;
import cz.cas.lib.proarc.common.object.DigitalObjectElement;
import cz.cas.lib.proarc.common.object.DigitalObjectHandler;
import cz.cas.lib.proarc.common.object.DigitalObjectManager;
import cz.cas.lib.proarc.common.object.MetadataHandler;
import cz.cas.lib.proarc.common.object.model.MetaModel;
import cz.cas.lib.proarc.common.object.model.MetaModelRepository;
import cz.cas.lib.proarc.mods.DateDefinition;
import cz.cas.lib.proarc.mods.FormDefinition;
import cz.cas.lib.proarc.mods.IdentifierDefinition;
import cz.cas.lib.proarc.mods.LocationDefinition;
import cz.cas.lib.proarc.mods.ModsDefinition;
import cz.cas.lib.proarc.mods.OriginInfoDefinition;
import cz.cas.lib.proarc.mods.PhysicalDescriptionDefinition;
import cz.cas.lib.proarc.mods.PhysicalLocationDefinition;
import cz.cas.lib.proarc.mods.RecordInfoDefinition;
import cz.cas.lib.proarc.mods.RelatedItemDefinition;
import cz.cas.lib.proarc.mods.StringPlusLanguage;
import cz.cas.lib.proarc.mods.StringPlusLanguagePlusAuthority;
import cz.cas.lib.proarc.mods.TitleInfoDefinition;
import cz.cas.lib.proarc.oaidublincore.OaiDcType;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.bind.DataBindingException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

/**
 * Handles description metadata in the NDK format.
 *
 * @author Jan Pokorsky
 */
public class NdkMetadataHandler implements MetadataHandler<ModsDefinition>, PageViewHandler {

    public static final String ERR_NDK_CHANGE_MODS_WITH_URNNBN = "Err_Ndk_Change_Mods_With_UrnNbn";
    public static final String ERR_NDK_CHANGE_MODS_WITH_MEMBERS = "Err_Ndk_Change_Mods_With_Members";
    public static final String ERR_NDK_DOI_DUPLICITY = "Err_Ndk_Doi_Duplicity";
    public static final String ERR_NDK_REMOVE_URNNBN = "Err_Ndk_Remove_UrnNbn";
    public static final String DEFAULT_PAGE_TYPE = "normalPage";

    /**
     * The set of model IDs that should be checked for connected members.
     */
    private static final Set<String> HAS_MEMBER_VALIDATION_MODELS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            NdkPlugin.MODEL_MONOGRAPHTITLE, NdkPlugin.MODEL_PERIODICAL, NdkPlugin.MODEL_PERIODICALVOLUME
    )));
    private static final Logger LOG = Logger.getLogger(NdkMetadataHandler.class.getName());

    protected final DigitalObjectHandler handler;
    protected final ModsStreamEditor editor;
    protected final FedoraObject fobject;
    protected DigitalObjectCrawler crawler;
    private final NdkMapperFactory mapperFactory;
    private static AppConfiguration appConfiguration;

    public NdkMetadataHandler(DigitalObjectHandler handler) {
        this(handler, new NdkMapperFactory());
    }

    public NdkMetadataHandler(DigitalObjectHandler handler, NdkMapperFactory mapperFactory) {
        this.handler = handler;
        this.fobject = handler.getFedoraObject();
        XmlStreamEditor streamEditor = fobject.getEditor(FoxmlUtils.inlineProfile(
                DESCRIPTION_DATASTREAM_ID, ModsConstants.NS, DESCRIPTION_DATASTREAM_LABEL));
        this.editor = new ModsStreamEditor(streamEditor, fobject);
        this.mapperFactory = mapperFactory;
        try {
            this.appConfiguration = AppConfigurationFactory.getInstance().defaultInstance();
        } catch (AppConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMetadata(DescriptionMetadata<ModsDefinition> data, String message, String typeRecord) throws DigitalObjectException {
        ModsDefinition mods = data.getData();
        String modelId = handler.relations().getModel();
        if (mods == null) {
            mods = createDefault(modelId);
        }
        write(modelId, mods, data, message, typeRecord);
    }

    /**
     * Creates a new MODS with required default values according to model ID.
     * Override to support custom models.
     */
    protected ModsDefinition createDefault(String modelId) throws DigitalObjectException {
        ModsDefinition defaultMods = ModsStreamEditor.defaultMods(fobject.getPid());
        DigitalObjectHandler parent = handler.getParameterParent();
        if (RdaRules.HAS_MEMBER_RDA_VALIDATION_MODELS.contains(modelId)) {
            setRules(defaultMods);
        }
        if (NdkPlugin.MODEL_PERIODICALISSUE.equals(modelId)) {
            // issue 124
            DigitalObjectHandler title = findEnclosingObject(parent, NdkPlugin.MODEL_PERIODICAL);
            if (title != null) {
                ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
                inheritTitleInfo(defaultMods, titleMods.getTitleInfo());
                defaultMods.getLanguage().addAll(titleMods.getLanguage());
                inheritLocation(defaultMods, titleMods.getLocation());
                inheritIdentifier(defaultMods, titleMods.getIdentifier(), "ccnb", "issn");
            }
            String partNumberVal = handler.getParameter(DigitalObjectHandler.PARAM_PART_NUMBER);
            String dateIssuedVal = handler.getParameter(DigitalObjectHandler.PARAM_ISSUE_DATE);
            fillIssueSeries(defaultMods, partNumberVal, dateIssuedVal);
        } else if (NdkPlugin.MODEL_PERIODICALSUPPLEMENT.equals(modelId)) {
            // issue 137
            DigitalObjectHandler title = findEnclosingObject(parent, NdkPlugin.MODEL_PERIODICAL);
            if (title != null) {
                ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
                inheritSupplementTitleInfo(defaultMods, titleMods.getTitleInfo());
                defaultMods.getLanguage().addAll(titleMods.getLanguage());
                inheritIdentifier(defaultMods, titleMods.getIdentifier(), "ccnb", "issn");
            }
        } else if (NdkPlugin.MODEL_MONOGRAPHSUPPLEMENT.equals(modelId)) {
            // issue 240
            DigitalObjectHandler title = findEnclosingObject(parent, NdkPlugin.MODEL_MONOGRAPHVOLUME);
            if (title != null) {
                ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
                inheritSupplementTitleInfo(defaultMods, titleMods.getTitleInfo());
                defaultMods.getLanguage().addAll(titleMods.getLanguage());
                inheritIdentifier(defaultMods, titleMods.getIdentifier(), "ccnb", "isbn");
                inheritOriginInfoDateIssued(defaultMods, titleMods.getOriginInfo());
                inheritPhysicalDescriptionForm(defaultMods, titleMods.getPhysicalDescription());
            }
            title = findEnclosingObject(parent, NdkAudioPlugin.MODEL_MUSICDOCUMENT);
            if (title != null) {
                ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
                defaultMods.getTitleInfo().addAll(titleMods.getTitleInfo());
                defaultMods.getName().addAll(titleMods.getName());
                defaultMods.getOriginInfo().addAll(titleMods.getOriginInfo());
                defaultMods.getPhysicalDescription().addAll(titleMods.getPhysicalDescription());
                defaultMods.getLanguage().addAll(titleMods.getLanguage());
                defaultMods.getTableOfContents().addAll(titleMods.getTableOfContents());
                defaultMods.getNote().addAll(titleMods.getNote());
                defaultMods.getSubject().addAll(titleMods.getSubject());
            }
        } else if (NdkPlugin.MODEL_CHAPTER.equals(modelId)) {
            // issue 241
            DigitalObjectHandler title = findEnclosingObject(parent, NdkPlugin.MODEL_MONOGRAPHVOLUME);
            if (title != null) {
                ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
                defaultMods.getLanguage().addAll(titleMods.getLanguage());
                inheritIdentifier(defaultMods, titleMods.getIdentifier(), "ccnb", "isbn");
                inheritPhysicalDescriptionForm(defaultMods, titleMods.getPhysicalDescription());
            }
        } else if (NdkPlugin.MODEL_MONOGRAPHVOLUME.equals(modelId)) {
            //issue 540
            DigitalObjectHandler title = findEnclosingObject(parent, NdkPlugin.MODEL_MONOGRAPHTITLE);
            if (title != null) {
                ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
                defaultMods.getTitleInfo().addAll(titleMods.getTitleInfo());
                defaultMods.getOriginInfo().addAll(titleMods.getOriginInfo());
            }
        } else if (NdkEbornPlugin.MODEL_EPERIODICALISSUE.equals(modelId)) {
            // issue 124
            DigitalObjectHandler title = findEnclosingObject(parent, NdkEbornPlugin.MODEL_EPERIODICAL);
            if (title != null) {
                ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
                inheritTitleInfo(defaultMods, titleMods.getTitleInfo());
                defaultMods.getLanguage().addAll(titleMods.getLanguage());
                inheritLocation(defaultMods, titleMods.getLocation());
                inheritIdentifier(defaultMods, titleMods.getIdentifier(), "ccnb", "issn");
            }
            String partNumberVal = handler.getParameter(DigitalObjectHandler.PARAM_PART_NUMBER);
            String dateIssuedVal = handler.getParameter(DigitalObjectHandler.PARAM_ISSUE_DATE);
            fillIssueSeries(defaultMods, partNumberVal, dateIssuedVal);
        } else if (NdkEbornPlugin.MODEL_EARTICLE.equals(modelId)) {
            copyEArticle(parent, defaultMods);
        } else if (NdkAudioPlugin.MODEL_SONG.equals(modelId)) {
            DigitalObjectHandler title = findEnclosingObject(parent, NdkAudioPlugin.MODEL_MUSICDOCUMENT);
            if (title != null) {
                ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
                defaultMods.getTitleInfo().addAll(titleMods.getTitleInfo());
                defaultMods.getName().addAll(titleMods.getName());
                defaultMods.getOriginInfo().addAll(titleMods.getOriginInfo());
                defaultMods.getPhysicalDescription().addAll(titleMods.getPhysicalDescription());
                defaultMods.getLanguage().addAll(titleMods.getLanguage());
                defaultMods.getTableOfContents().addAll(titleMods.getTableOfContents());
                defaultMods.getNote().addAll(titleMods.getNote());
                defaultMods.getSubject().addAll(titleMods.getSubject());
            }
        } else if (NdkAudioPlugin.MODEL_TRACK.equals(modelId)) {
            DigitalObjectHandler prent1 = handler.getParameterParent();
                if (NdkAudioPlugin.MODEL_MUSICDOCUMENT.equals(parent.relations().getModel())) {
                    DigitalObjectHandler title = findEnclosingObject(parent, NdkAudioPlugin.MODEL_MUSICDOCUMENT);
                    modsCopyMusicDocument(title, defaultMods);
                } else if (NdkAudioPlugin.MODEL_SONG.equals(parent.relations().getModel())) {
                    DigitalObjectHandler title = findEnclosingObject(parent, NdkAudioPlugin.MODEL_SONG);
                    modsCopyMusicDocument(title, defaultMods);
            }
        }

        return defaultMods;
    }

    private void copyEArticle(DigitalObjectHandler parent, ModsDefinition defaultMods) throws DigitalObjectException {
        // issue 859
        RelatedItemDefinition relatedItem = new RelatedItemDefinition();
        defaultMods.getRelatedItem().add(relatedItem);
        DigitalObjectHandler title = findEnclosingObject(parent, NdkEbornPlugin.MODEL_EPERIODICAL);
        if (title != null) {
            ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
            if (titleMods.getTitleInfo().size() != 0) {
                relatedItem.getTitleInfo().add(titleMods.getTitleInfo().get(0));
            }
            relatedItem.getName().addAll(titleMods.getName());
            copyIdentifier(relatedItem, titleMods, "issn");
        }
        DigitalObjectHandler issue = findEnclosingObject(parent, NdkEbornPlugin.MODEL_EPERIODICALISSUE);
        if (issue != null) {
            ModsDefinition issueMods = issue.<ModsDefinition>metadata().getMetadata().getData();
            if (relatedItem.getTitleInfo().size() != 0
                    && issueMods.getTitleInfo().size() != 0
                    && issueMods.getTitleInfo().get(0).getPartNumber().size() != 0) {
                relatedItem.getTitleInfo().get(0).getPartNumber().add(issueMods.getTitleInfo().get(0).getPartNumber().get(0));
            }
            copyIdentifier(relatedItem, issueMods, "uuid");
        }
    }

    private void copyIdentifier(RelatedItemDefinition relatedItem, ModsDefinition mods, String key) {
        List<IdentifierDefinition> identifiers = mods.getIdentifier();
        if (key == null) {
            return;
        }
        for (IdentifierDefinition identifier : identifiers) {
            if (key.equals(identifier.getType())) {
                relatedItem.getIdentifier().add(identifier);
            }
        }

    }

    public void modsCopyMusicDocument(DigitalObjectHandler title, ModsDefinition defaultMods) throws DigitalObjectException {
        if (title != null) {
            ModsDefinition titleMods = title.<ModsDefinition>metadata().getMetadata().getData();
            defaultMods.getTitleInfo().addAll(titleMods.getTitleInfo());
            defaultMods.getName().addAll(titleMods.getName());
            defaultMods.getTypeOfResource().addAll(titleMods.getTypeOfResource());
            defaultMods.getPhysicalDescription().addAll(titleMods.getPhysicalDescription());
        }
    }

    private void setRules(ModsDefinition mods) {
        StringPlusLanguagePlusAuthority descriptionStandard = new StringPlusLanguagePlusAuthority();
        String rules = appConfiguration.getRules();
        descriptionStandard.setValue(ModsConstants.VALUE_DESCRIPTIONSTANDARD_AACR.equalsIgnoreCase(rules)? ModsConstants.VALUE_DESCRIPTIONSTANDARD_AACR : ModsConstants.VALUE_DESCRIPTIONSTANDARD_RDA);
        RecordInfoDefinition recordInfo = new RecordInfoDefinition();
        recordInfo.getDescriptionStandard().add(0, descriptionStandard);
        mods.getRecordInfo().add(0, recordInfo);
    }

    private void fillIssueSeries(ModsDefinition mods, String partNumberVal, String dateIssuedVal) {
        if (partNumberVal != null) {
            TitleInfoDefinition titleInfo = mods.getTitleInfo().stream()
                    .filter(ti -> ti.getType() == null).findFirst().orElse(null);
            if (titleInfo == null) {
                mods.getTitleInfo().add(titleInfo = new TitleInfoDefinition());
            }
            StringPlusLanguage partNumber = titleInfo.getPartNumber().stream().findFirst().orElse(null);
            if (partNumber == null) {
                titleInfo.getPartNumber().add(partNumber = new StringPlusLanguage());
            }
            partNumber.setValue(partNumberVal);
        }
        if (dateIssuedVal != null) {
            OriginInfoDefinition originInfo = mods.getOriginInfo().stream().findFirst().orElse(null);
            if (originInfo == null) {
                mods.getOriginInfo().add(originInfo = new OriginInfoDefinition());
            }
            DateDefinition dateIssued = new DateDefinition();
            dateIssued.setValue(dateIssuedVal);
            originInfo.getDateIssued().add(dateIssued);
        }
    }

    protected final void inheritIdentifier(ModsDefinition mods, List<IdentifierDefinition> ids, String... includeIdTypes) {
        for (IdentifierDefinition id : ids) {
            String type = id.getType();
            if (includeIdTypes == null) {
                mods.getIdentifier().add(id);
            } else {
                for (String includeIdType : includeIdTypes) {
                    if (includeIdType.equals(type)) {
                        mods.getIdentifier().add(id);
                    }
                }
            }
        }
    }

    private void inheritLocation(ModsDefinition mods, List<LocationDefinition> locs) {
        for (LocationDefinition loc : locs) {
            List<PhysicalLocationDefinition> pls = loc.getPhysicalLocation();
            List<StringPlusLanguage> sls = loc.getShelfLocator();
            if (!pls.isEmpty() || !sls.isEmpty()) {
                loc.getUrl().clear();
                mods.getLocation().add(loc);
            }
        }
    }

    protected final void inheritOriginInfoDateIssued(ModsDefinition mods, List<OriginInfoDefinition> ois) {
        for (OriginInfoDefinition oi : ois) {
            OriginInfoDefinition newOi = null;
            for (DateDefinition dateIssued : oi.getDateIssued()) {
                if (newOi == null) {
                    newOi = new OriginInfoDefinition();
                    mods.getOriginInfo().add(newOi);
                }
                newOi.getDateIssued().add(dateIssued);
            }
        }
    }

    protected final void inheritPhysicalDescriptionForm(ModsDefinition mods, List<PhysicalDescriptionDefinition> pds) {
        for (PhysicalDescriptionDefinition pd : pds) {
            PhysicalDescriptionDefinition newPd = null;
            for (FormDefinition form : pd.getForm()) {
                if (newPd == null) {
                    newPd = new PhysicalDescriptionDefinition();
                    mods.getPhysicalDescription().add(newPd);
                }
                newPd.getForm().add(form);
            }
        }
    }

    private void inheritTitleInfo(ModsDefinition mods, List<TitleInfoDefinition> tis) {
        for (TitleInfoDefinition ti : tis) {
            if (ti.getType() == null) {
                ti.getPartNumber().clear();
                ti.getPartName().clear();
                ti.getNonSort().clear();
                mods.getTitleInfo().add(ti);
            }
        }
    }

    protected final void inheritSupplementTitleInfo(ModsDefinition mods, List<TitleInfoDefinition> tis) {
        for (TitleInfoDefinition ti : tis) {
            if (ti.getType() == null) {
                ti.getPartNumber().clear();
                ti.getPartName().clear();
                ti.getNonSort().clear();
                ti.getSubTitle().clear();
                mods.getTitleInfo().add(ti);
            }
        }
    }

    @Override
    public void setMetadataAsJson(DescriptionMetadata<String> jsonData, String message, String typeRecord) throws DigitalObjectException {
        String json = jsonData.getData();
        String modelId = handler.getModel().getPid();
        ModsDefinition mods;
        if (json == null) {
            mods = createDefault(modelId);
        } else {
            NdkMapper mapper = mapperFactory.get(modelId);
            mapper.setModelId(modelId);
            Context context = new Context(handler);
            ObjectMapper jsMapper = JsonUtils.defaultObjectMapper();
            try {
                mods = mapper.fromJsonObject(jsMapper, json, context);
            } catch (Exception ex) {
                throw new DigitalObjectException(fobject.getPid(), null, ModsStreamEditor.DATASTREAM_ID, null, ex);
            }
        }
        write(modelId, mods, jsonData, message, typeRecord);
    }

    @Override
    public void setMetadataAsXml(DescriptionMetadata<String> xmlData, String message, String typeRecord) throws DigitalObjectException {
        ModsDefinition mods;
        String modelId = handler.getModel().getPid();
        if (xmlData.getData() != null) {
            ValidationErrorHandler errHandler = new ValidationErrorHandler();
            try {
                String data = xmlData.getData();
                xmlData.setData(data);

                Validator validator = ModsUtils.getSchema().newValidator();
                validator.setErrorHandler(errHandler);
                validator.validate(new StreamSource(new StringReader(xmlData.getData())));
                checkValidation(errHandler, xmlData);
                mods = ModsUtils.unmarshalModsType(new StreamSource(new StringReader(xmlData.getData())));
            } catch (DataBindingException | SAXException | IOException ex) {
                checkValidation(errHandler, xmlData);
                throw new DigitalObjectValidationException(xmlData.getPid(),
                            xmlData.getBatchId(), ModsStreamEditor.DATASTREAM_ID, null, ex)
                        .addValidation("mods", ex.getMessage());
            }
        } else {
            mods = createDefault(modelId);
        }
        write(modelId, mods, xmlData, message, typeRecord);
    }

    private void checkValidation(ValidationErrorHandler errHandler, DescriptionMetadata<String> xmlData)
            throws DigitalObjectValidationException {
        if (!errHandler.getValidationErrors().isEmpty()) {
            String msg = errHandler.getValidationErrors().stream().collect(Collectors.joining("\n"));
            throw new DigitalObjectValidationException(xmlData.getPid(), xmlData.getBatchId(), ModsStreamEditor.DATASTREAM_ID, msg, null)
                    .addValidation("mods", msg);
        }
    }

    @Override
    public DescriptionMetadata<ModsDefinition> getMetadata() throws DigitalObjectException {
        ModsDefinition mods = editor.read();
        DescriptionMetadata<ModsDefinition> dm = new DescriptionMetadata<ModsDefinition>();
        dm.setPid(fobject.getPid());
        dm.setTimestamp(editor.getLastModified());
//            dm.setEditor(editorId);
        dm.setData(mods);
        return dm;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O> DescriptionMetadata<O> getMetadataAsJsonObject(String mappingId) throws DigitalObjectException {
        DescriptionMetadata<ModsDefinition> dm = getMetadata();
        DescriptionMetadata json = dm;
        String modelId = null;
        if (mappingId == null || ModsCutomEditorType.EDITOR_PAGE.equals(mappingId)) {
            modelId = handler.relations().getModel();
            MetaModel model = modelId == null ? null : MetaModelRepository.getInstance().find(modelId);
            if (model == null) {
                throw new DigitalObjectException(fobject.getPid(), null, "ds", "Missing mappingId!", null);
            }
            if (mappingId == null) {
                mappingId = model.getModsCustomEditor();
            }
        }
        NdkMapper mapper = mapperFactory.get(mappingId);
        mapper.setModelId(ModsCutomEditorType.EDITOR_PAGE.equals(mappingId) ? modelId : mappingId);
        Context context = new Context(handler);
        json.setData(mapper.toJsonObject(dm.getData(), context));
        json.setEditor(mappingId);
        return json;
    }

    @Override
    public DescriptionMetadata<String> getMetadataAsXml() throws DigitalObjectException {
        String xml = editor.readAsString();
        DescriptionMetadata<String> dm = new DescriptionMetadata<String>();
        dm.setPid(fobject.getPid());
        dm.setTimestamp(editor.getLastModified());
//            dm.setEditor(editorId);
        dm.setData(xml);
        return dm;
    }

    @Override
    public PageViewItem createPageViewItem(Locale locale) throws DigitalObjectException {
        String modelId = handler.relations().getModel();
        if (modelId.equals(NdkPlugin.MODEL_PAGE) || modelId.equals(NdkPlugin.MODEL_NDK_PAGE)) {
            ModsDefinition mods = editor.read();
            NdkPageMapper mapper = new NdkPageMapper();
            Page page = mapper.toJsonObject(mods, new Context(handler));
            PageViewItem item = new PageViewItem();
            item.setPageIndex(page.getIndex());
            item.setPageNumber(page.getNumber());
            item.setPageType(page.getType());
            item.setPageTypeLabel(NdkPageMapper.getPageTypeLabel(item.getPageType(), locale));
            return item;
        } else {
            throw new DigitalObjectException(fobject.getPid(), "Unexpected model for NDK page: " + modelId);
        }
    }

    @Override
    public void setPage(PageViewItem page, String message) throws DigitalObjectException {
        String modelId = handler.relations().getModel();
        if (modelId.equals(NdkPlugin.MODEL_PAGE) || modelId.equals(NdkPlugin.MODEL_NDK_PAGE)) {
            DescriptionMetadata<ModsDefinition> metadata = new DescriptionMetadata<ModsDefinition>();
            metadata.setTimestamp(editor.getLastModified());
            NdkPageMapper mapper = new NdkPageMapper();
            ModsDefinition mods = mapper.createPage(
                    page.getPageIndex(), page.getPageNumber(), page.getPageType(), new Context(handler));
            metadata.setIgnoreValidation(true);
            write(modelId, mods, metadata, message, "update");
        } else {
            throw new DigitalObjectException(fobject.getPid(), "Unexpected model for NDK page: " + modelId);
        }
    }

    private void checkBeforeWrite(ModsDefinition mods, ModsDefinition oldMods, boolean ignoreValidations, String modelId) throws DigitalObjectException {
        if (ignoreValidations) {
            checkIdentifiers(mods, oldMods, null);
            return ;
        }
        DigitalObjectValidationException ex = new DigitalObjectValidationException(fobject.getPid(), null,
                DESCRIPTION_DATASTREAM_ID, "MODS validation", null);
        checkIdentifiers(mods, oldMods, ex);
        RelationEditor relations = handler.relations();
        List<String> members = relations.getMembers();
        if (HAS_MEMBER_VALIDATION_MODELS.contains(relations.getModel()) && !members.isEmpty()) {
            ex.addValidation("mods", ERR_NDK_CHANGE_MODS_WITH_MEMBERS);
        }
        if (!ex.getValidations().isEmpty()) {
            throw ex;
        }

        RdaRules rdaRules = new RdaRules(modelId, mods, ex);
        rdaRules.check();
    }

    private void checkIdentifiers(ModsDefinition mods, ModsDefinition oldMods, DigitalObjectValidationException ex) throws DigitalObjectException {
        if (fobject != null && fobject.getPid() != null) {
            ModsStreamEditor.addPid(mods, fobject.getPid());
        }
        List<IdentifierDefinition> oldIds = oldMods != null ? oldMods.getIdentifier()
                : Collections.<IdentifierDefinition>emptyList();
        // check URN:NBN
        for (IdentifierDefinition oldId : oldIds) {
            if ("urnnbn".equals(oldId.getType()) && oldId.getValue() != null && !oldId.getValue().trim().isEmpty()) {
                boolean missingId = true;
                for (IdentifierDefinition id : mods.getIdentifier()) {
                    if (oldId.getType().equals(id.getType()) && oldId.getValue().equals(id.getValue())) {
                        missingId = false;
                        break;
                    }
                }
                if (missingId) {
                    if (ex != null) {
                        ex.addValidation("mods.identifier", ERR_NDK_REMOVE_URNNBN, oldId.getValue());
                    } else {
                        mods.getIdentifier().add(oldId);
                    }
                } else if (ex != null) {
                    ex.addValidation("mods.identifier", ERR_NDK_CHANGE_MODS_WITH_URNNBN, oldId.getValue());
                }
            }
        }
        checkDoiDuplicity(mods, ex);
    }

    /** issue 443. */
    private void checkDoiDuplicity(ModsDefinition mods, DigitalObjectValidationException ex) throws DigitalObjectException {
        if (ex == null) {
            return ;
        }
        SearchView search = RemoteStorage.getInstance().getSearch();
        for (IdentifierDefinition idDef : mods.getIdentifier()) {
            if ("doi".equals(idDef.getType()) && idDef.getValue() != null) {
                String doi = idDef.getValue();
                if (doi != null && !doi.isEmpty()) {
                    try {
                        List<Item> results = search.findQuery(new Query().setIdentifier(doi));
                        if (!results.isEmpty()) {
                            if (results.size() == 1 && results.get(0).getPid().equals(fobject.getPid())) {
                                // ignore the self-reference
                                continue;
                            }
                            ex.addValidation("mods.identifier", ERR_NDK_DOI_DUPLICITY, doi);
                        }
                    } catch (FedoraClientException ex1) {
                        throw new DigitalObjectException(fobject.getPid(), ex1);
                    } catch (IOException ex1) {
                        throw new DigitalObjectException(fobject.getPid(), ex1);
                    }
                }
            }
        }
    }

    protected void write(String modelId, ModsDefinition mods,
            DescriptionMetadata<?> options, String message, String typeRecord) throws DigitalObjectException {
        ModsDefinition oldMods = null;
        long timestamp = options.getTimestamp();
        if (timestamp < 0) {
            // rewrite with brand new MODS
            timestamp = editor.getLastModified();
        }
        if (timestamp > 0) {
            oldMods = editor.read();
        }
        if (!"new".equals(typeRecord)) {
            checkBeforeWrite(mods, oldMods, options.isIgnoreValidation(), modelId);
        }
        NdkMapper mapper = mapperFactory.get(modelId);
        mapper.setModelId(modelId);
        Context context = new Context(handler);
        mapper.createMods(mods, context);
        if (LOG.isLoggable(Level.FINE)) {
            String toXml = ModsUtils.toXml(mods, true);
            LOG.fine(toXml);
        }
        editor.write(mods, timestamp, message);

        // DC
        OaiDcType dc = mapper.toDc(mods, context);
        DcStreamEditor dcEditor = handler.objectMetadata();
        DublinCoreRecord dcr = dcEditor.read();
        dcr.setDc(dc);
        dcEditor.write(handler, dcr, message);

        // Label
        String label = mapper.toLabel(mods);
        fobject.setLabel(label);
    }

    protected final DigitalObjectHandler findEnclosingObject(
            DigitalObjectHandler obj, String searcModelId) throws DigitalObjectException {

        if (obj != null) {
            if (searcModelId.equals(obj.relations().getModel())) {
                return obj;
            } else {
                DigitalObjectElement parent = getCrawler().getParent(obj.getFedoraObject().getPid());
                return findEnclosingObject(parent, searcModelId);
            }
        }
        return null;
    }

    private DigitalObjectHandler findEnclosingObject(
            DigitalObjectElement obj, String searcModelId) throws DigitalObjectException {

        if (obj == DigitalObjectElement.NULL) {
            return null;
        } else if (searcModelId.equals(obj.getModelId())) {
            return obj.getHandler();
        } else {
            DigitalObjectElement parent = getCrawler().getParent(obj.getPid());
            return findEnclosingObject(parent, searcModelId);
        }
    }

    public DigitalObjectCrawler getCrawler() {
        if (crawler == null) {
            crawler = new DigitalObjectCrawler(DigitalObjectManager.getDefault(),
                    RemoteStorage.getInstance().getSearch());
        }
        return crawler;
    }

    /**
     * Wraps MODS for JSON serialization. Subclasses can add own properties.
     */
    public static class ModsWrapper {

        private ModsDefinition mods;

        public ModsWrapper() {
        }

        public ModsWrapper(ModsDefinition mods) {
            this.mods = mods;
        }

        public ModsDefinition getMods() {
            return mods;
        }

        public void setMods(ModsDefinition mods) {
            this.mods = mods;
        }
    }
}
