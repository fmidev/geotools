/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2005, Open Geospatial Consortium Inc.
 *
 *    All Rights Reserved. http://www.opengis.org/legal/
 */
package org.geotools.api.temporal;

import java.util.Collection;

/**
 * Provides only the attributes inherited from temporal reference system.
 *
 * @author Alexander Petkov
 * @todo The javadoc doesn't seem accurate.
 * @todo Missing UML annotations.
 */
public interface OrdinalReferenceSystem extends TemporalReferenceSystem {
    /**
     * Get the set of ordinal eras of which this ordinal reference system consists of.
     *
     * @return A hierarchically-structured collection of ordinal eras.
     * @todo What the structure is exactly?
     */
    Collection<OrdinalEra> getOrdinalEraSequence();
}
