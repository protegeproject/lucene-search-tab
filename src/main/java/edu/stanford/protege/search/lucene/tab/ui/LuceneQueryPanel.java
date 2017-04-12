package edu.stanford.protege.search.lucene.tab.ui;

import org.protege.editor.core.Disposable;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Center for Biomedical Informatics Research <br>
 * Stanford University
 */
public class LuceneQueryPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -1624373956245012306L;
    private OWLEditorKit editorKit;
    private QueryResultsPanel resultsPanel;
    private QueryEditorPanel editorPanel;
    private boolean painted;
    private JSplitPane splitPane;
    
    public LuceneQueryPanel(OWLEditorKit editorKit, LuceneTabLayout layout, JButton b) {
        this.editorKit = checkNotNull(editorKit);
        initUi(layout, b);
    }

    /**
     * Constructor
     *
     * @param editorKit OWL Editor Kit
     */
    public LuceneQueryPanel(OWLEditorKit editorKit, LuceneTabLayout layout) {
        this.editorKit = checkNotNull(editorKit);
        initUi(layout, null);
    }

    private void initUi(LuceneTabLayout layout, JButton b) {
        setLayout(new BorderLayout());
        editorPanel = new QueryEditorPanel(editorKit);
        resultsPanel = new QueryResultsPanel(editorKit, b);
        if(layout.equals(LuceneTabLayout.VERTICAL)) {
            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, editorPanel, resultsPanel);
        } else if(layout.equals(LuceneTabLayout.HORIZONTAL)) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, editorPanel, resultsPanel);
        }
        splitPane.setPreferredSize(new Dimension(1300, 600));
        splitPane.setBorder(LuceneUiUtils.EMPTY_BORDER);
        add(splitPane, BorderLayout.CENTER);
        splitPane.setResizeWeight(0.5);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (!painted) {
            painted = true;
            splitPane.setDividerLocation(0.5);
        }
    }

    public QueryResultsPanel getResultsPanel() {
        return resultsPanel;
    }

    public OWLEntity getSelectedEntity() {
        return resultsPanel.getSelectedEntity();
    }

    public static Optional<OWLEntity> showDialog(OWLEditorKit editorKit, LuceneTabLayout layout) {
        LuceneQueryPanel panel = new LuceneQueryPanel(editorKit, layout);
        int response = JOptionPaneEx.showConfirmDialog(
                editorKit.getOWLWorkspace(), "Lucene Query Dialog", panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        Optional<OWLEntity> output;
        if (response == JOptionPane.OK_OPTION) {
            output = Optional.ofNullable(panel.getSelectedEntity());
        } else {
            output = Optional.empty();
        }
        panel.dispose();
        return output;
    }

    @Override
    public void dispose() {
        resultsPanel.dispose();
        editorPanel.dispose();
    }

    public enum LuceneTabLayout {
        HORIZONTAL, VERTICAL
    }
}
