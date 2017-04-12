package edu.stanford.protege.csv.export.ui;

import javax.swing.*;

import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.OWLObjectTypeIndexProvider;

import edu.stanford.protege.search.lucene.tab.ui.LuceneUiUtils;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectImplWithoutEntityAndAnonCaching;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Center for Biomedical Informatics Research <br>
 * Stanford University
 */
public class SortedListModel<E> extends AbstractListModel<E> {
	private OWLEditorKit oek;
    private SortedSet<E> model = new TreeSet<E>(new Comparator<E>() {
    	
    	@Override
		public int compare(E o1, E o2) {
			OWLObjectTypeIndexProvider typer = new OWLObjectTypeIndexProvider();
			OWLEntity e1 = (OWLEntity) o1;
			OWLEntity e2 = (OWLEntity) o2;
			int t1 = typer.getTypeIndex(e1);
			int t2 = typer.getTypeIndex(e2);
			int diff = t1 - t2;
			if (diff != 0) {
				return diff;
			}
			String s1 = LuceneUiUtils.unescape(oek.getModelManager().getRendering(e1)).toUpperCase();
			String s2 = LuceneUiUtils.unescape(oek.getModelManager().getRendering(e2)).toUpperCase();
			//System.out.println("string s1 " + s1 + " string s2 " + s2 + " : " + s1.compareTo(s2));
			return s1.compareTo(s2);
			
		}});
		

    /**
     * No-arguments constructor
     */
    public SortedListModel(OWLEditorKit kit) { oek = kit; }
    

    @Override
    public int getSize() {
        return model.size();
    }

    @Override
    public E getElementAt(int index) {
        checkNotNull(index);
        return (E) model.toArray()[index];
    }

    public void add(E element) {
        checkNotNull(element);
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }

    public void addAll(E elements[]) {
        checkNotNull(elements);
        Collection<E> c = Arrays.asList(elements);
        addAll(c);
    }

    public void addAll(Collection<E> elements) {
        checkNotNull(elements);
        model.addAll(elements);
        fireContentsChanged(this, 0, getSize());
    }

    public void clear() {
        model.clear();
        fireContentsChanged(this, 0, getSize());
    }

    public boolean contains(E element) {
        checkNotNull(element);
        return model.contains(element);
    }

    public E firstElement() {
        return model.first();
    }

    public Iterator<E> iterator() {
        return model.iterator();
    }

    public E lastElement() {
        return model.last();
    }

    public boolean removeElement(E element) {
        checkNotNull(element);
        boolean removed = model.remove(element);
        if (removed) {
            fireContentsChanged(this, 0, getSize());
        }
        return removed;
    }
}