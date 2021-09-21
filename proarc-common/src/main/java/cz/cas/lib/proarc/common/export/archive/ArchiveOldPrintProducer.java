package cz.cas.lib.proarc.common.export.archive;

import cz.cas.lib.proarc.common.config.AppConfiguration;
import cz.cas.lib.proarc.common.export.ExportResultLog;
import cz.cas.lib.proarc.common.fedora.DigitalObjectException;
import cz.cas.lib.proarc.common.object.DigitalObjectElement;
import java.util.List;
import java.util.logging.Logger;

public class ArchiveOldPrintProducer extends ArchiveProducer {

    private static final Logger LOG = Logger.getLogger(ArchiveOldPrintProducer.class.getName());

    public ArchiveOldPrintProducer(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    @Override
    protected List<List<DigitalObjectElement>> selectObjects(List<String> pids) {
        ArchiveObjectSelector selector = new ArchiveOldPrintObjectSelector(crawler);
        try {
            selector.select(pids);
            return selector.getSelectedObjects();
        } catch (DigitalObjectException ex) {
            ExportResultLog.ExportResult archiveResult = new ExportResultLog.ExportResult();
            archiveResult.setInputPid(pids.get(0));
            reslog.getExports().add(archiveResult);
            archiveResult.setStatus(ExportResultLog.ResultStatus.FAILED);
            archiveResult.getError().add(new ExportResultLog.ResultError(ex.getPid(), null, ex));
            archiveResult.setEnd();
            throw new IllegalStateException("Archivation failed!", ex);
        }

    }
}