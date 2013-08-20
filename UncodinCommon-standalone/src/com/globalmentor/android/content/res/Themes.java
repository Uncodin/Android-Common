/*
 * Copyright © 2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globalmentor.android.content.res;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Various utilities for working with themes.
 * 
 * @author Garret Wilson
 * 
 * @see <a
 *      href="http://stackoverflow.com/questions/5982132/android-how-to-get-value-of-listpreferreditemheight-attribute-in-code">Android:
 *      how to get value of listPreferredItemHeight attribute in code?</a>
 */
public class Themes {

    /**
     * Resolves an attribute of the current context theme and returns the attribute value as a dimension of the display.
     * <p>
     * For example, this method can resolve the resource ID <code>android.R.attr.listPreferredItemHeight</code> and
     * return the value as a dimension to be used in programmatically constructing a layout.
     * </p>
     * 
     * @param context
     *            The current context.
     * @param resid
     *            The resource identifier of the desired theme attribute.
     * @return The context theme attribute as a display dimension.
     * @throws android.content.res.Resources.NotFoundException
     *             if the given resource is not found or is not of the appropriate type.
     * @see android.content.Context#getTheme()
     * @see android.content.res.Resources#getDisplayMetrics()
     * @see android.content.res.Resources.Theme#resolveAttribute(int, android.util.TypedValue, boolean)
     */
    public static float getAttributeDimension(final Context context, final int resId) {
        return getAttributeDimension(context, context.getTheme(), resId);
    }

    /**
     * Resolves an attribute of the theme and returns the attribute value as a dimension of the display.
     * <p>
     * For example, this method can resolve the resource ID <code>android.R.attr.listPreferredItemHeight</code> and
     * return the value as a dimension to be used in programmatically constructing a layout.
     * </p>
     * 
     * @param context
     *            The current context.
     * @param theme
     *            The theme for which an attribute should be resolved.
     * @param resid
     *            The resource identifier of the desired theme attribute.
     * @return The theme attribute as a display dimension.
     * @throws android.content.res.Resources.NotFoundException
     *             if the given resource is not found or is not of the appropriate type.
     * @see android.content.res.Resources#getDisplayMetrics()
     * @see android.content.res.Resources.Theme#resolveAttribute(int, android.util.TypedValue, boolean)
     */
    public static float getAttributeDimension(final Context context, final Theme theme, final int resId) {
        final TypedValue typedValue = new TypedValue(); // create a new typed value to received the resolved attribute
                                                        // value
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        if (!theme.resolveAttribute(resId, typedValue, true)) // if we can't resolve the value
        {
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(resId));
        }
        if (typedValue.type != TypedValue.TYPE_DIMENSION) // if the value isn't of the correct type
        {
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(resId) + " type #0x"
                    + Integer.toHexString(typedValue.type) + " is not valid");
        }
        return typedValue.getDimension(displayMetrics); // return the value of the attribute in terms of the display
    }

    /**
     * Returns the list preferred item height theme attribute as a dimension of the display.
     * 
     * @param context
     *            The current context.
     * @return The list preferred item height for the current context theme.
     * @throws android.content.res.Resources.NotFoundException
     *             if the given resource is not found or is not of the appropriate type.
     * @see <a
     *      href="http://stackoverflow.com/questions/5982132/android-how-to-get-value-of-listpreferreditemheight-attribute-in-code">Android:
     *      how to get value of listPreferredItemHeight attribute in code?</a>
     */
    public static float getListPreferredItemHeightDimension(final Context context) {
        return getAttributeDimension(context, android.R.attr.listPreferredItemHeight);
    }
}
