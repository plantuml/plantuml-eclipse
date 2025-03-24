package net.sourceforge.plantuml.text.test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

import net.sourceforge.plantuml.eclipse.test.util.AbstractDiagramTextTest;
import net.sourceforge.plantuml.eclipse.utils.PlantumlConstants;
import net.sourceforge.plantuml.eclipse.views.PlantUmlView;

public class TextEditorDiagramTextProviderTest2 extends AbstractDiagramTextTest {

	@Before
	public void setUpJavaProject() throws Exception {
		createProject("texteditortest");
	}
	
	private void testJavaEditorDiagramText(String path, String expected) throws Exception {
		String pluginProject = "net.sourceforge.plantuml.text.tests";
		IFile file = createFile(new Path("/texteditortest/" + path), getPluginTestFileContents(pluginProject, path));
		waitForBuild();
		openEditor(file, "org.eclipse.ui.DefaultTextEditor");
		PlantUmlView view = openView("net.sourceforge.plantuml.eclipse.views.PlantUmlView", PlantUmlView.class);
		
		// wait some time, otherwise the view has not been initialized and filled
		sleep(1500);
		
		AbstractDiagramTextTest.assertDiagramText(expected, view.getDiagramText());
	}

	@Test
	public void testJavaEditorDiagramFromComment() throws Exception {
		// TODO Improve this test, sometimes it fails, maybe because of timing issues
		testJavaEditorDiagramText("src/net/sourceforge/plantuml/text/test/ClassWithDiagramInComment.java",
				PlantumlConstants.START_UML + "\nclass ClassWithDiagramInComment {\n\tint field\n}\n" + PlantumlConstants.END_UML);
	}
}
