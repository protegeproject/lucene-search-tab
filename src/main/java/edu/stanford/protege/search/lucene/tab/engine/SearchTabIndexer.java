package edu.stanford.protege.search.lucene.tab.engine;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.find.OWLEntityFinder;
import org.protege.editor.search.lucene.AbstractLuceneIndexer;
import org.protege.editor.search.lucene.IndexField;
import org.protege.editor.search.lucene.IndexItemsCollector;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AxiomSubjectProvider;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Josef Hardi <josef.hardi@stanford.edu><br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 04/11/2015
 */
public class SearchTabIndexer extends AbstractLuceneIndexer {

    private final OWLEntityFinder entityFinder;
    private final OWLModelManager objectRenderer;

    public SearchTabIndexer(OWLEditorKit editorKit) {
        super(new ClassicWhitespaceAnalyzer());
        entityFinder = editorKit.getOWLModelManager().getOWLEntityFinder();
        objectRenderer = editorKit.getOWLModelManager();
    }

    @Override
    public IndexItemsCollector getIndexItemsCollector() {

        return new IndexItemsCollector() {

            private Set<Document> documents = new HashSet<>();

            @Override
            public Set<Document> getIndexDocuments() {
                return documents;
            }

            @Override
            public void visit(OWLOntology ontology) {
                for (OWLEntity entity : ontology.getSignature()) {
                    entity.accept(this);
                    for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(entity.getIRI())) {
                        axiom.accept(this);
                    }
                }
                for (OWLAxiom axiom : ontology.getLogicalAxioms()) {
                    axiom.accept(this);
                }
            }

            @Override
            public void visit(OWLClass cls) {
                Document doc = new Document();
                doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(cls), Store.YES));
                doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(cls), Store.YES));
                doc.add(new StringField(IndexField.ENTITY_TYPE, getType(cls), Store.YES));
                documents.add(doc);
            }

            @Override
            public void visit(OWLObjectProperty property) {
                Document doc = new Document();
                doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(property), Store.YES));
                doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(property), Store.YES));
                doc.add(new StringField(IndexField.ENTITY_TYPE, getType(property), Store.YES));
                documents.add(doc);
            }

            public void visit(OWLDataProperty property) {
                Document doc = new Document();
                doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(property), Store.YES));
                doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(property), Store.YES));
                doc.add(new StringField(IndexField.ENTITY_TYPE, getType(property), Store.YES));
                documents.add(doc);
            }

            public void visit(OWLNamedIndividual individual) {
                Document doc = new Document();
                doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(individual), Store.YES));
                doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(individual), Store.YES));
                doc.add(new StringField(IndexField.ENTITY_TYPE, getType(individual), Store.YES));
                documents.add(doc);
            }

            public void visit(OWLAnnotationProperty property) {
                Document doc = new Document();
                doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(property), Store.YES));
                doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(property), Store.YES));
                doc.add(new StringField(IndexField.ENTITY_TYPE, getType(property), Store.YES));
                documents.add(doc);
            }

            @Override
            public void visit(OWLAnnotationAssertionAxiom axiom) {
                if (axiom.getSubject() instanceof IRI) {
                    Document doc = new Document();
                    OWLEntity entity = getOWLEntity((IRI) axiom.getSubject());
                    doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(entity), Store.YES));
                    doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(entity), Store.YES));
                    doc.add(new StringField(IndexField.ANNOTATION_IRI, getEntityId(axiom.getProperty()), Store.YES));
                    doc.add(new TextField(IndexField.ANNOTATION_DISPLAY_NAME, getDisplayName(axiom.getProperty()), Store.YES));
                    OWLAnnotationValue value = axiom.getAnnotation().getValue();
                    
                    doc = addPropValToDoc(doc, value);
                    
                    documents.add(doc);
                    // add annotations on annotations
                    for (OWLAnnotation ann : axiom.getAnnotations()) {
                    	doc = new Document();
                    	doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(entity), Store.YES));
                        doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(entity), Store.YES));
                        
                        doc.add(new StringField(IndexField.ANNOTATION_IRI, getEntityId(ann.getProperty()), Store.YES));
                        doc.add(new TextField(IndexField.ANNOTATION_DISPLAY_NAME, getDisplayName(ann.getProperty()), Store.YES));
                       
                        value = ann.getValue();
                        doc = addPropValToDoc(doc, value);
                        
                        documents.add(doc);
                    	
                    }
                }
            }
            
            private Document addPropValToDoc(Document doc, OWLAnnotationValue value) {
            	if (value instanceof OWLLiteral) {
                    OWLLiteral literal = (OWLLiteral) value;
                    if (literal.getDatatype().getIRI().equals(XSDVocabulary.ANY_URI.getIRI())) {
                        doc.add(new StringField(IndexField.ANNOTATION_VALUE_IRI, literal.getLiteral(), Store.YES));
                    }
                    else {
                    	String foo = strip(literal.getLiteral());
                        doc.add(new TextField(IndexField.ANNOTATION_TEXT, foo, Store.YES));
                    }
                }
                else if (value instanceof IRI) {
                    IRI iri = (IRI) value;
                    doc.add(new StringField(IndexField.ANNOTATION_VALUE_IRI, iri.toString(), Store.YES));
                }
            	return doc;
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

            @Override
            public void visit(OWLEquivalentClassesAxiom axiom) {
                visitLogicalAxiom(axiom);
                Set<OWLSubClassOfAxiom> subClassAxioms = axiom.asOWLSubClassOfAxioms();
                for (OWLSubClassOfAxiom sc : subClassAxioms) {
                    sc.accept(this);
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
                            doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(subclass), Store.YES));
                            doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(subclass), Store.YES));
                            doc.add(new StringField(IndexField.OBJECT_PROPERTY_IRI, getEntityId(property), Store.YES));
                            doc.add(new TextField(IndexField.OBJECT_PROPERTY_DISPLAY_NAME, getDisplayName(property), Store.YES));
                            doc.add(new StringField(IndexField.FILLER_IRI, getEntityId(filler), Store.YES));
                            doc.add(new TextField(IndexField.FILLER_DISPLAY_NAME, getDisplayName(filler), Store.YES));
                        }
                        else {
                            doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(subclass), Store.YES));
                            doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(subclass), Store.YES));
                            doc.add(new StringField(IndexField.OBJECT_PROPERTY_IRI, getEntityId(property), Store.YES));
                            doc.add(new TextField(IndexField.OBJECT_PROPERTY_DISPLAY_NAME, getDisplayName(property), Store.YES));
                            doc.add(new StringField(IndexField.FILLER_IRI, "", Store.NO));
                            doc.add(new TextField(IndexField.FILLER_DISPLAY_NAME, "", Store.NO));
                        }
                        documents.add(doc);
                    }
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
                    doc.add(new TextField(IndexField.ENTITY_IRI, getEntityId(entity), Store.YES));
                    doc.add(new TextField(IndexField.DISPLAY_NAME, getDisplayName(entity), Store.YES));
                    doc.add(new TextField(IndexField.AXIOM_DISPLAY_NAME, getDisplayName(axiom), Store.YES));
                    doc.add(new StringField(IndexField.AXIOM_TYPE, getType(axiom), Store.YES));
                    documents.add(doc);
                }
            }

            /*
             * Utility methods
             */

            private OWLEntity getOWLEntity(IRI identifier) {
                return entityFinder.getEntities(identifier).stream().findFirst().get();
            }

            private String getEntityId(OWLEntity entity) {
                return entity.getIRI().toString();
            }

            private String getType(OWLObject object) {
                if (object instanceof OWLEntity) {
                    return ((OWLEntity) object).getEntityType().getName();
                }
                else if (object instanceof OWLAxiom) {
                    return ((OWLAxiom) object).getAxiomType().getName();
                }
                return "(Unknown type)";
            }

            private String getDisplayName(OWLObject object) {
                return objectRenderer.getRendering(object);
            }

            private String strip(String s) {
                return s.replaceAll("\\^\\^.*$", "") // remove datatype ending
                        .replaceAll("^\"|\"$", "") // remove enclosed quotes
                        .replaceAll("<[^>]+>", " ") // trim XML tags
                        .replaceAll("\\s+", " ") // trim excessive white spaces
                        .replaceAll("\\p{P}", "") // remove punctuation
                        .trim();
            }
        };
    }
}
