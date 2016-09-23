package edu.stanford.protege.search.lucene.tab.engine;

import com.google.common.base.Stopwatch;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.model.search.*;
import org.protege.editor.search.lucene.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: Josef Hardi <josef.hardi@stanford.edu><br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 13/11/2015
 */
public class SearchTabManager extends LuceneSearcher {

    private static final Logger logger = LoggerFactory.getLogger(SearchTabManager.class);

    private OWLEditorKit editorKit;

    private Set<SearchCategory> categories = new HashSet<>();

    private ExecutorService service = Executors.newSingleThreadExecutor();

    private AtomicLong lastSearchId = new AtomicLong(0);

    private AtomicBoolean stopSearch = new AtomicBoolean(false);

    private SearchStringParser searchStringParser = new LuceneStringParser();

    private SearchTabIndexer indexer;

    private IndexDelegator indexDelegator;

    private Directory indexDirectory;

    private OWLOntologyChangeListener ontologyChangeListener;

    private OWLModelManagerListener modelManagerListener;

    private OWLOntology currentActiveOntology;

    private final List<ProgressMonitor> progressMonitors = new ArrayList<>();

    public SearchTabManager() {
        // NO-OP
    }

    @Override
    public void initialise() {
        this.editorKit = getEditorKit();
        this.indexer = new SearchTabIndexer(editorKit);
        categories.add(SearchCategory.DISPLAY_NAME);
        categories.add(SearchCategory.IRI);
        categories.add(SearchCategory.ANNOTATION_VALUE);
        categories.add(SearchCategory.LOGICAL_AXIOM);
        ontologyChangeListener = new OWLOntologyChangeListener() {
            public void ontologiesChanged(List<? extends OWLOntologyChange> changes) throws OWLException {
                updateIndex(changes);
            }
        };
        modelManagerListener = new OWLModelManagerListener() {
            public void handleChange(OWLModelManagerChangeEvent event) {
                OWLOntology activeOntology = editorKit.getOWLModelManager().getActiveOntology();
                if (isCacheChangingEvent(event)) {
                    if (currentActiveOntology != null) {
                        /*
                         * A workaround: Protege signals ACTIVE_ONTOLOGY_CHANGED twice when opening an ontology.
                         * The loadOrCreateIndexDirectory() method shouldn't be called twice if the new active
                         * ontology is the same as the current active ontology.
                         */
                        if (!currentActiveOntology.equals(activeOntology)) {
                            loadIndex(activeOntology);
                        }
                        else {
                            // ignore if equals
                        }
                    }
                    else {
                        loadIndex(activeOntology);
                    }
                }
                else if (isCacheSavingEvent(event)) {
                    saveIndex(activeOntology);
                }
            }
        };
        editorKit.getOWLModelManager().addOntologyChangeListener(ontologyChangeListener);
        editorKit.getModelManager().addListener(modelManagerListener);
        initialiseIndex();
    }

    private void initialiseIndex() {
        OWLOntology activeOntology = editorKit.getOWLModelManager().getActiveOntology();
        loadIndex(activeOntology);
    }

    private boolean isCacheChangingEvent(OWLModelManagerChangeEvent event) {
        return event.isType(EventType.ACTIVE_ONTOLOGY_CHANGED);
    }

    private boolean isCacheSavingEvent(OWLModelManagerChangeEvent event) {
        return event.isType(EventType.ONTOLOGY_SAVED);
    }

    private void loadIndex(OWLOntology activeOntology) {
        if (activeOntology != null && !activeOntology.isEmpty()) {
            logger.info("Initializing index");
            currentActiveOntology = activeOntology;
            loadIndexDirectory(activeOntology, false); // false = reload index directory, if any
            markIndexAsStale();
        }
    }

    public void rebuildIndex(OWLOntology targetOntology) {
        if (targetOntology != null && !targetOntology.isEmpty()) {
            logger.info("Rebuilding index");
            loadIndexDirectory(targetOntology, true); // true = recreate the index directory
            service.submit(this::buildingIndex);
        }
    }

    private void updateIndex(List<? extends OWLOntologyChange> changes) {
        if (indexDelegator != null) {
            logger.info("Updating index from " + changes.size() + " change(s)");
            service.submit(() -> updatingIndex(changes));
        }
    }

    private void updatingIndex(List<? extends OWLOntologyChange> changes) {
        try {
            RemoveChangeSet removeChangeSet = RemoveChangeSet.create(changes, new SearchTabRemoveChangeSetHandler(editorKit));
            indexer.doRemove(indexDelegator, removeChangeSet);
            AddChangeSet addChangeSet = AddChangeSet.create(changes, new SearchTabAddChangeSetHandler(editorKit));
            indexer.doAppend(indexDelegator, addChangeSet);
        }
        catch (IOException e) {
            logger.error("... update index failed");
        }
    }

    private void markIndexAsStale() {
        lastSearchId.set(0);
    }

    private void saveIndex(OWLOntology targetOntology) {
        LuceneSearchPreferences.setIndexSnapshot(targetOntology);
    }

    @Override
    public void addProgressMonitor(ProgressMonitor pm) {
        progressMonitors.add(pm);
    }

    @Override
    public void dispose() {
        editorKit.getOWLModelManager().removeOntologyChangeListener(ontologyChangeListener);
        editorKit.getModelManager().removeListener(modelManagerListener);
        disposeIndexDelegator();
    }

    private void disposeIndexDelegator() {
        try {
            if (indexDelegator != null && indexLocationExists()) {
                indexDelegator.dispose();
            }
        }
        catch (IOException e) {
            logger.error("Failed to dispose index delegator", e);
        }
    }

    private boolean indexLocationExists() {
        return LuceneSearchPreferences.getIndexLocation(currentActiveOntology).isPresent();
    }

    @Override
    protected AbstractLuceneIndexer getIndexer() {
        return indexer;
    }

    @Override
    protected IndexSearcher getIndexSearcher() throws IOException {
        if (indexDelegator == null) {
            throw new RuntimeException("No index was loaded");
        }
        return indexDelegator.getSearcher();
    }

    @Override
    public boolean isSearchType(SearchCategory category) {
        return categories.contains(category);
    }

    @Override
    public void setCategories(Collection<SearchCategory> categories) {
        this.categories.clear();
        this.categories.addAll(categories);
    }

    @Override
    public void performSearch(String searchString, SearchResultHandler searchResultHandler) {
        try {
            if (lastSearchId.getAndIncrement() == 0) {
                if (!DirectoryReader.indexExists(getIndexDirectory())) {
                    logger.info("Building index");
                    service.submit(this::buildingIndex);
                }
            }
            List<SearchQuery> searchQueries = prepareQuery(searchString);
            service.submit(new SearchCallable(lastSearchId.incrementAndGet(), searchQueries, searchResultHandler));
        }
        catch (IOException e) {
            logger.error("Failed to perform search", e);
        }
    }

    public void performSearch(SearchTabQuery userQuery, SearchTabResultHandler searchTabResultHandler) {
        try {
            if (lastSearchId.getAndIncrement() == 0) {
                if (!DirectoryReader.indexExists(getIndexDirectory())) {
                    logger.info("Building index");
                    service.submit(this::buildingIndex);
                }
            }
            stopSearch.set(false);
            service.submit(new SearchTabCallable(lastSearchId.incrementAndGet(), userQuery, searchTabResultHandler));
        }
        catch (IOException e) {
            logger.error("Failed to perform search", e);
        }
    }

    public void stopSearch() {
        stopSearch.set(true);
    }

    private void loadIndexDirectory(@Nonnull OWLOntology targetOntology, boolean forceReset) {
        try {
            if (forceReset) {
                removeIndexDirectory();
                LuceneSearchPreferences.removeIndexLocation(targetOntology);
            }
            if (LuceneSearchPreferences.useInMemoryIndexStoring()
                    && isOntologySizeBelowMaximumStoringLimit(targetOntology)) {
                loadIndexFromMemory();
            }
            else {
                loadIndexFromLocalDisk(targetOntology);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to setup index directory", e);
        }
    }

    private boolean isOntologySizeBelowMaximumStoringLimit(OWLOntology targetOntology) {
        Optional<File> ontologyFile = getOntologyFile(targetOntology);
        if (!ontologyFile.isPresent()) {
            return false; // the ontology has no physical file
        }
        boolean shouldStoreInDisk = false;
        int fileSizeInMegaBytes = (int) (ontologyFile.get().length() / (1024 * 1024));
        int maxFileSize = LuceneSearchPreferences.getMaxSizeForInMemoryIndexStoring(); // in MB
        if (fileSizeInMegaBytes > maxFileSize) {
            shouldStoreInDisk = true;
        } else {
            shouldStoreInDisk = false;
        }
        return shouldStoreInDisk;
    }

    private void loadIndexFromMemory() throws IOException {
        logger.info("Storing index into RAM memory");
        Directory directory = new RAMDirectory();
        setIndexDirectory(directory);
    }

    private void loadIndexFromLocalDisk(OWLOntology targetOntology) throws IOException {
        String indexLocation = LuceneSearchPreferences.findIndexLocation(targetOntology);
        Directory directory = FSDirectory.open(Paths.get(indexLocation));
        setIndexDirectory(directory);
    }

    private Optional<File> getOntologyFile(OWLOntology targetOntology) {
        URI physicalLocationUri = editorKit.getOWLModelManager().getOntologyPhysicalURI(targetOntology);
        File ontologyFile = null;
        if (!physicalLocationUri.equals(URI.create(""))) {
            ontologyFile = new File(physicalLocationUri);
        }
        return Optional.ofNullable(ontologyFile);
    }

    private Directory getIndexDirectory() {
        return indexDirectory;
    }

    private void removeIndexDirectory() throws IOException {
        setIndexDelegator(null);
    }

    private void setIndexDirectory(Directory indexDirectory) throws IOException {
        this.indexDirectory = indexDirectory;
        fireIndexDirectoryChange();
    }

    private void fireIndexDirectoryChange() throws IOException {
        setupIndexDelegator();
    }

    private void setupIndexDelegator() throws IOException {
        IndexDelegator newDelegator = null;
        if (getIndexDirectory() != null) {
            newDelegator = IndexDelegator.getInstance(getIndexDirectory(), indexer.getIndexWriterConfig());
        }
        setIndexDelegator(newDelegator);
    }

    private void setIndexDelegator(IndexDelegator indexDelegator) throws IOException {
        if (this.indexDelegator != null) {
            this.indexDelegator.dispose();
        }
        this.indexDelegator = indexDelegator;
    }

    private List<SearchQuery> prepareQuery(String searchString) {
        SearchInput searchInput = searchStringParser.parse(searchString);
        LuceneSearchQueryBuilder builder = new LuceneSearchQueryBuilder(this);
        builder.setCategories(categories);
        searchInput.accept(builder);
        return builder.build();
    }

    private void buildingIndex() {
        fireIndexingStarted();
        try {
            indexer.doIndex(indexDelegator, new SearchContext(editorKit), progress -> fireIndexingProgressed(progress));
            saveIndex(currentActiveOntology);
        }
        catch (IOException e) {
            logger.error("... build index failed", e);
        }
        finally {
            fireIndexingFinished();
        }
    }

    private class SearchCallable implements Runnable {
        private long searchId;
        private List<SearchQuery> searchQueries;
        private SearchResultHandler searchResultHandler;

        private SearchCallable(long searchId, List<SearchQuery> searchQueries, SearchResultHandler searchResultHandler) {
            this.searchId = searchId;
            this.searchQueries = searchQueries;
            this.searchResultHandler = searchResultHandler;
        }

        @Override
        public void run() {
            logger.debug("Starting search {}", searchId);
            Stopwatch stopwatch = Stopwatch.createStarted();
            fireSearchStarted();
            Set<SearchResult> finalResults = new HashSet<>();
            for (SearchQuery query : searchQueries) {
                if (!isLatestSearch()) {
                    // New search started
                    logger.debug("... terminating search {} prematurely", searchId);
                    return;
                }
                try {
                    ResultDocumentHandler handler = new ResultDocumentHandler(editorKit);
                    logger.debug("... executing query " + query);
                    query.evaluate(handler, progress -> fireSearchingProgressed(progress));
                    SearchUtils.intersect(finalResults, handler.getSearchResults());
                }
                catch (QueryEvaluationException e) {
                    logger.error("Error while executing the query: {}", e);
                }
            }
            fireSearchFinished();
            stopwatch.stop();
            logger.debug("... finished search {} in {} ms ({} results)", searchId, stopwatch.elapsed(TimeUnit.MILLISECONDS), finalResults.size());
            showResults(finalResults, searchResultHandler);
        }

        private void showResults(final Set<SearchResult> results, final SearchResultHandler searchResultHandler) {
            if (SwingUtilities.isEventDispatchThread()) {
                searchResultHandler.searchFinished(results);
            }
            else {
                SwingUtilities.invokeLater(() -> searchResultHandler.searchFinished(results));
            }
        }

        private boolean isLatestSearch() {
            return searchId == lastSearchId.get();
        }
    }

    private class SearchTabCallable implements Runnable {
        private long searchId;
        private SearchTabQuery pluginQuery;
        private SearchTabResultHandler searchTabResultHandler;

        private SearchTabCallable(long searchId, SearchTabQuery pluginQuery, SearchTabResultHandler searchTabResultHandler) {
            this.searchId = searchId;
            this.pluginQuery = pluginQuery;
            this.searchTabResultHandler = searchTabResultHandler;
        }

        @Override
        public void run() {
            logger.debug("Starting search {}", searchId);
            Stopwatch stopwatch = Stopwatch.createStarted();
            try {
                logger.debug("... executing query " + pluginQuery);
                fireSearchStarted();
                Set<OWLEntity> finalResults = pluginQuery.evaluate(progress -> fireSearchingProgressed(progress), stopSearch);
                fireSearchFinished();
                stopwatch.stop();
                logger.debug("... finished search {} in {} ms ({} results)", searchId, stopwatch.elapsed(TimeUnit.MILLISECONDS), finalResults.size());
                showResults(finalResults);
            }
            catch (QueryEvaluationException e) {
                logger.error("Error while executing the query: {}", e);
            }
        }

        private void showResults(final Set<OWLEntity> results) {
            if (SwingUtilities.isEventDispatchThread()) {
                searchTabResultHandler.searchFinished(results);
            }
            else {
                SwingUtilities.invokeLater(() -> searchTabResultHandler.searchFinished(results));
            }
        }
    }
 
    /*
     * Private methods to handle progress monitor
     */

    private void fireIndexingStarted() {
        SwingUtilities.invokeLater(() -> {
            for (ProgressMonitor pm : progressMonitors) {
                pm.setSize(100);
                pm.setIndeterminate(true);
                pm.setStarted();
                pm.setMessage("initializing index");
            }
        });
    }

    private void fireIndexingProgressed(final long progress) {
        SwingUtilities.invokeLater(() -> {
            for (ProgressMonitor pm : progressMonitors) {
                pm.setIndeterminate(false);
                pm.setProgress(progress);
                switch ((int)progress % 4) {
                    case 0: pm.setMessage("indexing"); break;
                    case 1: pm.setMessage("indexing."); break;
                    case 2: pm.setMessage("indexing.."); break;
                    case 3: pm.setMessage("indexing..."); break;
                }
            }
        });
    }

    private void fireIndexingFinished() {
        SwingUtilities.invokeLater(() -> {
            for (ProgressMonitor pm : progressMonitors) {
                pm.setFinished();
            }
        });
    }

    private void fireSearchStarted() {
        SwingUtilities.invokeLater(() -> {
            for (ProgressMonitor pm : progressMonitors) {
                pm.setSize(100);
                pm.setStarted();
                pm.setMessage("processing query");
            }
        });
    }

    private void fireSearchingProgressed(final long progress) {
        SwingUtilities.invokeLater(() -> {
            for (ProgressMonitor pm : progressMonitors) {
                pm.setProgress(progress);
                switch ((int)progress % 4) {
                    case 0: pm.setMessage("searching"); break;
                    case 1: pm.setMessage("searching."); break;
                    case 2: pm.setMessage("searching.."); break;
                    case 3: pm.setMessage("searching..."); break;
                }
            }
        });
    }

    private void fireSearchFinished() {
        SwingUtilities.invokeLater(() -> {
            for (ProgressMonitor pm : progressMonitors) {
                pm.setFinished();
            }
        });
    }
}
