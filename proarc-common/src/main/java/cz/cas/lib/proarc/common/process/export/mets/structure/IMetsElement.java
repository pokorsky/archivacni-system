/*
 * Copyright (C) 2014 Robert Simonovsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cz.cas.lib.proarc.common.process.export.mets.structure;

import com.yourmediashelf.fedora.generated.foxml.DigitalObject;
import cz.cas.lib.proarc.common.process.export.mets.MetsContext;
import cz.cas.lib.proarc.common.process.export.mets.MetsExportException;
import cz.cas.lib.proarc.mets.FileType;
import cz.cas.lib.proarc.mets.MdSecType;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.XMLGregorianCalendar;
import org.w3c.dom.Element;

/**
 * Interface of Mets Element
 *
 * @author Robert Simonovsky
 *
 */
public interface IMetsElement {

    /**
     * Sets the modsElementID
     *
     * @param modsElementID
     */
    void setModsElementID(String modsElementID);

    /**
     * Returns the altoFile
     *
     * @return
     */
    FileType getAltoFile();

    /**
     * Returns the setsAltoFile
     *
     */
    void setAltoFile(FileType altoFile);

    /**
     *
     * Collects all identifiers for mods element
     *
     * @return
     */
     Map<String, String> getModsIdentifiers() throws MetsExportException;

    /**
     * Sets the mdSecType of element in the target mets
     *
     * @param modsMetsElement
     */
    void setModsMetsElement(MdSecType modsMetsElement);

    /**
     * Returns the mdSecType of element
     *
     */
    MdSecType getModsMetsElement();

    /**
     * Returns the label of original object
     *
     * @return
     */
    String getLabel();

    /**
     * Returns the createDate attribute of original object
     *
     * @return
     */
    XMLGregorianCalendar getCreateDate();

    /**
     * Returns the lastUpdateDate attribute of original object
     *
     * @return
     */
    XMLGregorianCalendar getLastUpdateDate();

    /**
     * Returns the mods stream of mets element
     *
     * @return
     */
    List<Element> getModsStream();

    /**
     * Returns a parent MetsElement
     *
     * @return
     */
    IMetsElement getParent();

    /**
     * Returns the list of child elements
     *
     * @return
     */
    List<IMetsElement> getChildren();

    /**
     * Creates and assigns the child elements
     *
     * @throws MetsExportException
     */
    void fillChildren() throws MetsExportException;

    /**
     * Returns a descriptor data stream (DC/NSSESS/...)
     *
     * @return
     */
    List<Element> getDescriptor();

    /**
     * Returns a model of the element
     *
     * @return
     */
    String getModel();

    /**
     * Returns an original PID of the element
     *
     * @return
     */
    String getOriginalPid();

    /**
     * Returns a rel-ext datastream
     *
     * @return
     */
    List<Element> getRelsExt();

    /**
     * Returns the DigitalObject representing current Element
     *
     * @return
     */
    DigitalObject getSourceObject();

    /**
     * Accept method for generator of mets
     *
     * @param metsVisitor
     * @throws MetsExportException
     */
    void accept(IMetsElementVisitor metsVisitor) throws MetsExportException;

    /**
     * Returns the type an element
     *
     * @return
     */
    String getElementType();

    /**
     * Retuns the context of mets export
     *
     * @return
     */
    MetsContext getMetsContext();

    /**
     * Returns the ID of the element
     *
     * @return
     */
    String getElementID();

    /**
     * Returns the ID for mods of the element
     *
     * The ID is generated by increment operator {@link MetsContext#addElementId(String)}. (Not populated from MODS)
     *
     * @return
     */
    String getModsElementID();

    /**
     * Returns the start element from mods (Part->Extent->Start)
     *
     * @return
     */
    BigInteger getModsStart();

    /**
     * Returns the end element from mods (Part->Extent->End)
     *
     * @return
     */
    BigInteger getModsEnd();

    boolean getIgnoreMissingUrnNbn();
}