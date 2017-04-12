package edu.stanford.protege.search.lucene.tab.ui;

import org.imgscalr.Scalr;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Center for Biomedical Informatics Research <br>
 * Stanford University
 */
public class LuceneUiUtils {

    public static Color
            MATTE_BORDER_COLOR = new Color(220, 220, 220);

    public static final Border
            MATTE_BORDER = new MatteBorder(1, 1, 1, 1, MATTE_BORDER_COLOR),
            EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);

    public static final String
            CLOSE_ICON_FILENAME = "close.png",
            BACK_ICON_FILENAME = "back.png",
            FORWARD_ICON_FILENAME = "forward.png",
            SEARCH_ICON_FILENAME = "search.png";

    public static Icon getIcon(String filename, int width, int height) {
        BufferedImage icon = null;
        ClassLoader classLoader = LuceneUiUtils.class.getClassLoader();
        try {
            icon = ImageIO.read(checkNotNull(classLoader.getResource(filename)));
        } catch (IOException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        Image img = Scalr.resize(icon, Scalr.Method.ULTRA_QUALITY, width, height);
        return new ImageIcon(img);
    }

    public static List<OWLEntity> getProperties(OWLEditorKit editorKit) {
        List<OWLEntity> entities = new ArrayList<>();
        OWLOntology ont = editorKit.getModelManager().getActiveOntology();
        entities.addAll(ont.getAnnotationPropertiesInSignature());
        entities.addAll(ont.getObjectPropertiesInSignature());
        entities.addAll(ont.getDataPropertiesInSignature());
        return entities;
    }

    public static int getWidestEntityStringRendering(OWLEditorKit editorKit, Collection<? extends OWLEntity> entities, FontMetrics fontMetrics) {
        int widest = 0;
        OWLModelManagerEntityRenderer renderer = editorKit.getModelManager().getOWLEntityRenderer();
        for(OWLEntity e : entities) {
            String str = renderer.render(e);
            int lineWidth = fontMetrics.stringWidth(str);
            widest = Math.max(widest, lineWidth);
        }
        return widest+60;
    }

    public static Optional<OWLProperty> getPropertyForIri(OWLEditorKit editorKit, IRI iri) {
        List<OWLEntity> properties = getProperties(editorKit);
        for(OWLEntity e : properties) {
            if(e.getIRI().equals(iri)) {
                if(e.isOWLAnnotationProperty()) {
                    return Optional.of(e.asOWLAnnotationProperty());
                } else if(e.isOWLDataProperty()) {
                    return Optional.of(e.asOWLDataProperty());
                } else if(e.isOWLObjectProperty()) {
                    return Optional.of(e.asOWLObjectProperty());
                }
            }
        }
        return Optional.empty();
    }
    
    public static String unescape(String s) {
    	if (s.startsWith("'") &&
    			s.endsWith("'")) {
    		return s.substring(1, s.length() - 1);
    	}
    	return s;
    }
}
