/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.jdbc;

import java.util.Collections;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;

@SuppressWarnings("PMD.JUnit4TestShouldUseTestAnnotation") // not yet a JUnit4 test
public abstract class JDBCPrimaryKeyOnlineTest extends JDBCTestSupport {

    @Override
    protected abstract JDBCPrimaryKeyTestSetup createTestSetup();

    @Override
    protected void connect() throws Exception {
        super.connect();

        if (setup.canResetSchema()) {
            dataStore.setDatabaseSchema(null);
        }
    }

    public void testAutoGeneratedPrimaryKey() throws Exception {
        JDBCFeatureStore fs = (JDBCFeatureStore) dataStore.getFeatureSource(tname("auto"));
        assertEquals(1, fs.getPrimaryKey().getColumns().size());
        assertTrue(fs.getPrimaryKey().getColumns().get(0) instanceof AutoGeneratedPrimaryKeyColumn);
        assertNull(fs.getSchema().getDescriptor(fs.getPrimaryKey().getColumns().get(0).getName()));

        SimpleFeatureCollection features = fs.getFeatures();
        assertPrimaryKeyValues(features, 3);
        addFeature(fs.getSchema(), fs);
        assertPrimaryKeyValues(features, 4);
    }

    public void testSequencedPrimaryKey() throws Exception {
        JDBCFeatureStore fs = (JDBCFeatureStore) dataStore.getFeatureSource(tname("seq"));

        assertEquals(1, fs.getPrimaryKey().getColumns().size());
        assertTrue(fs.getPrimaryKey().getColumns().get(0) instanceof SequencedPrimaryKeyColumn);

        SimpleFeatureCollection features = fs.getFeatures();
        assertPrimaryKeyValues(features, 3);
        addFeature(fs.getSchema(), fs);
        assertPrimaryKeyValues(features, 4);
    }

    public void testNonIncrementingPrimaryKey() throws Exception {
        JDBCFeatureStore fs = (JDBCFeatureStore) dataStore.getFeatureSource(tname("noninc"));

        assertEquals(1, fs.getPrimaryKey().getColumns().size());
        assertTrue(
                fs.getPrimaryKey().getColumns().get(0) instanceof NonIncrementingPrimaryKeyColumn);

        SimpleFeatureCollection features = fs.getFeatures();
        assertPrimaryKeyValues(features, 3);
        addFeature(fs.getSchema(), fs);
        assertPrimaryKeyValues(features, 4);
    }

    protected void addFeature(SimpleFeatureType featureType, JDBCFeatureStore features)
            throws Exception {
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(featureType);
        b.add("four");
        b.add(new GeometryFactory().createPoint(new Coordinate(4, 4)));

        SimpleFeature f = b.buildFeature(null);
        features.addFeatures(DataUtilities.collection(f));

        // pattern match to handle the multi primary key case
        assertTrue(
                ((String) f.getUserData().get("fid"))
                        .matches(tname(featureType.getTypeName()) + ".4(\\..*)?"));
    }

    protected void assertPrimaryKeyValues(final SimpleFeatureCollection features, int count)
            throws Exception {
        assertFeatureIterator(
                1,
                count,
                features.features(),
                new SimpleFeatureAssertion() {
                    @Override
                    public int toIndex(SimpleFeature feature) {
                        return Integer.parseInt(feature.getID().split("\\.", 2)[1]);
                    }

                    @Override
                    public void check(int index, SimpleFeature feature) {
                        assertEquals(
                                tname(features.getSchema().getName().getLocalPart()) + "." + index,
                                feature.getID());
                    }
                });
    }

    public void testMultiColumnPrimaryKey() throws Exception {
        JDBCFeatureStore fs = (JDBCFeatureStore) dataStore.getFeatureSource(tname("multi"));

        assertEquals(2, fs.getPrimaryKey().getColumns().size());

        SimpleFeatureCollection features = fs.getFeatures();

        assertMultiPrimaryKeyValues(features, 3);

        addFeature(fs.getSchema(), fs);

        assertMultiPrimaryKeyValues(features, 4);

        // test with a filter
        FilterFactory ff = dataStore.getFilterFactory();

        Id id = ff.id(Collections.singleton(ff.featureId(tname("multi") + ".1.x")));
        features = fs.getFeatures(id);
        assertEquals(1, features.size());
    }

    void assertMultiPrimaryKeyValues(final SimpleFeatureCollection features, int count)
            throws Exception {
        assertFeatureIterator(
                1,
                count,
                features.features(),
                new SimpleFeatureAssertion() {
                    String[] xyz = new String[] {"x", "y", "z"};

                    @Override
                    public int toIndex(SimpleFeature feature) {
                        return Integer.parseInt(feature.getID().split("\\.")[1]);
                    }

                    @Override
                    public void check(int index, SimpleFeature feature) {

                        if (index < 4) {
                            assertEquals(
                                    tname("multi") + "." + index + "." + xyz[index - 1],
                                    feature.getID());
                        } else {
                            assertTrue(feature.getID().startsWith(tname("multi") + ".4."));
                        }
                    }
                });
    }

    public void testNullPrimaryKey() throws Exception {
        JDBCFeatureSource fs = (JDBCFeatureSource) dataStore.getFeatureSource(tname("nokey"));
        assertFalse(fs instanceof FeatureStore);
    }

    public void testUniqueIndex() throws Exception {
        JDBCFeatureStore fs = (JDBCFeatureStore) dataStore.getFeatureSource(tname("uniq"));
        assertEquals(1, fs.getPrimaryKey().getColumns().size());
        assertTrue(
                fs.getPrimaryKey().getColumns().get(0) instanceof NonIncrementingPrimaryKeyColumn);
        assertNull(fs.getSchema().getDescriptor(fs.getPrimaryKey().getColumns().get(0).getName()));

        SimpleFeatureCollection features = fs.getFeatures();
        assertPrimaryKeyValues(features, 3);
        addFeature(fs.getSchema(), fs);
        assertPrimaryKeyValues(features, 4);
    }

    public void testExposePrimaryKeyColumns() throws Exception {
        JDBCFeatureStore fs = (JDBCFeatureStore) dataStore.getFeatureSource(tname("noninc"));
        assertEquals(2, fs.getSchema().getAttributeCount());

        fs = (JDBCFeatureStore) dataStore.getFeatureSource(tname("noninc"));
        fs.setExposePrimaryKeyColumns(true);
        assertEquals(3, fs.getSchema().getAttributeCount());
    }

    public void testUpdateWithExposePrimaryKeyColumns() throws Exception {
        JDBCFeatureStore fs = (JDBCFeatureStore) dataStore.getFeatureSource(tname("nonfirst"));
        fs.setExposePrimaryKeyColumns(true);

        String key = null;
        for (AttributeDescriptor ad : fs.getSchema().getAttributeDescriptors()) {
            if (Number.class.isAssignableFrom(ad.getType().getBinding())) {
                key = ad.getLocalName();
                break;
            }
        }

        assertNotNull(key);

        Object keyValue = null;
        try (FeatureReader r = fs.getReader()) {
            assertTrue(r.hasNext());

            SimpleFeature f = (SimpleFeature) r.next();
            keyValue = f.getAttribute(key);
        }

        assertNotNull(keyValue);

        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Filter filter = ff.equal(ff.property(key), ff.literal(keyValue), false);

        assertEquals(1, fs.getCount(new Query(tname("nonfirst"), filter)));

        try {
            fs.modifyFeatures(key, 10, filter);
            fail("expected exception");
        } catch (IllegalArgumentException e) {
        }

        fs.modifyFeatures(
                new String[] {aname("name"), key, aname("geom")},
                new Object[] {"foo", 10, null},
                filter);

        try (FeatureReader r =
                fs.getReader(ff.equal(ff.property(key), ff.literal(keyValue), true))) {
            assertTrue(r.hasNext());

            SimpleFeature f = (SimpleFeature) r.next();
            assertEquals("foo", f.getAttribute(aname("name")));
        }
    }
}
