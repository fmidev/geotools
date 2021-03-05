/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.geometry.jts;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * A thin wrapper that adapts a JTS geometry to the Shape interface so that the geometry can be used
 * by java2d without coordinate cloning.
 *
 * <p>This implementation supports the use of MathTransform and can be constructed with a Decimation
 * stratagy object (allowing you to fine tune the process by which a Geometry is simplified into a
 * Shape).
 *
 * <p>This implementation is very careful about cloning; and has the ability to go faster when you
 * are using a LiteCoordinateSequenceFactory behind your geometry classes.
 *
 * @author Jesse Eichar
 * @version $Id$
 */
public final class LiteShape2 implements Shape, Cloneable {

    /** The wrapped JTS geometry */
    private Geometry geometry;

    private boolean generalize = false;

    private double maxDistance = 1;

    private static GeometryFactory geomFac;

    /** transform from dataspace to screenspace */
    private MathTransform mathTransform;

    /**
     * Creates a new LiteShape object.
     *
     * @param geom - the wrapped geometry
     * @param mathTransform - the transformation applied to the geometry in order to get to the
     *     shape points
     * @param decimator -
     * @param generalize - set to true if the geometry need to be generalized during rendering
     * @param maxDistance - distance used in the generalization process
     */
    public LiteShape2(
            Geometry geom,
            MathTransform mathTransform,
            Decimator decimator,
            boolean generalize,
            double maxDistance)
            throws TransformException, FactoryException {
        this(geom, mathTransform, decimator, generalize);
        this.maxDistance = maxDistance;
    }

    /**
     * Creates a new LiteShape object.
     *
     * @param geom - the wrapped geometry
     * @param mathTransform - the transformation applied to the geometry in order to get to the
     *     shape points
     * @param decimator -
     * @param generalize - set to true if the geometry need to be generalized during rendering
     */
    public LiteShape2(
            Geometry geom, MathTransform mathTransform, Decimator decimator, boolean generalize)
            throws TransformException, FactoryException {
        this(geom, mathTransform, decimator, generalize, true);
    }

    /**
     * Creates a new LiteShape object.
     *
     * @param geom - the wrapped geometry
     * @param mathTransform - the transformation applied to the geometry in order to get to the
     *     shape points
     * @param decimator -
     * @param generalize - set to true if the geometry need to be generalized during rendering
     * @param clone - if clone is false the original geometry may be modified directly, if true it
     *     will be cloned to make sure the original remains untouched
     */
    public LiteShape2(
            Geometry geom,
            MathTransform mathTransform,
            Decimator decimator,
            boolean generalize,
            boolean clone)
            throws TransformException, FactoryException {
        if (geom != null) {
            if (!clone
                    && geom.getFactory().getCoordinateSequenceFactory()
                            instanceof LiteCoordinateSequenceFactory) this.geometry = geom;
            else this.geometry = LiteCoordinateSequence.cloneGeometry(geom);
        }

        this.mathTransform = mathTransform;
        if (decimator != null) {
            this.geometry =
                    decimator.decimateTransformGeneralize(this.geometry, this.mathTransform);
            this.geometry.geometryChanged();
        } else {
            // if we have a transform a decimation span can be detected, so try to decimate anyways
            if (mathTransform != null
                    && !mathTransform.isIdentity()
                    && generalize
                    && geometry != null) {
                new Decimator(
                                mathTransform.inverse(),
                                getRectangle(this.geometry.getEnvelopeInternal()))
                        .decimate(this.geometry);
                this.geometry.geometryChanged();
            }
            if (geometry != null) {
                transformGeometry(geometry);
                this.geometry.geometryChanged();
            }
        }
        this.generalize = false;
    }

    private Rectangle getRectangle(Envelope envelope) {
        int minX = (int) Math.floor(envelope.getMinX());
        int minY = (int) Math.floor(envelope.getMinY());
        int maxX = (int) Math.floor(envelope.getMaxX());
        int maxY = (int) Math.floor(envelope.getMaxY());
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private void transformGeometry(Geometry geometry) throws TransformException, FactoryException {

        if (mathTransform == null || mathTransform.isIdentity()) return;

        if (geometry instanceof GeometryCollection) {
            GeometryCollection collection = (GeometryCollection) geometry;
            for (int i = 0; i < collection.getNumGeometries(); i++) {
                transformGeometry(collection.getGeometryN(i));
            }
        } else if (geometry instanceof Point) {
            LiteCoordinateSequence seq =
                    (LiteCoordinateSequence) ((Point) geometry).getCoordinateSequence();
            double[] coords = seq.getArray();
            double[] newCoords = new double[coords.length];
            mathTransform.transform(coords, 0, newCoords, 0, seq.size());
            seq.setArray(newCoords);
        } else if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            transformGeometry(polygon.getExteriorRing());
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                transformGeometry(polygon.getInteriorRingN(i));
            }
        } else if (geometry instanceof LineString) {
            LiteCoordinateSequence seq =
                    (LiteCoordinateSequence) ((LineString) geometry).getCoordinateSequence();
            double[] coords = seq.getArray();
            mathTransform.transform(coords, 0, coords, 0, seq.size());
            seq.setArray(coords);
        }
    }

    private GeometryFactory getGeometryFactory() {
        if (geomFac == null) {
            geomFac = new GeometryFactory(new LiteCoordinateSequenceFactory());
        }

        return geomFac;
    }

    /**
     * Sets the geometry contained in this lite shape. Convenient to reuse this object instead of
     * creating it again and again during rendering
     */
    public void setGeometry(Geometry g) throws TransformException, FactoryException {
        if (g != null) {
            this.geometry = getGeometryFactory().createGeometry(g);
            transformGeometry(geometry);
        }
    }

    /**
     * Tests if the interior of the <code>Shape</code> entirely contains the specified <code>
     * Rectangle2D</code>. This method might conservatively return <code>false</code> when:
     *
     * <ul>
     *   <li>the <code>intersect</code> method returns <code>true</code> and
     *   <li>the calculations to determine whether or not the <code>Shape</code> entirely contains
     *       the <code>Rectangle2D</code> are prohibitively expensive.
     * </ul>
     *
     * This means that this method might return <code>false</code> even though the <code>Shape
     * </code> contains the <code>Rectangle2D</code>. The <code>Area</code> class can be used to
     * perform more accurate computations of geometric intersection for any <code>Shape</code>
     * object if a more precise answer is required.
     *
     * @param r The specified <code>Rectangle2D</code>
     * @return <code>true</code> if the interior of the <code>Shape</code> entirely contains the
     *     <code>Rectangle2D</code>; <code>false</code> otherwise or, if the <code>Shape</code>
     *     contains the <code>Rectangle2D</code> and the <code>intersects</code> method returns
     *     <code>true</code> and the containment calculations would be too expensive to perform.
     * @see #contains(double, double, double, double)
     */
    @Override
    public boolean contains(Rectangle2D r) {
        Geometry rect = rectangleToGeometry(r);

        return geometry.contains(rect);
    }

    /**
     * Tests if a specified {@link Point2D}is inside the boundary of the <code>Shape</code>.
     *
     * @param p a specified <code>Point2D</code>
     * @return <code>true</code> if the specified <code>Point2D</code> is inside the boundary of the
     *     <code>Shape</code>; <code>false</code> otherwise.
     */
    @Override
    public boolean contains(Point2D p) {
        Coordinate coord = new Coordinate(p.getX(), p.getY());
        Geometry point = geometry.getFactory().createPoint(coord);

        return geometry.contains(point);
    }

    /**
     * Tests if the specified coordinates are inside the boundary of the <code>Shape</code>.
     *
     * @param x the specified coordinates, x value
     * @param y the specified coordinates, y value
     * @return <code>true</code> if the specified coordinates are inside the <code>Shape</code>
     *     boundary; <code>false</code> otherwise.
     */
    @Override
    public boolean contains(double x, double y) {
        Coordinate coord = new Coordinate(x, y);
        Geometry point = geometry.getFactory().createPoint(coord);

        return geometry.contains(point);
    }

    /**
     * Tests if the interior of the <code>Shape</code> entirely contains the specified rectangular
     * area. All coordinates that lie inside the rectangular area must lie within the <code>Shape
     * </code> for the entire rectanglar area to be considered contained within the <code>Shape
     * </code>.
     *
     * <p>This method might conservatively return <code>false</code> when:
     *
     * <ul>
     *   <li>the <code>intersect</code> method returns <code>true</code> and
     *   <li>the calculations to determine whether or not the <code>Shape</code> entirely contains
     *       the rectangular area are prohibitively expensive.
     * </ul>
     *
     * This means that this method might return <code>false</code> even though the <code>Shape
     * </code> contains the rectangular area. The <code>Area</code> class can be used to perform
     * more accurate computations of geometric intersection for any <code>Shape</code> object if a
     * more precise answer is required.
     *
     * @param x the coordinates of the specified rectangular area, x value
     * @param y the coordinates of the specified rectangular area, y value
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return <code>true</code> if the interior of the <code>Shape</code> entirely contains the
     *     specified rectangular area; <code>false</code> otherwise or, if the <code>Shape</code>
     *     contains the rectangular area and the <code>intersects</code> method returns <code>true
     *     </code> and the containment calculations would be too expensive to perform.
     * @see java.awt.geom.Area
     * @see #intersects
     */
    @Override
    public boolean contains(double x, double y, double w, double h) {
        Geometry rect = createRectangle(x, y, w, h);

        return geometry.contains(rect);
    }

    /**
     * Returns an integer {@link Rectangle}that completely encloses the <code>Shape</code>. Note
     * that there is no guarantee that the returned <code>Rectangle</code> is the smallest bounding
     * box that encloses the <code>Shape</code>, only that the <code>Shape</code> lies entirely
     * within the indicated <code>Rectangle</code>. The returned <code>Rectangle</code> might also
     * fail to completely enclose the <code>Shape</code> if the <code>Shape</code> overflows the
     * limited range of the integer data type. The <code>getBounds2D</code> method generally returns
     * a tighter bounding box due to its greater flexibility in representation.
     *
     * @return an integer <code>Rectangle</code> that completely encloses the <code>Shape</code>.
     * @see #getBounds2D
     */
    @Override
    public Rectangle getBounds() {
        Rectangle2D env = getBounds2D();
        return new Rectangle(
                (int) Math.round(env.getMinX()),
                (int) Math.round(env.getMinY()),
                (int) Math.ceil(env.getWidth()),
                (int) Math.ceil(env.getHeight()));
    }

    /**
     * Returns a high precision and more accurate bounding box of the <code>Shape</code> than the
     * <code>getBounds</code> method. Note that there is no guarantee that the returned {@link
     * Rectangle2D}is the smallest bounding box that encloses the <code>Shape</code>, only that the
     * <code>Shape</code> lies entirely within the indicated <code>Rectangle2D</code>. The bounding
     * box returned by this method is usually tighter than that returned by the <code>getBounds
     * </code> method and never fails due to overflow problems since the return value can be an
     * instance of the <code>Rectangle2D</code> that uses double precision values to store the
     * dimensions.
     *
     * @return an instance of <code>Rectangle2D</code> that is a high-precision bounding box of the
     *     <code>Shape</code>.
     * @see #getBounds
     */
    @Override
    public Rectangle2D getBounds2D() {
        Envelope env = geometry.getEnvelopeInternal();
        // note, we dont' use getWidth/getHeight since they are slower
        return new Rectangle2D.Double(
                env.getMinX(),
                env.getMinY(),
                env.getMaxX() - env.getMinX(),
                env.getMaxY() - env.getMinY());
    }

    /**
     * Returns an iterator object that iterates along the <code>Shape</code> boundary and provides
     * access to the geometry of the <code>Shape</code> outline. If an optional {@link
     * AffineTransform}is specified, the coordinates returned in the iteration are transformed
     * accordingly.
     *
     * <p>Each call to this method returns a fresh <code>PathIterator</code> object that traverses
     * the geometry of the <code>Shape</code> object independently from any other <code>PathIterator
     * </code> objects in use at the same time.
     *
     * <p>It is recommended, but not guaranteed, that objects implementing the <code>Shape</code>
     * interface isolate iterations that are in process from any changes that might occur to the
     * original object's geometry during such iterations.
     *
     * <p>Before using a particular implementation of the <code>Shape</code> interface in more than
     * one thread simultaneously, refer to its documentation to verify that it guarantees that
     * iterations are isolated from modifications.
     *
     * @param at an optional <code>AffineTransform</code> to be applied to the coordinates as they
     *     are returned in the iteration, or <code>null</code> if untransformed coordinates are
     *     desired
     * @return a new <code>PathIterator</code> object, which independently traverses the geometry of
     *     the <code>Shape</code>.
     */
    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        PathIterator pi = null;

        if (this.geometry == null || this.geometry.isEmpty()) return EmptyIterator.INSTANCE;

        // return iterator according to the kind of geometry we include
        if (this.geometry instanceof Point) {
            pi = new PointIterator((Point) geometry, at);
        }

        if (this.geometry instanceof Polygon) {
            pi = new PolygonIterator((Polygon) geometry, at, generalize, maxDistance);
        } else if (this.geometry instanceof LineString) {
            pi = new LineIterator((LineString) geometry, at, generalize, (float) maxDistance);
        } else if (this.geometry instanceof GeometryCollection) {
            pi =
                    new GeomCollectionIterator(
                            (GeometryCollection) geometry, at, generalize, maxDistance);
        }
        return pi;
    }

    /**
     * Returns an iterator object that iterates along the <code>Shape</code> boundary and provides
     * access to a flattened view of the <code>Shape</code> outline geometry.
     *
     * <p>Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types are returned by the iterator.
     *
     * <p>If an optional <code>AffineTransform</code> is specified, the coordinates returned in the
     * iteration are transformed accordingly.
     *
     * <p>The amount of subdivision of the curved segments is controlled by the <code>flatness
     * </code> parameter, which specifies the maximum distance that any point on the unflattened
     * transformed curve can deviate from the returned flattened path segments. Note that a limit on
     * the accuracy of the flattened path might be silently imposed, causing very small flattening
     * parameters to be treated as larger values. This limit, if there is one, is defined by the
     * particular implementation that is used.
     *
     * <p>Each call to this method returns a fresh <code>PathIterator</code> object that traverses
     * the <code>Shape</code> object geometry independently from any other <code>PathIterator</code>
     * objects in use at the same time.
     *
     * <p>It is recommended, but not guaranteed, that objects implementing the <code>Shape</code>
     * interface isolate iterations that are in process from any changes that might occur to the
     * original object's geometry during such iterations.
     *
     * <p>Before using a particular implementation of this interface in more than one thread
     * simultaneously, refer to its documentation to verify that it guarantees that iterations are
     * isolated from modifications.
     *
     * @param at an optional <code>AffineTransform</code> to be applied to the coordinates as they
     *     are returned in the iteration, or <code>null</code> if untransformed coordinates are
     *     desired
     * @param flatness the maximum distance that the line segments used to approximate the curved
     *     segments are allowed to deviate from any point on the original curve
     * @return a new <code>PathIterator</code> that independently traverses the <code>Shape</code>
     *     geometry.
     */
    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    /**
     * Tests if the interior of the <code>Shape</code> intersects the interior of a specified <code>
     * Rectangle2D</code>. This method might conservatively return <code>true</code> when:
     *
     * <ul>
     *   <li>there is a high probability that the <code>Rectangle2D</code> and the <code>Shape
     *       </code> intersect, but
     *   <li>the calculations to accurately determine this intersection are prohibitively expensive.
     * </ul>
     *
     * This means that this method might return <code>true</code> even though the <code>Rectangle2D
     * </code> does not intersect the <code>Shape</code>.
     *
     * @param r the specified <code>Rectangle2D</code>
     * @return <code>true</code> if the interior of the <code>Shape</code> and the interior of the
     *     specified <code>Rectangle2D</code> intersect, or are both highly likely to intersect and
     *     intersection calculations would be too expensive to perform; <code>false</code>
     *     otherwise.
     * @see #intersects(double, double, double, double)
     */
    @Override
    public boolean intersects(Rectangle2D r) {
        Geometry rect = rectangleToGeometry(r);

        return geometry.intersects(rect);
    }

    /**
     * Tests if the interior of the <code>Shape</code> intersects the interior of a specified
     * rectangular area. The rectangular area is considered to intersect the <code>Shape</code> if
     * any point is contained in both the interior of the <code>Shape</code> and the specified
     * rectangular area.
     *
     * <p>This method might conservatively return <code>true</code> when:
     *
     * <ul>
     *   <li>there is a high probability that the rectangular area and the <code>Shape</code>
     *       intersect, but
     *   <li>the calculations to accurately determine this intersection are prohibitively expensive.
     * </ul>
     *
     * This means that this method might return <code>true</code> even though the rectangular area
     * does not intersect the <code>Shape</code>. The {@link java.awt.geom.Area Area}class can be
     * used to perform more accurate computations of geometric intersection for any <code>Shape
     * </code> object if a more precise answer is required.
     *
     * @param x the coordinates of the specified rectangular area, x value
     * @param y the coordinates of the specified rectangular area, y value
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return <code>true</code> if the interior of the <code>Shape</code> and the interior of the
     *     rectangular area intersect, or are both highly likely to intersect and intersection
     *     calculations would be too expensive to perform; <code>false</code> otherwise.
     * @see java.awt.geom.Area
     */
    @Override
    public boolean intersects(double x, double y, double w, double h) {
        Geometry rect = createRectangle(x, y, w, h);

        return geometry.intersects(rect);
    }

    /**
     * Converts the Rectangle2D passed as parameter in a jts Geometry object
     *
     * @param r the rectangle to be converted
     * @return a geometry with the same vertices as the rectangle
     */
    private Geometry rectangleToGeometry(Rectangle2D r) {
        return createRectangle(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    /**
     * Creates a jts Geometry object representing a rectangle with the given parameters
     *
     * @param x left coordinate
     * @param y bottom coordinate
     * @param w width
     * @param h height
     * @return a rectangle with the specified position and size
     */
    private Geometry createRectangle(double x, double y, double w, double h) {
        Coordinate[] coords = {
            new Coordinate(x, y),
            new Coordinate(x, y + h),
            new Coordinate(x + w, y + h),
            new Coordinate(x + w, y),
            new Coordinate(x, y)
        };
        LinearRing lr = geometry.getFactory().createLinearRing(coords);

        return geometry.getFactory().createPolygon(lr, null);
    }

    public MathTransform getMathTransform() {
        return mathTransform;
    }

    public Geometry getGeometry() {
        return geometry;
    }
}
