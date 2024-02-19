package cz.cas.lib.proarc.common.process.imports;

import cz.cas.lib.proarc.common.storage.FoxmlUtils;
import cz.cas.lib.proarc.common.process.BatchManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImportUtils {

    private static final Logger LOG = Logger.getLogger(ImportUtils.class.getName());

    public static boolean createPidHierarchy(List<BatchManager.BatchItemObject> batchItems, String documentPid, ArrayList<Hierarchy> songsPid, ArrayList<ArrayList<Hierarchy>> tracksPid, List<String> pids) {
        pids.clear();
        String pid = "";

        batchItems = sortBatchItems(batchItems);

        for (BatchManager.BatchItemObject batchItem : batchItems) {
            String name = nameWithoutExtention(batchItem.getFile().getName(), ".foxml");
            String[] splitName = name.split("-");

            try {
                int length = splitName.length;
                if (splitName[length-3].length() == 2 && splitName[length-2].length() == 2 && splitName[length-1].length() == 2) {
                    int disc = Integer.valueOf(splitName[splitName.length-3]);
                    int song = Integer.valueOf(splitName[splitName.length-2]);
                    int track = Integer.valueOf(splitName[splitName.length-1]);

                    if (disc < 1 || song < 1) {
                        LOG.log(Level.WARNING, "Spatna hodnota v nazvu souboru. Nepodarilo se automaticky vytvorit hierarchii objektu: " + splitName + ".");
                        return false;
                    }
                    if (track > 0 ) {
                        if (songsPid.size() < song) {
                            pid = FoxmlUtils.createPid();
                            Hierarchy songHierarchy = new Hierarchy(pid, null);
                            songsPid.add(song - 1, songHierarchy);
                            tracksPid.add(song - 1, new ArrayList<>());
                        }
                        if (tracksPid.get(song - 1).size() < track) {
                            pid = FoxmlUtils.createPid();
                            Hierarchy trackHierarchy = new Hierarchy(pid, batchItem.getPid());
                            tracksPid.get(song - 1).add(track - 1, trackHierarchy);
                        }
                    } else if (track == 0) {
                        if (songsPid.size() < song) {
                            pid = FoxmlUtils.createPid();
                            Hierarchy songHierarchy = new Hierarchy(pid, batchItem.getPid());
                            songsPid.add(song - 1, songHierarchy);
                            tracksPid.add(song - 1, new ArrayList<>());
                        }
                    } else {
                        LOG.log(Level.WARNING, "Spatna hodnota v nazvu souboru. Nepodarilo se automaticky vytvorit hierarchii objektu: " + splitName + ".");
                        return false;
                    }
                } else {
                    pids.add(batchItem.getPid());
                    continue;
                }
            } catch (NumberFormatException ex) {
                pids.add(batchItem.getPid());
                continue;
            }
        }
        return true;
    }

    private static List<BatchManager.BatchItemObject> sortBatchItems(List<BatchManager.BatchItemObject> batchItems) {

        Map<String, BatchManager.BatchItemObject> sortedList = new TreeMap<String, BatchManager.BatchItemObject>();


        for (BatchManager.BatchItemObject batchItem : batchItems) {
            String name = nameWithoutExtention(batchItem.getFile().getName(), ".foxml");
            String[] splitName = name.split("-");

            int length = splitName.length;
            try {
                if (splitName[length - 2].length() == 2 && splitName[length - 1].length() == 2) {
                    int song = Integer.valueOf(splitName[splitName.length - 2]);
                    int track = Integer.valueOf(splitName[splitName.length - 1]);

                    String id = String.format("%02d", song) + "_" + String.format("%02d", track);
                    sortedList.put(id, batchItem);
                } else {
                    sortedList.put(name, batchItem);
                }
            } catch (NumberFormatException ex) {
                sortedList.put(name, batchItem);
            }
        }
        Collection<BatchManager.BatchItemObject> values = sortedList.values();
        return new ArrayList<>(values);
    }

    private static String nameWithoutExtention(String name, String extension) {
        return name.substring(0, name.length() - extension.length());
    }

    public static class Hierarchy {
        String parent;
        String child;

        public Hierarchy(String parent, String child) {
            this.parent = parent;
            this.child = child;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public String getChild() {
            return child;
        }

        public void setChild(String child) {
            this.child = child;
        }
    }
}
