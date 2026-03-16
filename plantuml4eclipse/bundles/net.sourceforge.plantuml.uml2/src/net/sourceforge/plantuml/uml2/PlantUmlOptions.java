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
 * Definition of enumerations for PlantUML generation options from UML2.
 * The settings are linked with a preference page, enabling users to change properties.
 */
public class PlantUmlOptions {
	/**
	 * Different options, how to represent a comment
	 */
	public enum CommentStyle {
		/**
		 * no comments
		 */
		NONE,

		/**
		 * comments via single quote
		 */
		SIMPLE,

		/**
		 * note above element
		 */
		TOP_NOTE,

		/**
		 * note below element
		 */
		BOTTOM_NOTE,
	}

	/**
	 * Different ways to represent names
	 */
	public enum NamingStyle {
		/**
		 * simple name
		 */
		SIMPLE,

		/**
		 * qualified name
		 */
		QUALIFIED,

		/**
		 * absolute qualified name (preceding ".")
		 */
		ABSOLUTE
	}

	/**
	 * Filter output based on list of declarations. This element is not intended for users and can only be changed
	 * programmatically.
	 */
	public static List<Classifier> filterList = null;
}
