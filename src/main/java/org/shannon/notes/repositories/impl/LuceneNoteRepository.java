package org.shannon.notes.repositories.impl;

import lombok.NonNull;
import lombok.val;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.shannon.function.ExceptionalSupplier;
import org.shannon.notes.entities.Note;
import org.shannon.notes.repositories.NoteRepository;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LuceneNoteRepository implements NoteRepository, Closeable {

    private final Directory directory;
    private final Analyzer analyzer = new StandardAnalyzer();       // pretty "standard"
    private final AtomicLong maxId;
    private final IndexWriter writer;

    public LuceneNoteRepository(Directory directory) throws IOException {
        this.directory = directory;
        writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
        val searcher = getSearcher();
        val topDocs = searcher.search(new MatchAllDocsQuery(), 1, new Sort(new SortField("id", SortField.Type.LONG, true)));
        maxId = new AtomicLong(
                topDocsToNotes(topDocs, searcher)
                .map(Note::getId)
                .findFirst()
                .orElse(0L)
        );
    }

    @Override
    public void close() throws IOException {
        writer.close();
        directory.close();
    }

    private IndexSearcher getSearcher() throws IOException {
        return new IndexSearcher(DirectoryReader.open(writer));
    }

    private static Stream<Note> topDocsToNotes(TopDocs topDocs, IndexSearcher searcher) {
        return Arrays.stream(topDocs.scoreDocs)
                .flatMap(scoreDoc -> {
                    try {
                        return Stream.of(searcher.doc(scoreDoc.doc));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return Stream.empty();
                    }
                })
                .map(Note::fromDocument);
    }

    private Stream<Note> search(@NonNull Query query, int count) {
        try {
            val searcher = new IndexSearcher(DirectoryReader.open(writer));
            val topDocs = searcher.search(query, count);
            return topDocsToNotes(topDocs, searcher);
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    @Override
    public Optional<Note> findByID(long id) {
        return search(LongPoint.newExactQuery("id", id), 1)
                .findAny();
    }

    @Override
    public Stream<Note> search(@NonNull String queryString, int count) {
        try {
            return search( new QueryParser("body", analyzer).parse(queryString), count);
        } catch (ParseException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    private static <T> T doOrNull(ExceptionalSupplier<Exception, T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Note save(@NonNull Note note) {
            val noteToSave = note.getId() == null ? note.withId(maxId.incrementAndGet()) : note;
            val doc = new Document();
            doc.add(new StoredField("id", noteToSave.getId()));                          // Stored for retrieval
            doc.add(new NumericDocValuesField("id", noteToSave.getId()));                // for sorting
            doc.add(new LongPoint("id", noteToSave.getId()));                            // for exact queries
            doc.add(new TextField("body", noteToSave.getBody(), Field.Store.YES));       // typical full text search
            return doOrNull(() -> {
                writer.addDocument(doc);
                writer.commit();            // allows the near real time readers to see this doc... Could be done at read, but usually pay at index time.
                return noteToSave;
            });
    }

    @Override
    public Iterable<Note> save(Iterable<? extends Note> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Note findOne(Long aLong) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(Long aLong) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Note> findAll() {
        return search(new MatchAllDocsQuery(), Integer.MAX_VALUE)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long aLong) {
        if (aLong != null) {
            try {
                writer.deleteDocuments(LongPoint.newExactQuery("id", aLong));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void delete(@NonNull Note entity) {
        delete(entity.getId());
    }

    @Override
    public void delete(Iterable<? extends Note> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException();
    }
}
