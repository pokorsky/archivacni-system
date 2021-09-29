package cz.cas.lib.proarc.common.actions;

import cz.cas.lib.proarc.common.config.AppConfiguration;
import cz.cas.lib.proarc.common.dublincore.DcStreamEditor;
import cz.cas.lib.proarc.common.export.mets.MetsContext;
import cz.cas.lib.proarc.common.export.mets.MetsExportException;
import cz.cas.lib.proarc.common.export.mets.MetsUtils;
import cz.cas.lib.proarc.common.export.mets.structure.IMetsElement;
import cz.cas.lib.proarc.common.export.mets.structure.MetsElement;
import cz.cas.lib.proarc.common.fedora.DigitalObjectException;
import cz.cas.lib.proarc.common.fedora.FedoraObject;
import cz.cas.lib.proarc.common.fedora.FoxmlUtils;
import cz.cas.lib.proarc.common.fedora.RemoteStorage;
import cz.cas.lib.proarc.common.fedora.XmlStreamEditor;
import cz.cas.lib.proarc.common.fedora.relation.RelationEditor;
import cz.cas.lib.proarc.common.mods.ModsStreamEditor;
import cz.cas.lib.proarc.common.mods.custom.ModsConstants;
import cz.cas.lib.proarc.common.mods.ndk.NdkMapper;
import cz.cas.lib.proarc.common.mods.ndk.NdkNewPageMapper;
import cz.cas.lib.proarc.common.object.DigitalObjectHandler;
import cz.cas.lib.proarc.common.object.DigitalObjectManager;
import cz.cas.lib.proarc.common.object.MetadataHandler;
import cz.cas.lib.proarc.common.object.model.MetaModelRepository;
import cz.cas.lib.proarc.common.object.ndk.NdkPlugin;
import cz.cas.lib.proarc.common.object.oldprint.OldPrintPlugin;
import cz.cas.lib.proarc.mods.DateDefinition;
import cz.cas.lib.proarc.mods.DetailDefinition;
import cz.cas.lib.proarc.mods.GenreDefinition;
import cz.cas.lib.proarc.mods.ModsDefinition;
import cz.cas.lib.proarc.mods.OriginInfoDefinition;
import cz.cas.lib.proarc.mods.PartDefinition;
import cz.cas.lib.proarc.mods.RecordInfoDefinition;
import cz.cas.lib.proarc.mods.StringPlusLanguage;
import cz.cas.lib.proarc.mods.StringPlusLanguagePlusAuthority;
import cz.cas.lib.proarc.mods.TitleInfoDefinition;
import cz.cas.lib.proarc.oaidublincore.OaiDcType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.yourmediashelf.fedora.generated.foxml.DigitalObject;


public class ChangeModels {

    private static final Logger LOG = Logger.getLogger(ChangeModels.class.getName());

    private static AppConfiguration appConfig;
    private static String pid;
    private static String oldModel;
    private static String newModel;
    private List<String> pids;


    public ChangeModels(AppConfiguration appConfig, String pid, String modelId, String oldModel, String newModel) {
        this.appConfig = appConfig;
        this.pid = pid;
        this.oldModel = oldModel;
        this.newModel = newModel;
        this.pids = new ArrayList<>();
    }

    public List<String> findObjects() throws DigitalObjectException {
        IMetsElement element = getElement();
        if (element == null) {
            throw new DigitalObjectException(pid, "ChangeModels:findObjects - object is null");
        }
        findChildrens(element);
        return pids;
    }

    public String findRootObject() throws DigitalObjectException {
        IMetsElement element = getElement();
        if (element == null) {
            throw new DigitalObjectException(pid, "ChangeModels:findRootObject - object is null");
        }
        return getRootElement(element);
    }

    private String getRootElement(IMetsElement element) {
        return element.getMetsContext().getRootElement().getOriginalPid();
    }

    public void changeModelsAndRepairMetadata(String parentPid) throws DigitalObjectException {
        /*
        if (pids.isEmpty()) {
            throw new DigitalObjectException(pid, "No models with model " + oldModel);
        }
        */
        int updated = 0;
        try {
            for (String pid : pids) {
                DigitalObjectManager dom = DigitalObjectManager.getDefault();
                changeModel(dom, pid);
                repairMetadata(dom, pid, parentPid);
                updated++;
            }
            if (updated == 0) {
                LOG.log(Level.WARNING, "No objects with model " + oldModel + " found.");
            } else {
                LOG.log(Level.WARNING, "Object changed from " + oldModel + " to " + newModel + " succesfully. Total items (" + updated + ").");
            }
        } catch (DigitalObjectException ex) {
            LOG.log(Level.SEVERE, "Changing objects failed, totaly items (" + pids.size() + "), changed only " + updated + "items.") ;
            throw new DigitalObjectException(pid, "Changing objects failed, totaly items (" + pids.size() + "), changed only " + updated + "items.");
        }
    }

    private void repairMetadata(DigitalObjectManager dom, String pid, String parentPid) throws DigitalObjectException {
        FedoraObject fo = dom.find(pid, null);
        DigitalObjectHandler handler = new DigitalObjectHandler(fo, MetaModelRepository.getInstance());
        NdkMapper.Context context = new NdkMapper.Context(handler);

        NdkMapper mapper = NdkMapper.get(newModel);
        mapper.setModelId(newModel);

        XmlStreamEditor xml = fo.getEditor(FoxmlUtils.inlineProfile(
                MetadataHandler.DESCRIPTION_DATASTREAM_ID, ModsConstants.NS,
                MetadataHandler.DESCRIPTION_DATASTREAM_LABEL));
        ModsStreamEditor modsStreamEditor = new ModsStreamEditor(xml, fo);
        ModsDefinition mods = modsStreamEditor.read();
        fixMods(pid, mods, parentPid);
        mapper.createMods(mods, context);
        modsStreamEditor.write(mods, modsStreamEditor.getLastModified(), null);


        OaiDcType dc = mapper.toDc(mods, context);
        DcStreamEditor dcEditor = handler.objectMetadata();
        DcStreamEditor.DublinCoreRecord dcr = dcEditor.read();
        dcr.setDc(dc);
        dcEditor.write(handler, dcr, null);

        fo.setLabel(mapper.toLabel(mods));

        handler.commit();
    }

    private void fixMods(String pid, ModsDefinition mods, String parentPid) throws DigitalObjectException {
        switch (newModel) {
            case NdkPlugin.MODEL_PAGE:
                fixPageMods(mods);
                break;
            case NdkPlugin.MODEL_NDK_PAGE:
                fixNdkPageMods(mods);
                break;
            case OldPrintPlugin.MODEL_PAGE:
                fixOldPrintMods(mods);
            case NdkPlugin.MODEL_MONOGRAPHVOLUME:
                fixNdkMonographVolumeMods(mods, parentPid);
                break;
            case NdkPlugin.MODEL_MONOGRAPHTITLE:
                fixNdkMonographVolumeMods(mods, parentPid);
                break;
            default:
                throw new DigitalObjectException(pid, "ChangeModels:fixMods - Unsupported model.");
        }
    }

    private void fixNdkMonographVolumeMods(ModsDefinition mods, String parentPid) throws DigitalObjectException {
        String title = null;
        if (parentPid != null) {
            title = getTitle(getParentMods(parentPid));
        }
        if (title != null) {
            fixTitleInfo(title, mods);
            fixOriginInfo(mods);
        }
        fixRecordInfo(mods);
    }

    private void fixOriginInfo(ModsDefinition mods) {
        for (OriginInfoDefinition originInfo : mods.getOriginInfo()) {
            if (originInfo.getDateIssued().isEmpty()) {
                originInfo.getDateIssued().add(new DateDefinition());
            }
            for (DateDefinition date : originInfo.getDateIssued()) {
                date.setValue("20/21. století");
            }
        }
    }

    private void fixTitleInfo(String titleValue, ModsDefinition mods) {
        for (TitleInfoDefinition titleInfo : mods.getTitleInfo()) {
            titleInfo.getPartName().addAll(titleInfo.getTitle());
            titleInfo.getTitle().clear();
            StringPlusLanguage title = new StringPlusLanguage();
            title.setValue(titleValue);
            titleInfo.getTitle().add(title);
        }
    }

    private String getTitle(ModsDefinition mods) {
        if (mods != null) {
            for (TitleInfoDefinition title : mods.getTitleInfo()) {
                for (StringPlusLanguage titleInfo : title.getTitle()) {
                    return titleInfo.getValue();
                }
            }
        }
        return null;
    }

    private void fixNdkPageMods(ModsDefinition mods) {
        fixPartNdkPage(mods);
        fixGenre(mods, getPageType(mods));
    }

    private void fixPageMods(ModsDefinition mods) {
        fixPartPage(mods);
        fixGenre(mods, null);
    }


    private void fixOldPrintMods(ModsDefinition mods) {
        fixPartPage(mods);
        fixGenre(mods, null);
    }

    private void fixRecordInfo(ModsDefinition mods) {
        for (RecordInfoDefinition recordInfo : mods.getRecordInfo()) {
            if (recordInfo.getDescriptionStandard().size() == 0) {
                StringPlusLanguagePlusAuthority descriptionStandard = new StringPlusLanguagePlusAuthority();
                recordInfo.getDescriptionStandard().add(descriptionStandard);
            }
            for (StringPlusLanguagePlusAuthority descriptionStandard : recordInfo.getDescriptionStandard()) {
                descriptionStandard.setValue(ModsConstants.VALUE_DESCRIPTIONSTANDARD_AACR);
            }
        }
    }

    private void fixPartPage(ModsDefinition mods) {
        List<PartDefinition> partDefinitions = new ArrayList<>();
        String pageType = null;
        String pageNumber = null;
        String pageIndex = null;
        for (PartDefinition part : mods.getPart()) {
            if (pageType == null) {
                pageType = part.getType();
            }
            for (DetailDefinition detail : part.getDetail()) {
                if ("pageNumber".equals(detail.getType()) || "page number".equals(detail.getType())) {
                    pageNumber = getValue(detail);
                } else if ("pageIndex".equals(detail.getType())) {
                    pageIndex = getValue(detail);
                }
            }
        }
        if (NdkNewPageMapper.PAGE_TYPE_NORMAL.equals(pageType)) {
            pageType = null;
        }
        partDefinitions.add(createPart(pageType, pageNumber, pageIndex));

        mods.getPart().clear();
        mods.getPart().addAll(partDefinitions);

    }

    private void fixPartNdkPage(ModsDefinition mods) {
        List<PartDefinition> partDefinitions = new ArrayList<>();
        String pageType = null;
        String pageNumber = null;
        String pageIndex = null;
        for (PartDefinition part : mods.getPart()) {
            if (pageType == null) {
                pageType = part.getType();
            }
            for (DetailDefinition detail : part.getDetail()) {
                if ("pageNumber".equals(detail.getType()) || "page number".equals(detail.getType())) {
                    pageNumber = getValue(detail);
                } else if ("pageIndex".equals(detail.getType())) {
                    pageIndex = getValue(detail);
                }
            }
        }
        if (pageType == null) {
            pageType = NdkNewPageMapper.PAGE_TYPE_NORMAL;
        }
        partDefinitions.add(createPageNumberDetail(pageType, pageNumber));
        partDefinitions.add(createPageIndexDetail(pageIndex));

        mods.getPart().clear();
        mods.getPart().addAll(partDefinitions);
    }

    private DetailDefinition setValue(String type, String value) {
        StringPlusLanguage number = new StringPlusLanguage();
        number.setValue(value);

        DetailDefinition detail = new DetailDefinition();
        detail.setType(type);
        detail.getNumber().add(number);

        return detail;
    }

    private PartDefinition createPart(String pageType, String pageNumber, String pageIndex) {
        PartDefinition part = new PartDefinition();
        part.getDetail().add(setValue("pageIndex", pageIndex));
        part.getDetail().add(setValue("pageNumber", pageNumber));
        part.setType(pageType);

        return part;
    }

    private PartDefinition createPageIndexDetail(String pageIndex) {
        PartDefinition part = new PartDefinition();
        part.getDetail().add(setValue("pageIndex", pageIndex));
        return part;
    }

    private PartDefinition createPageNumberDetail(String pageType, String pageNumber) {
        PartDefinition part = new PartDefinition();
        part.setType(pageType);
        part.getDetail().add(setValue("pageNumber", pageNumber));
        return part;
    }

    private String getValue(DetailDefinition detail) {
        String value = null;
        for (StringPlusLanguage number : detail.getNumber()) {
            if (value == null) {
                value = number.getValue();
            }
        }
        return value;
    }

    private String getPageType(ModsDefinition mods) {
        String pageType = null;
        for (PartDefinition part : mods.getPart()) {
            pageType = part.getType();
            break;
        }
        return pageType;
    }

    private void fixGenre(ModsDefinition mods, String pageType) {
        if (mods.getGenre().isEmpty()) {
            GenreDefinition genre = new GenreDefinition();
            genre.setValue("page");
            mods.getGenre().add(genre);
        }
        if (mods.getGenre().size() > 0) {
            mods.getGenre().get(0).setType(pageType);
        }
    }

    private ModsDefinition getParentMods(String parentPid) throws DigitalObjectException {
        DigitalObjectManager dom = DigitalObjectManager.getDefault();
        FedoraObject fo = dom.find(parentPid, null);
        XmlStreamEditor xml = fo.getEditor(FoxmlUtils.inlineProfile(
                MetadataHandler.DESCRIPTION_DATASTREAM_ID, ModsConstants.NS,
                MetadataHandler.DESCRIPTION_DATASTREAM_LABEL));
        ModsStreamEditor modsStreamEditor = new ModsStreamEditor(xml, fo);
        return modsStreamEditor.read();
    }


    private void changeModel(DigitalObjectManager dom, String pid) throws DigitalObjectException {
        FedoraObject fedoraObject = dom.find(pid, null);
        DigitalObjectHandler handler = dom.createHandler(fedoraObject);
        RelationEditor editor = handler.relations();
        editor.setModel(newModel);
        editor.write(editor.getLastModified(), "Change model");
        handler.commit();
    }

    private void findChildrens(IMetsElement element) throws DigitalObjectException {
        if (element == null) {
            throw new DigitalObjectException(pid, "ChangeModels:findChildrens - element is null");
        }
        String modelId = element.getModel().substring(12);
        if (oldModel.equals(modelId)) {
            pids.add(element.getOriginalPid());
        }

        for (IMetsElement childElement : element.getChildren()) {
            findChildrens(childElement);
        }
    }

    public IMetsElement getElement() throws DigitalObjectException {
        try {
            RemoteStorage rstorage = RemoteStorage.getInstance(appConfig);
            RemoteStorage.RemoteObject robject = rstorage.find(pid);
            MetsContext metsContext = buildContext(robject, null, rstorage);
            DigitalObject dobj = MetsUtils.readFoXML(robject.getPid(), robject.getClient());
            if (dobj == null) {
                return null;
            }
            return MetsElement.getElement(dobj, null, metsContext, true);
        } catch (IOException | MetsExportException ex) {
            throw new DigitalObjectException(pid, "ChangeModels:getElement - impossible to find element");
        }
    }

    private MetsContext buildContext(RemoteStorage.RemoteObject fo, String packageId, RemoteStorage rstorage) {
        MetsContext mc = new MetsContext();
        mc.setFedoraClient(fo.getClient());
        mc.setRemoteStorage(rstorage);
        mc.setPackageID(packageId);
        mc.setOutputPath(null);
        mc.setAllowNonCompleteStreams(false);
        mc.setAllowMissingURNNBN(false);
        mc.setConfig(null);
        return mc;
    }
}
