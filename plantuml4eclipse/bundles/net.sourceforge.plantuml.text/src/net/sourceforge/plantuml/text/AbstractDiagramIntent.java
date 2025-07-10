package net.sourceforge.plantuml.text;

import java.util.Map;

import net.sourceforge.plantuml.eclipse.utils.PlantumlConstants;

public abstract class AbstractDiagramIntent<T> extends net.sourceforge.plantuml.util.AbstractDiagramIntent<T> {

	public AbstractDiagramIntent(final T source) {
		super(source);
	}

	public AbstractDiagramIntent(final T source, final String label) {
		super(source, label);
	}

	protected String getStartPlantUml() {
		return PlantumlConstants.START_UML;
	}

	protected String getEndPlantUml() {
		return PlantumlConstants.END_UML;
	}

	public String ensureDiagramText(String diagramText) {
		if (diagramText != null) {
			diagramText = diagramText.trim();
			if (! diagramText.startsWith(getStartPlantUml())) {
				diagramText = getStartPlantUml() + "\n" + diagramText;
			}
			if (! diagramText.endsWith(getEndPlantUml())) {
				diagramText = diagramText + "\n" + getEndPlantUml();
			}
		}
		return diagramText;
	}

	private int indentLevel = 0;
	private final String indentString = "\t";

	protected void indent(final int d) {
		indentLevel += d;
	}

	protected void indent(final StringBuilder buffer) {
		for (int i = 0; i < indentLevel; i++) {
			buffer.append(indentString);
		}
	}

	//

	protected static boolean includes(final int flags, final int... bits) {
		for (int i = 0; i < bits.length; i++) {
			if ((flags & bits[i]) == 0) {
				return false;
			}
		}
		return true;
	}

	protected void appendSkinParams(final Map<String, String> skinParams, final StringBuilder buffer) {
		appendSkinParams(null, skinParams, buffer);
	}

	protected void appendSkinParams(final String qualifier, final Map<String, String> skinParams, final StringBuilder buffer) {
		if (qualifier != null) {
			buffer.append("skinparam");
			buffer.append(" ");
			buffer.append(qualifier);
			buffer.append(" {\n");
		}
		for (String param : skinParams.keySet()) {
			if (qualifier != null) {
				buffer.append("\t");
				if (param.startsWith(qualifier)) {
					param = param.substring(qualifier.length());
				}
			} else {
				buffer.append("skinparam ");
			}
			buffer.append(param);
			buffer.append(" ");
			buffer.append(skinParams.get(param));
			buffer.append("\n");
		}
		if (qualifier != null) {
			buffer.append("}\n");
		}
	}

	protected void appendLink(final String link, final boolean isMember, final StringBuilder result) {
		if (link != null) {
			result.append(" [[");
			if (isMember) {
				result.append("[");
			}
			result.append(link);
			if (isMember) {
				result.append("]");
			}
			result.append("]]");
		}
	}

	protected String getSimpleName(final String name) {
		final int pos = (name != null ? name.lastIndexOf('.') : -1);
		return (pos >= 0 ? name.substring(pos + 1) : name);
	}

	protected String getLogicalName(final String name) {
		StringBuilder builder = null;
		for (int i = 0; i < name.length(); i++) {
			final char c = name.charAt(i);
			if (i == 0 ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c)) {
				if (builder != null) {
					builder.append(c);
				}
			} else {
				if (builder == null) {
					builder = new StringBuilder(name.substring(0, i));
				}
				builder.append("_");
			}
		}
		return builder != null ? builder.toString() : name;
	}

	protected final static String RELATION_LINE = "--";

	protected void appendRelation(final String start, final boolean cont1, final String startLabel, String relation, final String direction, final String end, final boolean cont2, final String endLabel, final String label, final StringBuilder buffer) {
		buffer.append(getLogicalName(start));
		if (startLabel != null) {
			buffer.append(" \"");
			buffer.append(startLabel);
			buffer.append("\"");
		}
		buffer.append(" ");
		if (cont1) {
			buffer.append("*");
		}
		// insert direction directive
		if (direction != null) {
			int pos = relation.indexOf(RELATION_LINE);
			if (pos >= 0) {
				pos = RELATION_LINE.length() / 2;
				relation = relation.replace(RELATION_LINE, RELATION_LINE.substring(0, pos) + direction + RELATION_LINE.substring(pos));
			}
		}
		buffer.append(relation);
		if (cont2) {
			buffer.append("*");
		}
		if (endLabel != null) {
			buffer.append(" \"");
			buffer.append(endLabel);
			buffer.append("\"");
		}
		buffer.append(" ");
		buffer.append(getLogicalName(end));
		if (label != null) {
			buffer.append(" : ");
			buffer.append(label);
		}
		buffer.append("\n");
	}

	protected void appendNameLink(final String name, final String link, final StringBuilder buffer) {
		if (link != null) {
			buffer.append("url of ");
			buffer.append(name);
			buffer.append(" is ");
			appendLink(link, false, buffer);
			buffer.append("\n");
		}
	}
}
