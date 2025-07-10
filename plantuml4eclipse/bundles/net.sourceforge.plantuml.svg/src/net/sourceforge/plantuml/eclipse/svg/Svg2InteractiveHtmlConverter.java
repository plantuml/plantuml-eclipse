package net.sourceforge.plantuml.eclipse.svg;

public class Svg2InteractiveHtmlConverter extends SimpleSvg2HtmlConverter {

	public Svg2InteractiveHtmlConverter() {
		super("<html>\n" +
				"  <head>\n" +
				"    <script src=\"https://bumbu.github.io/svg-pan-zoom/dist/svg-pan-zoom.min.js\"></script>\n" +
				"    <style type=\"text/css\" media=\"screen\">\n" +
				"      svg {\n" +
				"        display:block; border:1px; position:absolute;\n" +
				"        width:100% !important; height:100% !important;\n" +
				"      }\n" +
				"    </style>\n" +
				"  </head>\n" +
				"  <body>\n",
				"\n    <script>\n" +
						"      window.onload = function() {\n" +
						"        window.zoomPlantuml = svgPanZoom('#plantuml', {\n" +
						"          zoomEnabled: true,\n" +
						"          controlIconsEnabled: true,\n" +
						"          fit: true,\n" +
						"          center: true,\n" +
						"        });\n" +
						"      };\n" +
						"    </script>\n" +
						"  </body>\n" +
				"</html>");
	}
}
