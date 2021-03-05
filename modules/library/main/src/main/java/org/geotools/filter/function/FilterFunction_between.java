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
import org.opengis.filter.capability.FunctionName;

public class FilterFunction_between extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "between",
                    parameter(
                            "between",
                            Boolean.class,
                            "Between",
                            "Returns true if between min and max."),
                    parameter("value", Object.class, "Value", "Number or string value"),
                    parameter("min", Object.class, "Min", "Minimum number or string"),
                    parameter("max", Object.class, "Max", "Maximum number or string"));

    public FilterFunction_between() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object feature) {
        Object value;
        Object low;
        Object high;

        try { // attempt to get value and perform conversion
            value = getExpression(0).evaluate(feature);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function between argument #0 - expected type Object");
        }

        try { // attempt to get value and perform conversion
            low = getExpression(1).evaluate(feature);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function between argument #1 - expected type Object");
        }

        try { // attempt to get value and perform conversion
            high = getExpression(2).evaluate(feature);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function between argument #2 - expected type Object");
        }

        return Boolean.valueOf(StaticGeometry.between(value, low, high));
    }
}
