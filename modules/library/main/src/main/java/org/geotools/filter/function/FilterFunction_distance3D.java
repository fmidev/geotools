/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.filter.function;

// this code is autogenerated - you shouldnt be modifying it!

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.distance3d.Distance3DOp;
import org.opengis.filter.capability.FunctionName;

public class FilterFunction_distance3D extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "distance3D",
                    Double.class,
                    parameter("geometry1", Geometry.class),
                    parameter("geometry2", Geometry.class));

    public FilterFunction_distance3D() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object feature) {
        Geometry arg0;
        Geometry arg1;

        try { // attempt to get value and perform conversion
            arg0 = getExpression(0).evaluate(feature, Geometry.class);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function distance3D argument #0 - expected type Geometry");
        }

        try { // attempt to get value and perform conversion
            arg1 = getExpression(1).evaluate(feature, Geometry.class);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function distance3D argument #1 - expected type Geometry");
        }

        return Double.valueOf(Distance3DOp.distance(arg0, arg1));
    }
}
