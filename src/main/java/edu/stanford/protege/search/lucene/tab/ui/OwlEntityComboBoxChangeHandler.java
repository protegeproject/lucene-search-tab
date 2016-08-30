package edu.stanford.protege.search.lucene.tab.ui;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLOntologyChangeVisitorAdapter;

public class OwlEntityComboBoxChangeHandler extends OWLOntologyChangeVisitorAdapter {

    private OwlEntityComboBox comboBox;

    public OwlEntityComboBoxChangeHandler(OwlEntityComboBox comboBox) {
        this.comboBox = comboBox;
    }

    @Override
    public void visit(RemoveAxiom change) {
        OWLAxiom axiom = change.getAxiom();
        if (axiom instanceof OWLDeclarationAxiom) {
            OWLEntity entity = ((OWLDeclarationAxiom) axiom).getEntity();
            if (entity instanceof OWLProperty) {
                comboBox.removeItem(entity);
            }
        }
    }

    @Override
    public void visit(AddAxiom change) {
        OWLAxiom axiom = change.getAxiom();
        if (axiom instanceof OWLDeclarationAxiom) {
            OWLEntity entity = ((OWLDeclarationAxiom) axiom).getEntity();
            if (entity instanceof OWLProperty) {
                comboBox.addItem(entity);
            }
        }
    }
}
