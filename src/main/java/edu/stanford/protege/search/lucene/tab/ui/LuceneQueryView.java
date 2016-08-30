package edu.stanford.protege.search.lucene.tab.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Center for Biomedical Informatics Research <br>
 * Stanford University
 */
public class LuceneQueryView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = 7549563062096902208L;
    private LuceneQueryPanel panel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        setBorder(LuceneUiUtils.EMPTY_BORDER);
        panel = new LuceneQueryPanel(getOWLEditorKit(), LuceneQueryPanel.LuceneTabLayout.HORIZONTAL);
        add(panel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        panel.dispose();
    }
}
