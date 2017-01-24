package edu.stanford.protege.search.lucene.tab.engine;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.search.lucene.AddChangeSetHandler;
import org.protege.editor.search.lucene.IndexField;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AxiomSubjectProvider;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import java.util.Set;

public class SearchTabAddChangeSetHandler extends AddChangeSetHandler implements OWLAxiomVisitor {

    public SearchTabAddChangeSetHandler(OWLEditorKit editorKit) {
        super(editorKit);
    }

    @Override
    public void visit(AddAxiom change) {
        OWLAxiom changeAxiom = change.getAxiom();
        changeAxiom.accept(this);
    }

    @Override
    public void visit(OWLDeclarationAxiom axiom) {
        OWLEntity entity = axiom.getEntity();
        Document doc = new Document();
        doc.add(new TextField(IndexField.ENTITY_IRI, getIri(entity), Store.YES));
        doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(entity), Store.YES));
        doc.add(new StringField(IndexField.ENTITY_TYPE, getType(entity), Store.YES));
        documents.add(doc);
    }

    @Override
    public void visit(OWLAnnotationAssertionAxiom axiom) {
        if (axiom.getSubject() instanceof IRI) {
            Document doc = new Document();
            OWLEntity entity = getOWLEntity((IRI) axiom.getSubject());
            doc.add(new TextField(IndexField.ENTITY_IRI, getIri(entity), Store.YES));
            doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(entity), Store.YES));
            doc.add(new StringField(IndexField.ANNOTATION_IRI, getIri(axiom.getProperty()), Store.YES));
            doc.add(new TextField(IndexField.ANNOTATION_DISPLAY_NAME, getDisplayName(axiom.getProperty()), Store.YES));
            OWLAnnotationValue value = axiom.getAnnotation().getValue();
            if (value instanceof OWLLiteral) {
                OWLLiteral literal = (OWLLiteral) value;
                if (literal.getDatatype().getIRI().equals(XSDVocabulary.ANY_URI.getIRI())) {
                    doc.add(new StringField(IndexField.ANNOTATION_VALUE_IRI, literal.getLiteral(), Store.YES));
                }
                else {
                	doc.add(new TextField(IndexField.ANNOTATION_TEXT, strip(literal.getLiteral()), Store.YES));
                }
            }
            else if (value instanceof IRI) {
                IRI iri = (IRI) value;
                doc.add(new StringField(IndexField.ANNOTATION_VALUE_IRI, iri.toString(), Store.YES));
            }
            documents.add(doc);
            
            for (OWLAnnotation ann : axiom.getAnnotations()) {
            	doc = new Document();
            	doc.add(new TextField(IndexField.ENTITY_IRI, getIri(entity), Store.YES));
                doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(entity), Store.YES));
                
                doc.add(new StringField(IndexField.ANNOTATION_IRI, getIri(ann.getProperty()), Store.YES));
                doc.add(new TextField(IndexField.ANNOTATION_DISPLAY_NAME, getDisplayName(ann.getProperty()), Store.YES));
               
                value = ann.getValue();
                if (value instanceof OWLLiteral) {
                    OWLLiteral literal = (OWLLiteral) value;
                    if (literal.getDatatype().getIRI().equals(XSDVocabulary.ANY_URI.getIRI())) {
                        doc.add(new StringField(IndexField.ANNOTATION_VALUE_IRI, literal.getLiteral(), Store.YES));
                    }
                    else {
                    	doc.add(new TextField(IndexField.ANNOTATION_TEXT, strip(literal.getLiteral()), Store.YES));
                    }
                }
                else if (value instanceof IRI) {
                    IRI iri = (IRI) value;
                    doc.add(new StringField(IndexField.ANNOTATION_VALUE_IRI, iri.toString(), Store.YES));
                }
                
                
                documents.add(doc);
            	
            }
        }
    }

    @Override
    public void visit(OWLSubClassOfAxiom axiom) {
        visitLogicalAxiom(axiom);
        if (!(axiom.getSubClass() instanceof OWLClass)) {
            return;
        }
        OWLClass cls = axiom.getSubClass().asOWLClass();
        if (axiom.getSuperClass() instanceof OWLRestriction) {
            OWLRestriction restriction = (OWLRestriction) axiom.getSuperClass();
            visitObjectRestriction(cls, restriction);
        }
        else if (axiom.getSuperClass() instanceof OWLBooleanClassExpression) {
            OWLBooleanClassExpression expr = (OWLBooleanClassExpression) axiom.getSuperClass();
            if (expr instanceof OWLObjectIntersectionOf) {
                for (OWLClassExpression ce : expr.asConjunctSet()) {
                    if (ce instanceof OWLRestriction) {
                        visitObjectRestriction(cls, (OWLRestriction) ce);
                    }
                }
            }
            else if (expr instanceof OWLObjectUnionOf) {
                for (OWLClassExpression ce : expr.asDisjunctSet()) {
                    if (ce instanceof OWLRestriction) {
                        visitObjectRestriction(cls, (OWLRestriction) ce);
                    }
                }
            }
            else if (expr instanceof OWLObjectComplementOf) {
                OWLClassExpression ce = ((OWLObjectComplementOf) expr).getObjectComplementOf();
                if (ce instanceof OWLRestriction) {
                    visitObjectRestriction(cls, (OWLRestriction) ce);
                }
            }
        }
    }

    private void visitObjectRestriction(OWLClass subclass, OWLRestriction restriction) {
        if (restriction.getProperty() instanceof OWLProperty) {
            OWLProperty property = (OWLProperty) restriction.getProperty();
            if (restriction instanceof HasFiller<?>) {
                HasFiller<?> restrictionWithFiller = (HasFiller<?>) restriction;
                Document doc = new Document();
                if (restrictionWithFiller.getFiller() instanceof OWLClass) {
                    OWLClass filler = (OWLClass) restrictionWithFiller.getFiller();
                    doc.add(new TextField(IndexField.ENTITY_IRI, getIri(subclass), Store.YES));
                    doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(subclass), Store.YES));
                    doc.add(new StringField(IndexField.OBJECT_PROPERTY_IRI, getIri(property), Store.YES));
                    doc.add(new TextField(IndexField.OBJECT_PROPERTY_DISPLAY_NAME, getDisplayName(property), Store.YES));
                    doc.add(new StringField(IndexField.FILLER_IRI, getIri(filler), Store.YES));
                    doc.add(new TextField(IndexField.FILLER_DISPLAY_NAME, getDisplayName(filler), Store.YES));
                }
                else {
                    doc.add(new TextField(IndexField.ENTITY_IRI, getIri(subclass), Store.YES));
                    doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(subclass), Store.YES));
                    doc.add(new StringField(IndexField.OBJECT_PROPERTY_IRI, getIri(property), Store.YES));
                    doc.add(new TextField(IndexField.OBJECT_PROPERTY_DISPLAY_NAME, getDisplayName(property), Store.YES));
                    doc.add(new StringField(IndexField.FILLER_IRI, "", Store.NO));
                    doc.add(new TextField(IndexField.FILLER_DISPLAY_NAME, "", Store.NO));
                }
                documents.add(doc);
            }
        }
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        visitLogicalAxiom(axiom);
        Set<OWLSubClassOfAxiom> subClassAxioms = axiom.asOWLSubClassOfAxioms();
        for (OWLSubClassOfAxiom sc : subClassAxioms) {
            sc.accept(this);
        }
    }

    //@formatter:off
    @Override public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLAsymmetricObjectPropertyAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLReflexiveObjectPropertyAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLDisjointClassesAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLDataPropertyDomainAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLObjectPropertyDomainAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLEquivalentObjectPropertiesAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLDifferentIndividualsAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLDisjointDataPropertiesAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLDisjointObjectPropertiesAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLObjectPropertyRangeAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLObjectPropertyAssertionAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLFunctionalObjectPropertyAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLSubObjectPropertyOfAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLDisjointUnionAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLSymmetricObjectPropertyAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLDataPropertyRangeAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLFunctionalDataPropertyAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLEquivalentDataPropertiesAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLClassAssertionAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLDataPropertyAssertionAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLTransitiveObjectPropertyAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLSubDataPropertyOfAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLSameIndividualAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLSubPropertyChainOfAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLInverseObjectPropertiesAxiom axiom) { visitLogicalAxiom(axiom); }
    @Override public void visit(OWLHasKeyAxiom axiom) { visitLogicalAxiom(axiom); }

    //@formatter:on
    private void visitLogicalAxiom(OWLAxiom axiom) {
        Document doc = new Document();
        OWLObject subject = new AxiomSubjectProvider().getSubject(axiom);
        if (subject instanceof OWLEntity) {
            OWLEntity entity = (OWLEntity) subject;
            doc.add(new TextField(IndexField.ENTITY_IRI, getIri(entity), Store.YES));
            doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(entity), Store.YES));
            doc.add(new TextField(IndexField.AXIOM_DISPLAY_NAME, getDisplayName(axiom), Store.YES));
            doc.add(new StringField(IndexField.AXIOM_TYPE, getType(axiom), Store.YES));
            documents.add(doc);
        }
    }

    //@formatter:off
    @Override public void visit(OWLSubAnnotationPropertyOfAxiom axiom) { doesNothing(); }
    @Override public void visit(OWLAnnotationPropertyDomainAxiom axiom) { doesNothing(); }
    @Override public void visit(OWLAnnotationPropertyRangeAxiom axiom) { doesNothing(); }
    @Override public void visit(SWRLRule rule) { doesNothing(); }
    @Override public void visit(OWLDatatypeDefinitionAxiom axiom) { doesNothing(); }

    //@formatter:on
    private void doesNothing() {
        // NO-OP
    }

    private String strip(String s) {
        return s.replaceAll("\\^\\^.*$", "") // remove datatype ending
                .replaceAll("^\"|\"$", "") // remove enclosed quotes
                .replaceAll("<[^>]+>", " ") // trim XML tags
                .replaceAll("\\s+", " ") // trim excessive white spaces
                .trim();
    }
}
