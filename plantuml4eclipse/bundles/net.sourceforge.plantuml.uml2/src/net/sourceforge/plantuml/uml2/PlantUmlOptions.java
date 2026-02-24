/**
 * Copyright (c) 2025, 2026 CEA LIST and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 */

package net.sourceforge.plantuml.uml2;

import java.util.List;

import org.eclipse.uml2.uml.Classifier;

/**
 * Static configuration of PlantUML generation options from UML2.
 * TODO: Link with a preference page, enabling users to change properties. Currently,
 * the preferences can only be changed programmatically.
 */
public class PlantUmlOptions {
	/**
	 * Different options, how to represent a comment
	 */
	public enum CommentStyle {
		NONE, SIMPLE, TOP_NOTE, BOTTOM_NOTE,
	}

	/**
	 * Choose comment style depending on enumeration value
	 */
	public static CommentStyle commentStyle = CommentStyle.TOP_NOTE;

	/**
	 * use qualified names in references
	 */
	public static boolean useQName = true;

	/**
	 * if true, use absolute package name (prefixed with .)
	 */
	public static boolean useAbsolute = false;

	/**
	 * Filter output based on list of declarations
	 */
	public static List<Classifier> filterList = null;
}
