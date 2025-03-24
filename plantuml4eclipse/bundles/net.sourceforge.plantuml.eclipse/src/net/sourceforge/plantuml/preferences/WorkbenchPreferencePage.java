package net.sourceforge.plantuml.preferences;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.sourceforge.plantuml.OptionFlags;
import net.sourceforge.plantuml.eclipse.Activator;
import net.sourceforge.plantuml.eclipse.utils.PlantumlConstants;
import net.sourceforge.plantuml.eclipse.utils.WorkbenchUtil;
import net.sourceforge.plantuml.util.DiagramData;

public class WorkbenchPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Label authorLabel;
	private Link plantumlLink;

	private Group graphvizGroup;
	private Label graphvizPathText;
	private FileFieldEditor graphvizPath;
	private Label testDotLabel;

	public WorkbenchPreferencePage() {
		super();
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(final Composite parent) {
		final Composite pnl = new Composite(parent, SWT.NONE);
		pnl.setLayout(new GridLayout(1, false));

		// author image
		final GridData authorsGridData = new GridData();
		authorsGridData.horizontalAlignment = GridData.CENTER;
		authorsGridData.horizontalSpan = 3;
		authorLabel = new Label(pnl, SWT.BORDER);
		authorLabel.setLayoutData(authorsGridData);

		// generate images
		ImageData authorsImg = null;
		ImageData testDotImg = null;
		try {
			authorsImg = new DiagramData(PlantumlConstants.AUTHORS_DIAGRAM).getImage();
			testDotImg = new DiagramData(PlantumlConstants.TEST_DOT_DIAGRAM).getImage();
		} catch (final StackOverflowError e) {
			WorkbenchUtil.errorBox("Error during preferences loading.", e);
		}

		if (authorsImg != null) {
			authorLabel.setImage(new Image(pnl.getDisplay(), authorsImg));
		}

		// PlantUml link
		plantumlLink = new Link(pnl, SWT.NONE);
		plantumlLink.setText("Visit the <a>PlantUML Website</a>.");
		plantumlLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				Program.launch("http://plantuml.com");
			}
		});

		// Graphviz path group
		graphvizGroup = new Group(pnl, SWT.NONE);
		graphvizGroup.setText(PlantumlConstants.GRAPHVIZ_PATH_GROUP_LABEL);
		final GridData groupGridData = new GridData();
		groupGridData.horizontalAlignment = SWT.FILL;
		groupGridData.grabExcessHorizontalSpace = true;
		graphvizGroup.setLayoutData(groupGridData);

		// Graphviz path selection
		graphvizPathText = new Label(graphvizGroup, SWT.NONE);
		final GridData labelGridData = new GridData();
		labelGridData.horizontalSpan = 3;
		graphvizPathText.setLayoutData(labelGridData);
		graphvizPathText.setText(PlantumlConstants.GRAPHVIZ_PATH_TEXT_LABEL);
		graphvizPath = new FileFieldEditor(PlantumlConstants.GRAPHVIZ_PATH,
				PlantumlConstants.GRAPHVIZ_PATH_LABEL, graphvizGroup);
		graphvizPath.setStringValue(getPreferenceStore().getString(
				PlantumlConstants.GRAPHVIZ_PATH));

		// test dot image
		final GridData testDotGridData = new GridData();
		testDotGridData.horizontalAlignment = GridData.CENTER;
		testDotGridData.horizontalSpan = 3;
		testDotLabel = new Label(graphvizGroup, SWT.NONE);
		testDotLabel.setLayoutData(testDotGridData);
		if (testDotImg != null) {
			testDotLabel.setImage(new Image(graphvizGroup.getDisplay(), testDotImg));
		}

		// listen for change of Graphviz path to reload testDotImg
		graphvizPath.setPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				ImageData testDotImg = null;
				OptionFlags.getInstance().setDotExecutable(graphvizPath.getStringValue());
				try {
					testDotImg = new DiagramData(PlantumlConstants.TEST_DOT_DIAGRAM).getImage();
				} catch (final StackOverflowError e) {
					WorkbenchUtil.errorBox("Error during preferences loading.", e);
				}
				if (testDotImg != null) {
					testDotLabel.setImage(new Image(graphvizGroup.getDisplay(),
							testDotImg));
				}
			}
		});
		return pnl;
	}

	/**
	 * Called when user clicks Restore Defaults
	 */
	@Override
	protected void performDefaults() {
		// Reset the fields to the defaults
		graphvizPath.setStringValue("");
	}

	/**
	 * Called when user clicks Apply or OK
	 *
	 * @return boolean
	 */
	@Override
	public boolean performOk() {
		// Get the preference store
		final IPreferenceStore preferenceStore = getPreferenceStore();

		// Set the values from the fields
		if (graphvizPath != null)
			preferenceStore.setValue(PlantumlConstants.GRAPHVIZ_PATH,
					graphvizPath.getStringValue());

		// Return true to allow dialog to close
		return true;
	}

}
